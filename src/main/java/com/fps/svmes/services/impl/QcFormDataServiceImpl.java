package com.fps.svmes.services.impl;

import com.fps.svmes.dto.PagedResultDTO;
import com.fps.svmes.dto.dtos.alert.ExceededFieldInfoDTO;
import com.fps.svmes.models.nosql.approval.ApprovalActionLog;
import com.fps.svmes.dto.dtos.approval.ApprovalInstanceDTO;
import com.fps.svmes.dto.dtos.approval.ApprovalInstanceListItemDTO;
import com.fps.svmes.dto.dtos.approval.ApprovalInstanceListStepDTO;
// import com.fps.svmes.dto.dtos.qcForm.QcApprovalAssignmentDTO;
import com.fps.svmes.dto.dtos.qcForm.QcFormTemplateDTO;
import com.fps.svmes.enums.approval.ApprovalStepState;
import com.fps.svmes.dto.requests.ApprovalInstanceQueryRequest;
import com.fps.svmes.dto.requests.ApprovalFlowEditRequest;
import com.fps.svmes.enums.form.FormSubmissionState;
import com.fps.svmes.models.nosql.approval.ApprovalInstance;
import com.fps.svmes.models.nosql.approval.FormSubmissionSnapshotForFilter;
import com.fps.svmes.models.nosql.approval.ApprovalInstanceStep;
import com.fps.svmes.dto.dtos.websocket.QcRecordEvent;
import com.fps.svmes.services.ApprovalInfoGeneratorService;
import com.fps.svmes.services.ApprovalInstanceService;
import com.fps.svmes.services.ControlLimitEvaluationService;
import com.fps.svmes.services.FormNotificationConfigService;
import com.fps.svmes.services.QcFormDataService;
import com.fps.svmes.services.QcFormTemplateService;
import com.fps.svmes.services.QcSnapshotSubmissionService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import com.fps.svmes.exceptions.ApprovalInstanceException;
import com.fps.svmes.exceptions.InvalidRequestException;
import com.fps.svmes.utils.MongoFormTemplateUtils;
import com.mongodb.client.result.UpdateResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Owns form-submission lifecycle, versioning, and delegate approval instances updates.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QcFormDataServiceImpl implements QcFormDataService {
    private final MongoTemplate mongoTemplate;
    private final QcFormTemplateService qcFormTemplateService;
    private final ControlLimitEvaluationService controlLimitEvaluationService;
    private final ApprovalInfoGeneratorService approvalInfoGeneratorService;
    private final QcSnapshotSubmissionService qcSnapshotSubmissionService;
    private final MongoFormTemplateUtils mongoUtils;
    private final ApprovalInstanceService approvalInstanceService;
    private final FormNotificationConfigService formNotificationConfigService;
    private final FormSubmissionIndexManager formSubmissionIndexManager;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public Map<String, Object> insertFormData(String collectionName, Long userId, Map<String, Object> formData, boolean submitForApproval) {
        Long formTemplateId = parseTemplateId(collectionName);
        ensureCollectionExists(collectionName, formTemplateId);
        Date now = new Date();

        // Single template fetch — approvalType, approvalTemplateId, and templateName all sourced from here.
        QcFormTemplateDTO template = qcFormTemplateService.getTemplateById(formTemplateId);
        String approvalTemplateId = template.getApprovalTemplateId();
        String templateName = template.getName();
        boolean hasApprovalTemplate = approvalTemplateId != null && !approvalTemplateId.isBlank();
        // Pre-validate template existence before persisting anything; spares us a void-rollback for templated forms with bad config.
        if (hasApprovalTemplate) {
            approvalInstanceService.validateApprovalTemplateExists(approvalTemplateId);
        }

        FormSubmissionState initialState = hasApprovalTemplate
                ? FormSubmissionState.SUBMITTED
                : FormSubmissionState.ARCHIVED;

        Map<String, Object> document = new HashMap<>(formData);
        document.put("version", 1);
        document.put("created_at", now);
        document.put("updated_at", now);
        document.put("created_by", userId);
        document.put("state", initialState.dbValue());

        Map<String, ExceededFieldInfoDTO> exceededInfoMap = controlLimitEvaluationService.evaluateExceededInfo(formTemplateId, formData);
        document.put("exceeded_info", exceededInfoMap);

        // Compat: approval_info and qc_approval_assignment are written for all submissions
        // to support old frontend clients that have not yet been redeployed against the V2 approval UI.
        // Remove once the frontend deployment window is closed.
        String approvalType = template.getApprovalType();
        List<Map<String, Object>> approvalInfo = approvalInfoGeneratorService.generateApprovalInfo(approvalType, userId);
        document.put("approval_info", normalizeStepsForDraft(approvalInfo));

        Document insertedDocument = mongoTemplate.insert(new Document(document), collectionName);
        String submissionId = insertedDocument.getObjectId("_id").toString();

        // Standalone Mongo has no transaction here; remove the form if its approval instance cannot be created.
        ApprovalInstance instance;
        try {
            instance = approvalInstanceService.create(submissionId, collectionName, formTemplateId, approvalTemplateId, userId);
        } catch (RuntimeException e) {
            deleteSubmissionAfterApprovalInstanceFailure(submissionId, collectionName, e);
            throw e;
        }

        FormSubmissionState finalState = initialState;
        if (submitForApproval && instance.getApprovalSteps() != null && !instance.getApprovalSteps().isEmpty()) {
            com.fps.svmes.dto.requests.FormSubmissionActionRequest autoSubmitRequest =
                    new com.fps.svmes.dto.requests.FormSubmissionActionRequest();
            autoSubmitRequest.setSubmissionId(submissionId);
            autoSubmitRequest.setCollectionName(collectionName);
            autoSubmitRequest.setActorUserId(userId);
            approvalInstanceService.enterReview(autoSubmitRequest);
            finalState = FormSubmissionState.UNDER_REVIEW;
        }

        List<String> warnings = new ArrayList<>();
        runPostInsertCompatibilityWork(submissionId, collectionName, formTemplateId, templateName, approvalType, userId, formData, warnings);

        // Send notification emails to responsible persons (failure must not break submission)
        try {
            formNotificationConfigService.triggerSubmissionNotification(
                    formTemplateId, userId, formData, exceededInfoMap,
                    insertedDocument.getObjectId("_id").toString());
        } catch (Exception notifEx) {
            log.warn("Notification trigger failed for formTemplateId={}: {}", formTemplateId, notifEx.getMessage());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("object_id", submissionId);
        response.put("state", finalState.dbValue());
        response.put("version", insertedDocument.getInteger("version", 1));
        if (!warnings.isEmpty()) {
            response.put("warnings", warnings);
        }
        response.put("message", "Form data inserted successfully to " + collectionName);

        try {
            messagingTemplate.convertAndSend("/topic/qc-records/" + formTemplateId, new QcRecordEvent(formTemplateId, collectionName));
        } catch (Exception wsEx) {
            log.warn("WebSocket broadcast failed for templateId={}: {}", formTemplateId, wsEx.getMessage());
        }

        return response;
    }

    @Override
    public Map<String, Object> editFormData(String collectionName, Long userId, String parentSubmissionId, Long formTemplateId,
                                            Map<String, Object> updatedData) {
        Document parent = findSubmission(parentSubmissionId, collectionName);
        Date now = new Date();

        FormSubmissionState parentState = resolveLifecycleState(parent);

        Map<String, Object> newDoc = new HashMap<>(updatedData);
        enrichNewDocWithParentData(newDoc, parent);

        String versionGroupId = parent.getString("version_group_id");
        Integer parentVersion = parent.getInteger("version");
        boolean generatedVersionGroupId = false;
        if (versionGroupId == null || parentVersion == null) {
            versionGroupId = UUID.randomUUID().toString();
            parentVersion = 1;
            parent.put("version_group_id", versionGroupId);
            parent.put("version", parentVersion);
            generatedVersionGroupId = true;
        }
        newDoc.put("version_group_id", versionGroupId);
        newDoc.put("version", parentVersion + 1);
        newDoc.put("created_at", now);
        newDoc.put("updated_at", now);
        newDoc.put("created_by", userId);
        newDoc.put("state", parentState.dbValue());

        Map<String, ExceededFieldInfoDTO> exceededInfoMap =
                controlLimitEvaluationService.evaluateExceededInfo(formTemplateId, updatedData);
        newDoc.put("exceeded_info", exceededInfoMap);

        Document inserted = mongoTemplate.insert(new Document(newDoc), collectionName);
        String newSubmissionId = inserted.getObjectId("_id").toString();

        // Update the approval pointer before voiding the prior version to keep retries safer.
        if (hasPairedApprovalInstance(parentSubmissionId, collectionName)) {
            try {
                approvalInstanceService.onFormSubmissionEdited(parentSubmissionId, newSubmissionId, collectionName, userId);
            } catch (RuntimeException e) {
                deleteSubmissionAfterApprovalInstanceFailure(newSubmissionId, collectionName, e);
                throw e;
            }
        } else {
            log.debug("Skipping approval instance update because submission {} has no paired approval instance", parentSubmissionId);
        }

        Update parentUpdate = new Update()
                .set("state", parentState == FormSubmissionState.ARCHIVED
                        ? FormSubmissionState.ARCHIVED.dbValue()
                        : FormSubmissionState.SUBMITTED.dbValue())
                .set("updated_at", new Date());
        if (generatedVersionGroupId) {
            parentUpdate.set("version_group_id", versionGroupId)
                    .set("version", parentVersion);
        }
        mongoTemplate.updateFirst(submissionIdQuery(parentSubmissionId), parentUpdate, collectionName);

        List<String> warnings = new ArrayList<>();
        runPostEditCompatibilityWork(parentSubmissionId, newSubmissionId, collectionName, formTemplateId, userId, newDoc, warnings);

        Map<String, Object> response = new HashMap<>();
        response.put("new_submission_id", newSubmissionId);
        response.put("state", parentState.dbValue());
        if (!warnings.isEmpty()) {
            response.put("warnings", warnings);
        }
        response.put("message", "Edited form data inserted with parent linkage.");
        return response;
    }

    @Override
    public Document editApprovalFlow(ApprovalFlowEditRequest request) {
        return new Document("approvalInstance", approvalInstanceService.editApprovalFlow(request));
    }

    @Override
    public List<Document> getVersionHistory(String submissionId, String collectionName) {
        Document initial = findSubmission(submissionId, collectionName);
        String versionGroupId = initial.getString("version_group_id");
        List<Document> rawVersions;
        if (versionGroupId == null || versionGroupId.isEmpty()) {
            rawVersions = List.of(initial);
        } else {
            Query query = new Query(Criteria.where("version_group_id").is(versionGroupId));
            query.with(Sort.by(Sort.Direction.DESC, "version"));
            rawVersions = mongoTemplate.find(query, Document.class, collectionName);
        }

        Long formTemplateId = parseTemplateId(collectionName);
        var optionItems = mongoUtils.getOptionItemsKeyValueMapping(formTemplateId);
        var templateMapping = mongoUtils.getFormTemplateKeyValueMapping(formTemplateId);
        return rawVersions.stream()
                .map(doc -> mongoUtils.formatRecord(doc, optionItems, templateMapping))
                .collect(Collectors.toList());
    }

    @Override
    public ApprovalInstanceDTO getApprovalInstance(String submissionId, String collectionName) {
        Document submission = findSubmission(submissionId, collectionName);
        if (!hasPairedApprovalInstance(submissionId, collectionName)) {
            return buildLegacyApprovalSummary(submission, collectionName);
        }
        ApprovalInstance instance = approvalInstanceService.getByFormSubmission(submissionId, collectionName);
        return toApprovalInstanceDto(instance, submission, formTemplateName(parseTemplateId(collectionName)));
    }

    @Override
    public List<?> getApprovalSteps(String submissionId, String collectionName) {
        Document submission = findSubmission(submissionId, collectionName);
        if (!hasPairedApprovalInstance(submissionId, collectionName)) {
            Object raw = submission.get("approval_info");
            return raw instanceof List<?> list ? list : List.of();
        }
        return approvalInstanceService.getApprovalSteps(submissionId, collectionName);
    }

    @Override
    public ApprovalInstanceDTO getApprovalInstanceById(String approvalInstanceId) {
        ApprovalInstance instance = approvalInstanceService.getById(approvalInstanceId);
        String collectionName = instance.getFormSubmissionCollectionName();
        Document submission = findSubmission(instance.getFormSubmissionId(), collectionName);
        return toApprovalInstanceDto(instance, submission, formTemplateName(parseTemplateId(collectionName)));
    }

    @Override
    public PagedResultDTO<ApprovalInstanceListItemDTO> getApprovalInstances(ApprovalInstanceQueryRequest request) {
        ApprovalInstanceQueryRequest safeRequest = request == null ? new ApprovalInstanceQueryRequest() : request;
        int page = Math.max(safeRequest.getPage(), 0);
        int size = safeRequest.getSize() <= 0 ? 10 : safeRequest.getSize();
        Sort sort = approvalInstanceSort(safeRequest);
        Criteria criteria = approvalInstanceCriteria(safeRequest);

        long countStartedAt = System.nanoTime();
        Query countQuery = new Query(criteria);
        long totalElements = mongoTemplate.count(countQuery, ApprovalInstance.class);
        long countMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - countStartedAt);

        long fetchStartedAt = System.nanoTime();
        Query pageQuery = new Query(criteria);
        pageQuery.with(sort);
        pageQuery.skip((long) page * size);
        pageQuery.limit(size);
        applyApprovalInstanceListingProjection(pageQuery);
        List<ApprovalInstance> instances = mongoTemplate.find(pageQuery, ApprovalInstance.class);
        long fetchMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - fetchStartedAt);

        long formFetchStartedAt = System.nanoTime();
        Map<String, Document> latestForms = safeRequest.isIncludeFormData()
                ? latestFormDocumentsByApprovalInstance(instances)
                : Map.of();
        long formFetchMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - formFetchStartedAt);

        Map<Long, String> formTemplateNames = formTemplateNamesById(instances);

        long dtoStartedAt = System.nanoTime();
        List<ApprovalInstanceListItemDTO> content = instances.stream()
                .map(instance -> toApprovalInstanceListItemDto(
                        instance,
                        latestForms.get(instance.getFormSubmissionId()),
                        safeRequest.isIncludeFormData(),
                        formTemplateNames
                ))
                .toList();
        long dtoMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - dtoStartedAt);

        log.info("getApprovalInstances count={}ms fetch={}ms formFetch={}ms dto={}ms, page={}, size={}, totalElements={}, includeFormData={}, sortField={}, approvalTemplateId={}, formSubmissionState={}",
                countMillis,
                fetchMillis,
                formFetchMillis,
                dtoMillis,
                page,
                size,
                totalElements,
                safeRequest.isIncludeFormData(),
                safeRequest.getSortField(),
                safeRequest.getApprovalTemplateId(),
                safeRequest.getFormSubmissionState());

        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new PagedResultDTO<>(content, totalElements, totalPages, page, size);
    }

    private Criteria approvalInstanceCriteria(ApprovalInstanceQueryRequest request) {
        List<Criteria> criteria = new ArrayList<>();
        
        if (request.getSubmissionId() != null && !request.getSubmissionId().isBlank()) {
            criteria.add(Criteria.where("formSubmissionId").is(request.getSubmissionId()));
        }
        if (request.getApprovalTemplateId() != null && !request.getApprovalTemplateId().isBlank()) {
            criteria.add(Criteria.where("approvalTemplateId").is(request.getApprovalTemplateId()));
        }
        if (request.getFormTemplateId() != null) {
            criteria.add(Criteria.where("filterSnapshot.formTemplateId").is(request.getFormTemplateId()));
        }
        if (request.getFormSubmissionState() != null && !request.getFormSubmissionState().isBlank()) {
            criteria.add(Criteria.where("filterSnapshot.formSubmissionState").is(request.getFormSubmissionState()));
        }
        if (request.getIsAlarmTriggered() != null) {
            criteria.add(Criteria.where("filterSnapshot.isAlarmTriggered").is(request.getIsAlarmTriggered()));
        }
        if (request.getSubmitterUserId() != null) {
            criteria.add(Criteria.where("filterSnapshot.createdBy").is(request.getSubmitterUserId()));
        }
        addCurrentRequiredApproverCriteria(criteria, request);
        addLongCriteria(criteria, "filterSnapshot.relatedInspectorIds", request.getInspectorUserId());
        addLongCriteria(criteria, "filterSnapshot.relatedProductIds", request.getSuggestedProductId());
        addLongCriteria(criteria, "filterSnapshot.relatedBatchIds", request.getSuggestedBatchId());
        addLongCriteria(criteria, "filterSnapshot.relatedTeamId", request.getTeamId());
        addLongCriteria(criteria, "filterSnapshot.relatedShiftId", request.getShiftId());
        Date createdAtStart = parseDate(request.getCreatedAtStart(), false);
        Date createdAtEnd = parseDate(request.getCreatedAtEnd(), true);
        if (createdAtStart != null || createdAtEnd != null) {
            Criteria createdAtCriteria = Criteria.where("filterSnapshot.createdAt");
            if (createdAtStart != null) {
                createdAtCriteria = createdAtCriteria.gte(createdAtStart);
            }
            if (createdAtEnd != null) {
                createdAtCriteria = createdAtCriteria.lte(createdAtEnd);
            }
            criteria.add(createdAtCriteria);
        }
        if (criteria.isEmpty()) {
            return new Criteria();
        }
        if (criteria.size() == 1) {
            return criteria.get(0);
        }
        return new Criteria().andOperator(criteria.toArray(new Criteria[0]));
    }

    private void addCurrentRequiredApproverCriteria(List<Criteria> criteria, ApprovalInstanceQueryRequest request) {
        boolean hasRequiredUser = request.getCurrentRequiredUserId() != null && !request.getCurrentRequiredUserId().isBlank();
        boolean hasRequiredRole = request.getCurrentRequiredRoleId() != null && !request.getCurrentRequiredRoleId().isBlank();
        if (!hasRequiredUser && !hasRequiredRole) {
            return;
        }

        List<Criteria> approverCriteria = new ArrayList<>();
        if (hasRequiredUser) {
            approverCriteria.add(Criteria.where("approvalSteps").elemMatch(
                    Criteria.where("stepState").is("IN_PROGRESS")
                            .and("requiredUserId").is(request.getCurrentRequiredUserId())
            ));
        }
        if (hasRequiredRole) {
            approverCriteria.add(Criteria.where("approvalSteps").elemMatch(
                    Criteria.where("stepState").is("IN_PROGRESS")
                            .and("requiredRoleId").is(request.getCurrentRequiredRoleId())
            ));
        }

        if (approverCriteria.size() == 1) {
            criteria.add(approverCriteria.get(0));
        } else {
            criteria.add(new Criteria().orOperator(approverCriteria.toArray(new Criteria[0])));
        }
    }

    private Sort approvalInstanceSort(ApprovalInstanceQueryRequest request) {
        String field = request.getSortField() == null || request.getSortField().isBlank()
                ? "created_at"
                : request.getSortField();
        String mongoField = switch (field) {
            case "submission_id", "submissionId" -> "formSubmissionId";
            case "collection_name", "collectionName" -> "formSubmissionCollectionName";
            case "form_template_id", "formTemplateId" -> "filterSnapshot.formTemplateId";
            case "form_submission_state", "formSubmissionState", "state" -> "filterSnapshot.formSubmissionState";
            case "approval_instance_id", "approvalInstanceId" -> "_id";
            case "approval_template_id", "approvalTemplateId" -> "approvalTemplateId";
            case "current_step_sequence", "currentStepSequence" -> "currentStepSequence";
            case "form_submission_version", "formSubmissionVersion" -> "filterSnapshot.formSubmissionVersion";
            case "created_by", "createdBy" -> "filterSnapshot.createdBy";
            case "created_at", "createdAt" -> "filterSnapshot.createdAt";
            default -> throw new InvalidRequestException("Unsupported sort field for approval-instance listing: " + field);
        };
        return Sort.by(request.getSortDirection(), mongoField).and(Sort.by(request.getSortDirection(), "_id"));
    }

    private Map<String, Document> latestFormDocumentsByApprovalInstance(List<ApprovalInstance> instances) {
        Map<String, Document> latestForms = new HashMap<>();
        Map<String, List<ApprovalInstance>> byCollection = instances.stream()
                .collect(Collectors.groupingBy(ApprovalInstance::getFormSubmissionCollectionName));
        for (Map.Entry<String, List<ApprovalInstance>> entry : byCollection.entrySet()) {
            List<ObjectId> ids = entry.getValue().stream()
                    .map(ApprovalInstance::getFormSubmissionId)
                    .filter(ObjectId::isValid)
                    .map(ObjectId::new)
                    .toList();
            if (ids.isEmpty()) {
                continue;
            }
            // include_form_data=true intentionally returns the full latest form document for current callers.
            Query query = new Query(Criteria.where("_id").in(ids));
            for (Document document : mongoTemplate.find(query, Document.class, entry.getKey())) {
                latestForms.put(document.getObjectId("_id").toString(), document);
            }
        }
        return latestForms;
    }

    private void applyApprovalInstanceListingProjection(Query query) {
        query.fields()
                .include("_id")
                .include("formSubmissionId")
                .include("formSubmissionCollectionName")
                .include("approvalTemplateId")
                .include("approvalTemplateName")
                .include("currentStepSequence")
                .include("approvalProcessStatus")
                .include("approvalSteps.sequence")
                .include("approvalSteps.stepName")
                .include("approvalSteps.requiredUserId")
                .include("approvalSteps.requiredRoleId")
                .include("approvalSteps.requiredType")
                .include("approvalSteps.stepState")
                .include("approvalSteps.resetCounter")
                .include("isAlarmTriggered")
                .include("filterSnapshot");
    }

    private ApprovalInstanceListItemDTO toApprovalInstanceListItemDto(ApprovalInstance instance, Document latestForm,
                                                                      boolean includeFormData, Map<Long, String> formTemplateNames) {
        FormSubmissionSnapshotForFilter snapshot = instance.getFilterSnapshot();
        Long formTemplateId = snapshot == null ? asLong(instance.getFormTemplateId()) : snapshot.getFormTemplateId();
        ApprovalInstanceListItemDTO dto = new ApprovalInstanceListItemDTO();
        dto.setFormSubmissionId(instance.getFormSubmissionId());
        dto.setCollectionName(instance.getFormSubmissionCollectionName());
        dto.setFormTemplateId(formTemplateId);
        dto.setFormTemplateName(formTemplateId == null ? null : formTemplateNames.get(formTemplateId));
        dto.setFormSubmissionState(snapshot == null ? null : snapshot.getFormSubmissionState());
        dto.setApprovalInstanceId(instance.getId());
        dto.setApprovalTemplateId(instance.getApprovalTemplateId());
        dto.setApprovalTemplateName(instance.getApprovalTemplateName());
        dto.setCurrentStepSequence(instance.getCurrentStepSequence());
        dto.setApprovalSteps(toApprovalInstanceListSteps(instance));
        dto.setApprovalProcessStatus(resolveApprovalProcessStatus(instance));
        dto.setFormSubmissionVersion(snapshot == null ? 1 : nullToOne(snapshot.getFormSubmissionVersion()));
        dto.setCreatedAt(snapshot == null ? null : snapshot.getCreatedAt());
        dto.setUpdatedAt(instance.getUpdatedAt());
        dto.setCreatedBy(snapshot == null ? null : snapshot.getCreatedBy());
        dto.setUpdatedBy(instance.getUpdatedBy());
        dto.setRelatedInspectorIds(snapshot == null ? null : snapshot.getRelatedInspectorIds());
        dto.setRelatedInspectors(snapshot == null ? null : snapshot.getRelatedInspectors());
        dto.setRelatedProductIds(snapshot == null ? null : snapshot.getRelatedProductIds());
        dto.setRelatedProducts(snapshot == null ? null : snapshot.getRelatedProducts());
        dto.setRelatedBatchIds(snapshot == null ? null : snapshot.getRelatedBatchIds());
        dto.setRelatedBatches(snapshot == null ? null : snapshot.getRelatedBatches());
        dto.setRelatedTeamId(snapshot == null ? null : snapshot.getRelatedTeamId());
        dto.setRelatedTeams(snapshot == null ? null : snapshot.getRelatedTeams());
        dto.setRelatedShiftId(snapshot == null ? null : snapshot.getRelatedShiftId());
        dto.setRelatedShifts(snapshot == null ? null : snapshot.getRelatedShifts());
        dto.setIsAlarmTriggered(snapshot == null ? instance.getIsAlarmTriggered() : snapshot.getIsAlarmTriggered());
        dto.setFormData(includeFormData && latestForm != null ? new HashMap<>(latestForm) : null);
        return dto;
    }

    private ApprovalInstanceDTO toApprovalInstanceDto(ApprovalInstance instance, Document submission, String formTemplateName) {
        FormSubmissionSnapshotForFilter snapshot = instance.getFilterSnapshot();
        Long formTemplateId = snapshot == null ? asLong(instance.getFormTemplateId()) : snapshot.getFormTemplateId();

        ApprovalInstanceDTO dto = new ApprovalInstanceDTO();
        dto.setApprovalInstanceId(instance.getId());
        dto.setApprovalTemplateId(instance.getApprovalTemplateId());
        dto.setApprovalTemplateName(instance.getApprovalTemplateName());
        dto.setCurrentStepSequence(instance.getCurrentStepSequence());
        dto.setApprovalSteps(toApprovalInstanceListSteps(instance));
        dto.setApprovalProcessStatus(resolveApprovalProcessStatus(instance));
        dto.setActionLog(formatApprovalActionLogs(instance.getActionLog(), formTemplateId));
        dto.setCreatedAt(snapshot == null ? null : snapshot.getCreatedAt());
        dto.setUpdatedAt(instance.getUpdatedAt());
        dto.setCreatedBy(snapshot == null ? null : snapshot.getCreatedBy());
        dto.setUpdatedBy(instance.getUpdatedBy());

        dto.setFormSubmissionId(instance.getFormSubmissionId());
        dto.setFormSubmissionState(snapshot == null ? resolveLifecycleState(submission).dbValue() : snapshot.getFormSubmissionState());
        dto.setFormSubmissionVersion(snapshot == null ? submission.getInteger("version", 1) : nullToOne(snapshot.getFormSubmissionVersion()));

        dto.setCollectionName(instance.getFormSubmissionCollectionName());
        dto.setFormTemplateId(formTemplateId);
        dto.setFormTemplateName(formTemplateName);

        dto.setRelatedInspectorIds(snapshot == null ? submission.get("related_inspector_ids") : snapshot.getRelatedInspectorIds());
        dto.setRelatedInspectors(snapshot == null ? submission.get("related_inspectors") : snapshot.getRelatedInspectors());
        dto.setRelatedProductIds(snapshot == null ? submission.get("related_product_ids") : snapshot.getRelatedProductIds());
        dto.setRelatedProducts(snapshot == null ? submission.get("related_products") : snapshot.getRelatedProducts());
        dto.setRelatedBatchIds(snapshot == null ? submission.get("related_batch_ids") : snapshot.getRelatedBatchIds());
        dto.setRelatedBatches(snapshot == null ? submission.get("related_batches") : snapshot.getRelatedBatches());
        dto.setRelatedTeamId(snapshot == null ? submission.get("related_team_id") : snapshot.getRelatedTeamId());
        dto.setRelatedTeams(snapshot == null ? submission.get("related_teams") : snapshot.getRelatedTeams());
        dto.setRelatedShiftId(snapshot == null ? submission.get("related_shift_id") : snapshot.getRelatedShiftId());
        dto.setRelatedShifts(snapshot == null ? submission.get("related_shifts") : snapshot.getRelatedShifts());
        dto.setIsAlarmTriggered(snapshot == null ? instance.getIsAlarmTriggered() : snapshot.getIsAlarmTriggered());
        dto.setFormData(new HashMap<>(submission));
        return dto;
    }

    private Map<Long, String> formTemplateNamesById(List<ApprovalInstance> instances) {
        if (instances == null || instances.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> namesById = new HashMap<>();
        for (ApprovalInstance instance : instances) {
            Long formTemplateId = instance.getFilterSnapshot() == null
                    ? asLong(instance.getFormTemplateId())
                    : instance.getFilterSnapshot().getFormTemplateId();
            if (formTemplateId == null || namesById.containsKey(formTemplateId)) {
                continue;
            }
            try {
                QcFormTemplateDTO template = qcFormTemplateService.getTemplateById(formTemplateId);
                namesById.put(formTemplateId, template == null ? null : template.getName());
            } catch (RuntimeException e) {
                log.warn("Unable to resolve form template name for id={}", formTemplateId, e);
                namesById.put(formTemplateId, null);
            }
        }
        return namesById;
    }

    private List<ApprovalInstanceListStepDTO> toApprovalInstanceListSteps(ApprovalInstance instance) {
        if (instance.getApprovalSteps() == null || instance.getApprovalSteps().isEmpty()) {
            return List.of();
        }
        return instance.getApprovalSteps().stream()
                .map(this::toApprovalInstanceListStep)
                .toList();
    }

    private ApprovalInstanceListStepDTO toApprovalInstanceListStep(ApprovalInstanceStep step) {
        ApprovalInstanceListStepDTO dto = new ApprovalInstanceListStepDTO();
        dto.setSequence(step.getSequence());
        dto.setStepName(step.getStepName());
        dto.setRequiredUserId(step.getRequiredUserId());
        dto.setRequiredRoleId(step.getRequiredRoleId());
        dto.setRequiredType(step.getRequiredType());
        dto.setStepState(step.getStepState());
        dto.setResetCounter(step.getResetCounter());
        return dto;
    }

    // private void createLegacyApprovalAssignment(String submissionId, String collectionName, Long formTemplateId, String templateName, String approvalType) {
    //     QcApprovalAssignmentDTO assignmentDTO = new QcApprovalAssignmentDTO();
    //     assignmentDTO.setSubmissionId(submissionId);
    //     assignmentDTO.setQcFormTemplateId(formTemplateId);
    //     assignmentDTO.setQcFormTemplateName(templateName);
    //     assignmentDTO.setMongoCollection(collectionName);
    //     assignmentDTO.setApprovalType(approvalType);
    //     if ("flow_1".equals(approvalType)) {
    //         assignmentDTO.setState("fully_approved");
    //     } else if ("flow_3".equals(approvalType)) {
    //         assignmentDTO.setState("pending_supervisor");
    //     } else {
    //         assignmentDTO.setState("pending_leader");
    //     }
    //     qcApprovalAssignmentService.insertIfNotExists(assignmentDTO);
    // }

    private List<Document> normalizeStepsForDraft(List<?> rawSteps) {
        List<Document> steps = toDocumentList(rawSteps);
        for (Document step : steps) {
            String role = step.getString("role");
            if ("submitter".equals(role)) {
                step.put("status", "completed");
            } else {
                step.put("status", "not_started");
                step.put("timestamp", null);
            }
        }
        return steps;
    }

    /**
     * Compensates for approval-instance failures after a form document has already been inserted.
     */
    private void deleteSubmissionAfterApprovalInstanceFailure(String submissionId, String collectionName, RuntimeException failure) {
        try {
            mongoTemplate.remove(submissionIdQuery(submissionId), collectionName);
        } catch (RuntimeException deleteFailure) {
            log.error("Failed to remove submission {} after approval instance failure", submissionId, deleteFailure);
        }
    }

    private void runPostInsertCompatibilityWork(
            String submissionId,
            String collectionName,
            Long formTemplateId,
            String templateName,
            String approvalType,
            Long userId,
            Map<String, Object> formData,
            List<String> warnings
    ) {
        // Compat: qc_approval_assignment row written for all submissions (including V2) to support old
        // frontend clients during the deployment window. Remove once the window is closed.
        // runCompatibilityStep("legacy_approval_assignment_failed", submissionId, collectionName, userId, warnings,
        //         () -> createLegacyApprovalAssignment(submissionId, collectionName, formTemplateId, templateName, approvalType));
        runCompatibilityStep("alert_evaluation_failed", submissionId, collectionName, userId, warnings,
                () -> controlLimitEvaluationService.evaluateAndTriggerAlerts(formTemplateId, userId, formData, submissionId));
    }

    private void runPostEditCompatibilityWork(
            String parentSubmissionId,
            String newSubmissionId,
            String collectionName,
            Long formTemplateId,
            Long userId,
            Map<String, Object> newDoc,
            List<String> warnings
    ) {
        runCompatibilityStep("snapshot_cleanup_failed", newSubmissionId, collectionName, userId, warnings,
                () -> qcSnapshotSubmissionService.deleteBySubmissionId(parentSubmissionId));
        runCompatibilityStep("alert_evaluation_failed", newSubmissionId, collectionName, userId, warnings,
                () -> controlLimitEvaluationService.evaluateAndTriggerAlerts(formTemplateId, userId, newDoc, newSubmissionId));
    }

    /**
     * Runs legacy side effects without allowing them to corrupt core form/approval state.
     */
    private void runCompatibilityStep(
            String actionType,
            String submissionId,
            String collectionName,
            Long userId,
            List<String> warnings,
            Runnable action
    ) {
        try {
            action.run();
        } catch (RuntimeException e) {
            String warning = actionType + ": " + e.getMessage();
            warnings.add(warning);
            log.warn("Post-core compatibility step failed for submission {}: {}", submissionId, warning, e);
        }
    }

    private List<Document> toDocumentList(List<?> rawSteps) {
        List<Document> steps = new ArrayList<>();
        if (rawSteps == null) {
            return steps;
        }
        for (Object rawStep : rawSteps) {
            if (rawStep instanceof Document document) {
                steps.add(new Document(document));
            } else if (rawStep instanceof Map<?, ?> map) {
                Document document = new Document();
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    document.put(String.valueOf(entry.getKey()), entry.getValue());
                }
                steps.add(document);
            }
        }
        return steps;
    }

    private int nullToOne(Integer value) {
        return value == null ? 1 : value;
    }

    private Long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private Document findSubmission(String submissionId, String collectionName) {
        if (!ObjectId.isValid(submissionId)) {
            throw new IllegalArgumentException("Invalid submissionId format: " + submissionId);
        }
        Document document = mongoTemplate.findOne(submissionIdQuery(submissionId), Document.class, collectionName);
        if (document == null) {
            throw new IllegalArgumentException("Submission not found: " + submissionId);
        }
        return document;
    }

    /**
     * Returns a normalized legacy approval summary for the deprecated getApprovalInstance endpoint.
     * Wraps embedded approval_info with model/state context so callers get a consistent shape.
     */
    private ApprovalInstanceDTO buildLegacyApprovalSummary(Document submission, String collectionName) {
        ApprovalInstanceDTO dto = new ApprovalInstanceDTO();
        dto.setFormSubmissionId(submission.getObjectId("_id").toString());
        dto.setFormSubmissionState(resolveLifecycleState(submission).dbValue());
        dto.setFormSubmissionVersion(submission.getInteger("version", 1));
        dto.setCollectionName(collectionName);
        dto.setFormTemplateId(parseTemplateId(collectionName));
        dto.setFormTemplateName(formTemplateName(dto.getFormTemplateId()));
        List<ApprovalInstanceListStepDTO> approvalSteps = toLegacyApprovalSteps(submission.get("approval_info"));
        dto.setApprovalSteps(approvalSteps);
        dto.setApprovalProcessStatus(deriveApprovalProcessStatus(approvalSteps));
        dto.setFormData(new HashMap<>(submission));
        return dto;
    }

    private List<ApprovalActionLog> formatApprovalActionLogs(List<ApprovalActionLog> actionLogs, Long formTemplateId) {
        if (actionLogs == null || actionLogs.isEmpty()) {
            return List.of();
        }
        return actionLogs.stream()
                .map(log -> copyApprovalActionLog(log, formTemplateId))
                .toList();
    }

    private ApprovalActionLog copyApprovalActionLog(ApprovalActionLog source, Long formTemplateId) {
        // Copy into a response-only object so stored audit data stays raw in Mongo.
        ApprovalActionLog copy = new ApprovalActionLog();
        copy.setLogId(source.getLogId());
        copy.setAction(source.getAction());
        copy.setStepSequence(source.getStepSequence());
        copy.setComments(source.getComments());
        copy.setESignature(source.getESignature());
        copy.setSuggestRetest(source.getSuggestRetest());
        copy.setActorUserId(source.getActorUserId());
        copy.setActorUserName(source.getActorUserName());
        copy.setActorRoleId(source.getActorRoleId());
        copy.setActorRoleName(source.getActorRoleName());
        copy.setFormSubmissionSnapshot(mongoUtils.formatSubmissionSnapshotForResponse(source.getFormSubmissionSnapshot(), formTemplateId));
        copy.setFormTemplateSnapshot(source.getFormTemplateSnapshot());
        copy.setActedAt(source.getActedAt());
        return copy;
    }

    private String formTemplateName(Long formTemplateId) {
        if (formTemplateId == null) {
            return null;
        }
        QcFormTemplateDTO template = qcFormTemplateService.getTemplateById(formTemplateId);
        return template == null ? null : template.getName();
    }

    private List<ApprovalInstanceListStepDTO> toLegacyApprovalSteps(Object rawApprovalInfo) {
        if (!(rawApprovalInfo instanceof List<?> rawSteps) || rawSteps.isEmpty()) {
            return List.of();
        }

        List<ApprovalInstanceListStepDTO> steps = new ArrayList<>();
        int sequence = 1;
        for (Object rawStep : rawSteps) {
            if (!(rawStep instanceof Map<?, ?> rawMap)) {
                continue;
            }

            String role = stringValue(rawMap.get("role"));
            if ("submitter".equals(role) || "archive".equals(role)) {
                continue;
            }

            ApprovalInstanceListStepDTO dto = new ApprovalInstanceListStepDTO();
            dto.setSequence(sequence++);
            dto.setStepName(stringValue(rawMap.get("label")));
            dto.setRequiredType("role");
            dto.setRequiredRoleId(role);
            dto.setStepState(legacyStepState(stringValue(rawMap.get("status"))));
            dto.setResetCounter(0);
            steps.add(dto);
        }
        return steps;
    }

    private ApprovalStepState legacyStepState(String status) {
        return switch (status == null ? "" : status) {
            case "completed" -> ApprovalStepState.APPROVED;
            case "pending" -> ApprovalStepState.IN_PROGRESS;
            case "not_started" -> ApprovalStepState.PENDING;
            default -> ApprovalStepState.PENDING;
        };
    }

    private String resolveApprovalProcessStatus(ApprovalInstance instance) {
        if (instance.getApprovalProcessStatus() != null && !instance.getApprovalProcessStatus().isBlank()) {
            return instance.getApprovalProcessStatus();
        }
        return ApprovalProcessStatusResolver.deriveDbValue(instance.getApprovalSteps());
    }

    private String deriveApprovalProcessStatus(List<ApprovalInstanceListStepDTO> approvalSteps) {
        return ApprovalProcessStatusResolver.deriveDbValueFromStates(approvalSteps.stream()
                .map(ApprovalInstanceListStepDTO::getStepState)
                .toList());
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private boolean hasPairedApprovalInstance(String submissionId, String collectionName) {
        try {
            approvalInstanceService.getByFormSubmission(submissionId, collectionName);
            return true;
        } catch (ApprovalInstanceException e) {
            return false;
        }
    }

    private FormSubmissionState resolveLifecycleState(Document submission) {
        String state = submission.getString("state");
        if (state != null && !state.isBlank()) {
            try {
                return FormSubmissionState.fromValue(state);
            } catch (IllegalArgumentException ignored) {
                return FormSubmissionState.ARCHIVED;
            }
        }

        return FormSubmissionState.ARCHIVED;
    }

    private Query submissionIdQuery(String submissionId) {
        return new Query(Criteria.where("_id").is(new ObjectId(submissionId)));
    }

    private Long parseTemplateId(String collectionName) {
        String[] parts = collectionName.split("_");
        if (parts.length < 3) {
            throw new IllegalArgumentException("Invalid collection name format: " + collectionName);
        }
        try {
            return Long.parseLong(parts[2]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid form template ID in collection name: " + collectionName, e);
        }
    }

    private void ensureCollectionExists(String collectionName, Long formTemplateId) {
        if (mongoTemplate.collectionExists(collectionName)) {
            return;
        }
        QcFormTemplateDTO template = qcFormTemplateService.getTemplateById(formTemplateId);
        if (template == null) {
            throw new IllegalArgumentException("Error: Template ID " + formTemplateId + " does not exist. Cannot create collection.");
        }
        mongoTemplate.createCollection(collectionName);
        formSubmissionIndexManager.ensureFormSubmissionIndexes(collectionName);
        log.info("Created new collection: {}", collectionName);
    }

    private void enrichNewDocWithParentData(Map<String, Object> newDoc, Document parent) {
        for (Map.Entry<String, Object> entry : parent.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (key.startsWith("related_") && !newDoc.containsKey(key)) {
                newDoc.put(key, value);
            }
        }
        if (parent.containsKey("approval_info")) {
            newDoc.put("approval_info", parent.get("approval_info"));
        }
    }

    private void addLongCriteria(List<Criteria> criteria, String fieldName, Long value) {
        if (value != null) {
            criteria.add(Criteria.where(fieldName).is(value));
        }
    }

    private Date parseDate(String value, boolean endOfDay) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Date.from(Instant.parse(value));
        } catch (RuntimeException ignored) {
            // Accept date-only values for filter controls that do not send full timestamps.
        }
        try {
            return Date.from(OffsetDateTime.parse(value).toInstant());
        } catch (RuntimeException ignored) {
        }
        try {
            return Date.from(LocalDateTime.parse(value).toInstant(ZoneOffset.UTC));
        } catch (RuntimeException ignored) {
        }
        LocalDate date = LocalDate.parse(value);
        Instant instant = endOfDay
                ? date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).minusMillis(1)
                : date.atStartOfDay().toInstant(ZoneOffset.UTC);
        return Date.from(instant);
    }
}
