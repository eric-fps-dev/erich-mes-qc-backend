package com.fps.svmes.services.impl;

import com.fps.svmes.dto.PagedResultDTO;
import com.fps.svmes.dto.dtos.alert.ExceededFieldInfoDTO;
import com.fps.svmes.dto.dtos.qcForm.ApprovalInstanceListItemDTO;
import com.fps.svmes.dto.dtos.qcForm.ApprovalInstanceListStepDTO;
import com.fps.svmes.dto.dtos.qcForm.QcApprovalAssignmentDTO;
import com.fps.svmes.dto.dtos.qcForm.QcFormTemplateDTO;
import com.fps.svmes.dto.requests.ApprovalInstanceQueryRequest;
import com.fps.svmes.dto.requests.ApprovalFlowEditRequest;
import com.fps.svmes.dto.requests.FormSubmissionActionRequest;
import com.fps.svmes.enums.form.FormSubmissionState;
import com.fps.svmes.models.nosql.approval.ApprovalInstance;
import com.fps.svmes.models.nosql.approval.ApprovalInstanceFilterSnapshot;
import com.fps.svmes.models.nosql.approval.ApprovalInstanceStep;
import com.fps.svmes.services.ApprovalInfoGeneratorService;
import com.fps.svmes.services.ApprovalInstanceService;
import com.fps.svmes.services.ControlLimitEvaluationService;
import com.fps.svmes.services.QcApprovalAssignmentService;
import com.fps.svmes.services.QcFormDataService;
import com.fps.svmes.services.QcFormTemplateService;
import com.fps.svmes.services.QcSnapshotSubmissionService;
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
import java.util.regex.Pattern;
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
    private final QcApprovalAssignmentService qcApprovalAssignmentService;
    private final QcSnapshotSubmissionService qcSnapshotSubmissionService;
    private final MongoFormTemplateUtils mongoUtils;
    private final ApprovalInstanceService approvalInstanceService;
    private final FormSubmissionIndexManager formSubmissionIndexManager;

    @Override
    public Map<String, Object> insertFormData(String collectionName, Long userId, Map<String, Object> formData) {
        Long formTemplateId = parseTemplateId(collectionName);
        ensureCollectionExists(collectionName, formTemplateId);

        Map<String, Object> document = new HashMap<>(formData);
        document.put("version_group_id", UUID.randomUUID().toString());
        document.put("version", 1);
        document.put("created_at", new Date());
        document.put("created_by", userId);
        document.put("state", FormSubmissionState.DRAFT.dbValue());

        Map<String, ExceededFieldInfoDTO> exceededInfoMap = controlLimitEvaluationService.evaluateExceededInfo(formTemplateId, formData);
        document.put("exceeded_info", exceededInfoMap);

        // can be depreciated later on
        String approvalType = qcFormTemplateService.getApprovalTypeByFormId(formTemplateId);
        List<Map<String, Object>> approvalInfo = approvalInfoGeneratorService.generateApprovalInfo(approvalType, userId);
        List<Document> draftSteps = normalizeStepsForDraft(approvalInfo);
        document.put("approval_info", draftSteps);

        QcFormTemplateDTO template = qcFormTemplateService.getTemplateById(formTemplateId);
        String approvalTemplateId = template.getApprovalTemplateId();
        if (approvalTemplateId == null || approvalTemplateId.isBlank()) {
            throw new IllegalStateException("Form template has no approval_template_id: " + formTemplateId);
        }
        approvalInstanceService.validateApprovalTemplateExists(approvalTemplateId);

        Document insertedDocument = mongoTemplate.insert(new Document(document), collectionName);
        String submissionId = insertedDocument.getObjectId("_id").toString();

        // Standalone Mongo has no transaction here; void the form if its approval instance cannot be created.
        try {
            approvalInstanceService.create(submissionId, collectionName, formTemplateId, approvalTemplateId, userId);
        } catch (RuntimeException e) {
            markSubmissionVoidAfterApprovalInstanceFailure(submissionId, collectionName, userId, e);
            throw e;
        }

        List<String> warnings = new ArrayList<>();
        runPostInsertCompatibilityWork(submissionId, collectionName, formTemplateId, approvalType, userId, formData, warnings);

        Map<String, Object> response = new HashMap<>();
        response.put("object_id", submissionId);
        response.put("state", FormSubmissionState.DRAFT.dbValue());
        response.put("version", insertedDocument.getInteger("version", 1));
        response.put("version_group_id", insertedDocument.getString("version_group_id"));
        if (!warnings.isEmpty()) {
            response.put("warnings", warnings);
        }
        response.put("message", "Form data inserted successfully to " + collectionName);
        return response;
    }

    @Override
    public Map<String, Object> editFormData(String collectionName, Long userId, String parentSubmissionId, Long formTemplateId,
                                            FormSubmissionState previousRecordState, Map<String, Object> updatedData) {
        Document parent = findSubmission(parentSubmissionId, collectionName);
        requireState(parent, List.of(FormSubmissionState.DRAFT, FormSubmissionState.PENDING_REVISION),
                "Only draft or pending revision form entries can be edited.");
        validatePreviousRecordState(previousRecordState);

        Map<String, Object> newDoc = new HashMap<>(updatedData);
        enrichNewDocWithParentData(newDoc, parent);

        String versionGroupId = parent.getString("version_group_id");
        Integer parentVersion = parent.getInteger("version");
        if (versionGroupId == null || parentVersion == null) {
            versionGroupId = UUID.randomUUID().toString();
            parentVersion = 1;
            parent.put("version_group_id", versionGroupId);
            parent.put("version", parentVersion);
        }
        newDoc.put("version_group_id", versionGroupId);
        newDoc.put("version", parentVersion + 1);
        newDoc.put("created_at", new Date());
        newDoc.put("created_by", userId);
        newDoc.put("state", resolveState(parent).dbValue());

        Map<String, ExceededFieldInfoDTO> exceededInfoMap =
                controlLimitEvaluationService.evaluateExceededInfo(formTemplateId, updatedData);
        newDoc.put("exceeded_info", exceededInfoMap);

        Document inserted = mongoTemplate.insert(new Document(newDoc), collectionName);
        String newSubmissionId = inserted.getObjectId("_id").toString();

        // Update the approval pointer before voiding the prior version to keep retries safer.
        try {
            approvalInstanceService.onFormSubmissionEdited(parentSubmissionId, newSubmissionId, collectionName, userId);
        } catch (RuntimeException e) {
            markSubmissionVoidAfterApprovalInstanceFailure(newSubmissionId, collectionName, userId, e);
            throw e;
        }

        parent.put("state", previousRecordState.dbValue());
        parent.put("updated_at", new Date());
        mongoTemplate.save(parent, collectionName);

        List<String> warnings = new ArrayList<>();
        runPostEditCompatibilityWork(parentSubmissionId, newSubmissionId, collectionName, formTemplateId, userId, newDoc, warnings);

        Map<String, Object> response = new HashMap<>();
        response.put("new_submission_id", newSubmissionId);
        response.put("state", resolveState(parent).dbValue());
        if (!warnings.isEmpty()) {
            response.put("warnings", warnings);
        }
        response.put("message", "Edited form data inserted with parent linkage.");
        return response;
    }

    @Override
    public void voidFormSubmission(FormSubmissionActionRequest request) {
        Document submission = findSubmission(request.getSubmissionId(), request.getCollectionName());
        FormSubmissionState state = resolveState(submission);
        if (!List.of(FormSubmissionState.DRAFT, FormSubmissionState.UNDER_REVIEW, FormSubmissionState.ARCHIVED).contains(state)) {
            throw new IllegalStateException("Only draft, under review, or archived form entries can be voided.");
        }
        approvalInstanceService.voidForFormSubmissionDelete(request);
        setSubmissionState(request.getSubmissionId(), request.getCollectionName(), FormSubmissionState.VOID);
        approvalInstanceService.refreshFilterSnapshot(request.getSubmissionId(), request.getCollectionName(), request.getActorUserId());
    }

    @Override
    public void submitForApproval(FormSubmissionActionRequest request) {
        approvalInstanceService.submitForApproval(request);
    }

    @Override
    public void recall(FormSubmissionActionRequest request) {
        approvalInstanceService.recall(request);
    }

    @Override
    public void approve(FormSubmissionActionRequest request) {
        approvalInstanceService.approve(request);
    }

    @Override
    public void forward(FormSubmissionActionRequest request) {
        approvalInstanceService.forward(request);
    }

    @Override
    public void rejectFullRedo(FormSubmissionActionRequest request) {
        approvalInstanceService.rejectFullReset(request);
    }

    @Override
    public void rejectPartialRedo(FormSubmissionActionRequest request) {
        approvalInstanceService.rejectPartialReset(request);
    }

    @Override
    public void rejectDiscard(FormSubmissionActionRequest request) {
        approvalInstanceService.rejectDiscard(request);
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
        return rawVersions.stream()
                .map(doc -> mongoUtils.formatRecord(
                        doc,
                        mongoUtils.getOptionItemsKeyValueMapping(formTemplateId),
                        mongoUtils.getFormTemplateKeyValueMapping(formTemplateId)
                ))
                .collect(Collectors.toList());
    }

    @Override
    public Object getApprovalInstance(String submissionId, String collectionName) {
        return approvalInstanceService.getByFormSubmissionIncludingVoid(submissionId, collectionName);
    }

    @Override
    public List<?> getApprovalSteps(String submissionId, String collectionName) {
        return approvalInstanceService.getApprovalSteps(submissionId, collectionName);
    }

    @Override
    public ApprovalInstance getApprovalInstanceById(String approvalInstanceId) {
        return approvalInstanceService.getByIdIncludingVoid(approvalInstanceId);
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
        criteria.add(Criteria.where("status").is(1));
        criteria.add(Criteria.where("filterSnapshot.formSubmissionState").ne(FormSubmissionState.VOID.dbValue()));
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
            case "approval_instance_version", "approvalInstanceVersion" -> "versionNumber";
            case "form_submission_version", "formSubmissionVersion" -> "filterSnapshot.formSubmissionVersion";
            case "created_by", "createdBy" -> "filterSnapshot.createdBy";
            case "created_at", "createdAt" -> "filterSnapshot.createdAt";
            default -> throw new IllegalArgumentException("Unsupported sort field for approval-instance listing: " + field);
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
                .include("currentStepSequence")
                .include("approvalSteps.sequence")
                .include("approvalSteps.stepName")
                .include("approvalSteps.requiredUserId")
                .include("approvalSteps.requiredRoleId")
                .include("approvalSteps.requiredType")
                .include("approvalSteps.stepState")
                .include("approvalSteps.resetCounter")
                .include("versionNumber")
                .include("filterSnapshot")
                .include("status");
    }

    private ApprovalInstanceListItemDTO toApprovalInstanceListItemDto(ApprovalInstance instance, Document latestForm,
                                                                      boolean includeFormData, Map<Long, String> formTemplateNames) {
        ApprovalInstanceFilterSnapshot snapshot = instance.getFilterSnapshot();
        Long formTemplateId = snapshot == null ? asLong(instance.getFormTemplateId()) : snapshot.getFormTemplateId();
        ApprovalInstanceListItemDTO dto = new ApprovalInstanceListItemDTO();
        dto.setSubmissionId(instance.getFormSubmissionId());
        dto.setCollectionName(instance.getFormSubmissionCollectionName());
        dto.setFormTemplateId(formTemplateId);
        dto.setFormTemplateName(formTemplateId == null ? null : formTemplateNames.get(formTemplateId));
        dto.setFormSubmissionState(snapshot == null ? null : snapshot.getFormSubmissionState());
        dto.setApprovalInstanceId(instance.getId());
        dto.setApprovalTemplateId(instance.getApprovalTemplateId());
        dto.setCurrentStepSequence(instance.getCurrentStepSequence());
        dto.setApprovalSteps(toApprovalInstanceListSteps(instance));
        dto.setApprovalInstanceVersion(nullToOne(instance.getVersionNumber()));
        dto.setFormSubmissionVersion(snapshot == null ? 1 : nullToOne(snapshot.getFormSubmissionVersion()));
        dto.setCreatedAt(snapshot == null ? null : snapshot.getCreatedAt());
        dto.setUpdatedAt(instance.getUpdatedAt());
        dto.setCreatedBy(snapshot == null ? null : snapshot.getCreatedBy());
        dto.setUpdatedBy(instance.getUpdatedBy());
        dto.setRelatedInspectorIds(snapshot == null ? null : snapshot.getRelatedInspectorIds());
        dto.setRelatedProductIds(snapshot == null ? null : snapshot.getRelatedProductIds());
        dto.setRelatedBatchIds(snapshot == null ? null : snapshot.getRelatedBatchIds());
        dto.setRelatedTeamId(snapshot == null ? null : snapshot.getRelatedTeamId());
        dto.setRelatedShiftId(snapshot == null ? null : snapshot.getRelatedShiftId());
        dto.setFormData(includeFormData && latestForm != null ? new HashMap<>(latestForm) : null);
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

    private void createLegacyApprovalAssignment(String submissionId, String collectionName, Long formTemplateId, String approvalType) {
        QcApprovalAssignmentDTO assignmentDTO = new QcApprovalAssignmentDTO();
        assignmentDTO.setSubmissionId(submissionId);
        assignmentDTO.setQcFormTemplateId(formTemplateId);
        QcFormTemplateDTO template = qcFormTemplateService.getTemplateById(formTemplateId);
        assignmentDTO.setQcFormTemplateName(template.getName());
        assignmentDTO.setMongoCollection(collectionName);
        assignmentDTO.setApprovalType(approvalType);
        if ("flow_1".equals(approvalType)) {
            assignmentDTO.setState("fully_approved");
        } else if ("flow_3".equals(approvalType)) {
            assignmentDTO.setState("pending_supervisor");
        } else {
            assignmentDTO.setState("pending_leader");
        }
        qcApprovalAssignmentService.insertIfNotExists(assignmentDTO);
    }

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

    private void setSubmissionState(String submissionId, String collectionName, FormSubmissionState state) {
        Update update = new Update()
                .set("state", state.dbValue())
                .set("updated_at", new Date());
        UpdateResult result = mongoTemplate.updateFirst(submissionIdQuery(submissionId), update, collectionName);
        if (result.getMatchedCount() == 0) {
            throw new IllegalStateException("Form submission state update failed because submission was not found: " + submissionId);
        }
    }

    /**
     * Compensates for approval-instance failures after a form document has already been inserted.
     */
    private void markSubmissionVoidAfterApprovalInstanceFailure(String submissionId, String collectionName, Long userId, RuntimeException failure) {
        try {
            setSubmissionState(
                    submissionId,
                    collectionName,
                    FormSubmissionState.VOID
            );
        } catch (RuntimeException voidFailure) {
            log.error("Failed to void submission {} after approval instance failure", submissionId, voidFailure);
        }
    }

    private void runPostInsertCompatibilityWork(
            String submissionId,
            String collectionName,
            Long formTemplateId,
            String approvalType,
            Long userId,
            Map<String, Object> formData,
            List<String> warnings
    ) {
        runCompatibilityStep("legacy_approval_assignment_failed", submissionId, collectionName, userId, warnings,
                () -> createLegacyApprovalAssignment(submissionId, collectionName, formTemplateId, approvalType));
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
        runCompatibilityStep("legacy_approval_assignment_update_failed", newSubmissionId, collectionName, userId, warnings,
                () -> qcApprovalAssignmentService.updateSubmissionId(parentSubmissionId, newSubmissionId));
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

    private void requireState(Document submission, List<FormSubmissionState> requiredStates, String message) {
        if (!requiredStates.contains(resolveState(submission))) {
            throw new IllegalStateException(message);
        }
    }

    private void validatePreviousRecordState(FormSubmissionState previousRecordState) {
        if (!List.of(FormSubmissionState.VOID, FormSubmissionState.ARCHIVED).contains(previousRecordState)) {
            throw new IllegalArgumentException("previous_record_state must be either 'void' or 'archived'.");
        }
    }

    /**
     * Resolves missing lifecycle state for legacy documents that only have approval_info.
     */
    private FormSubmissionState resolveState(Document submission) {
        String state = submission.getString("state");
        if (state != null && !state.isBlank()) {
            return FormSubmissionState.fromValue(state);
        }
        if (legacyApprovalComplete(submission)) {
            return FormSubmissionState.ARCHIVED;
        }
        if (legacyApprovalInProgress(submission)) {
            return FormSubmissionState.UNDER_REVIEW;
        }
        return FormSubmissionState.DRAFT;
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

    @SuppressWarnings("unchecked")
    private List<Document> legacyApprovalInfo(Document document) {
        Object approvalInfo = document.get("approval_info");
        return approvalInfo instanceof List<?> list ? toDocumentList((List<?>) list) : List.of();
    }

    private boolean legacyApprovalComplete(Document document) {
        List<Document> approvalInfo = legacyApprovalInfo(document);
        return !approvalInfo.isEmpty() && approvalInfo.stream()
                .allMatch(step -> "completed".equals(step.getString("status")));
    }

    private boolean legacyApprovalInProgress(Document document) {
        return legacyApprovalInfo(document).stream()
                .anyMatch(step -> "pending".equals(step.getString("status")));
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
