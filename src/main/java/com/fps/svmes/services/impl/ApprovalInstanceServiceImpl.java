package com.fps.svmes.services.impl;

import com.fps.shared.entity.primary.approval.ApprovalStep;
import com.fps.shared.entity.primary.approval.ApprovalTemplate;
import com.fps.shared.entity.primary.rbac.Role;
import com.fps.shared.entity.primary.user.User;
import com.fps.svmes.dto.LegacyMigrationResult;
import com.fps.svmes.dto.requests.ApprovalStepRequest;
import com.fps.svmes.dto.requests.ApprovalFlowEditRequest;
import com.fps.svmes.dto.requests.FormSubmissionActionRequest;
import com.fps.svmes.enums.approval.ApprovalAction;
import com.fps.svmes.enums.approval.ApprovalStepState;
import com.fps.svmes.enums.form.FormSubmissionState;
import com.fps.svmes.exceptions.ApprovalInstanceException;
import com.fps.svmes.models.nosql.approval.ApprovalActionLog;
import com.fps.svmes.models.nosql.approval.ApprovalInstance;
import com.fps.svmes.models.nosql.approval.FormSubmissionSnapshotForFilter;
import com.fps.svmes.models.nosql.approval.ApprovalInstanceStep;
import com.fps.svmes.repositories.jpaRepo.user.RoleRepository;
import com.fps.svmes.repositories.jpaRepo.user.UserRepository;
import com.fps.svmes.repositories.mongoRepo.ApprovalInstanceRepository;
import com.fps.svmes.services.ApprovalInstanceService;
import com.fps.svmes.services.ApprovalTemplateLookupService;
import com.fps.svmes.services.FormSubmissionStateUpdater;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Owns approval-instance step progress, audit logging, and form-submission state signaling.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApprovalInstanceServiceImpl implements ApprovalInstanceService {
    private static final int BACKFILL_BATCH_SIZE = 200;
    private static final String FORM_COLLECTION_PREFIX = "form_template_";

    private final MongoTemplate mongoTemplate;
    private final ApprovalInstanceRepository approvalInstanceRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ApprovalTemplateLookupService approvalTemplateLookupService;
    private final FormSubmissionStateUpdater formSubmissionStateUpdater;

    @Override
    public ApprovalInstance create(String formSubmissionId, String formSubmissionCollectionName, Long formTemplateId, String approvalTemplateId, Long createdBy) {
        boolean hasTemplate = approvalTemplateId != null && !approvalTemplateId.isBlank();

        ApprovalInstance instance = new ApprovalInstance();
        instance.setFormSubmissionId(formSubmissionId);
        instance.setFormSubmissionCollectionName(formSubmissionCollectionName);
        instance.setFormTemplateId(String.valueOf(formTemplateId));
        instance.setCurrentStepSequence(0);
        instance.setActionLog(new ArrayList<>());

        if (hasTemplate) {
            ApprovalTemplate template = findApprovalTemplateById(approvalTemplateId);
            instance.setApprovalTemplateId(template.getId());
            instance.setApprovalSteps(copyTemplateSteps(template.getApprovalSteps()));
        } else {
            instance.setApprovalTemplateId(null);
            instance.setApprovalSteps(new ArrayList<>());
        }

        populateFilterSnapshot(instance, formSubmissionStateUpdater.getLatestFormSubmission(formSubmissionId, formSubmissionCollectionName));
        instance.setCreatedAt(Instant.now());
        instance.setCreatedBy(createdBy);
        instance.setUpdatedAt(Instant.now());
        instance.setUpdatedBy(createdBy);
        return saveInstance(instance);
    }

    @Override
    public void validateApprovalTemplateExists(String approvalTemplateId) {
        copyTemplateSteps(findApprovalTemplateById(approvalTemplateId).getApprovalSteps());
    }

    private ApprovalTemplate findApprovalTemplateById(String approvalTemplateId) {
        ApprovalTemplate template = approvalTemplateLookupService.findById(approvalTemplateId);
        if (template == null) {
            throw new ApprovalInstanceException("Approval template not found: " + approvalTemplateId);
        }
        return template;
    }

    @Override
    public void onFormSubmissionEdited(String oldFormSubmissionId, String newFormSubmissionId, String formSubmissionCollectionName, Long updatedBy) {
        ApprovalInstance instance = getByFormSubmission(oldFormSubmissionId, formSubmissionCollectionName);
        instance.setFormSubmissionId(newFormSubmissionId);
        populateFilterSnapshot(instance, formSubmissionStateUpdater.getLatestFormSubmission(newFormSubmissionId, formSubmissionCollectionName));
        touch(instance, updatedBy);
        saveInstance(instance);
    }

    @Override
    public void enterReview(FormSubmissionActionRequest request) {
        ActionableInstance actionable = resolveActionableInstance(request.getSubmissionId(), request.getCollectionName());
        ApprovalInstance instance = actionable.instance();
        Document formSubmission = actionable.formSubmission();
        Long updatedBy = request.getActorUserId();
        if (hasNoApprovalSteps(instance)) {
            appendBuiltActionLog(instance, request, ApprovalAction.SUBMITTED_FOR_APPROVAL, null, formSubmission);
            saveWithAudit(instance, updatedBy);
            updateSubmissionStateAndRefresh(instance, FormSubmissionState.ARCHIVED, updatedBy);
            return;
        }
        ApprovalInstanceStep current = currentStep(instance);
        ApprovalStepState previousStepState = current.getStepState();
        appendBuiltActionLog(instance, request, ApprovalAction.SUBMITTED_FOR_APPROVAL, null, formSubmission);
        activateCurrentStep(instance);
        touch(instance, updatedBy);
        saveInstance(instance);
        try {
            updateSubmissionStateAndRefresh(instance, FormSubmissionState.UNDER_REVIEW, updatedBy);
        } catch (Exception e) {
            rollbackSingleStepMutation(instance, current, previousStepState);
            throw rollbackFailure(e);
        }
    }

    @Override
    public void exitReview(FormSubmissionActionRequest request) {
        ActionableInstance actionable = resolveActionableInstance(request.getSubmissionId(), request.getCollectionName());
        ApprovalInstance instance = actionable.instance();
        Document formSubmission = actionable.formSubmission();
        Long updatedBy = request.getActorUserId();
        int currentSequence = safeCurrent(instance);
        List<StepStateSnapshot> stepSnapshots = snapshotAllSteps(instance.getApprovalSteps());
        appendBuiltActionLog(instance, request, ApprovalAction.RECALLED, null, formSubmission);
        resetAllSteps(instance);
        saveWithAudit(instance, updatedBy);
        try {
            updateSubmissionStateAndRefresh(instance, FormSubmissionState.SUBMITTED, updatedBy);
        } catch (Exception e) {
            instance.setCurrentStepSequence(currentSequence);
            restoreStepStates(instance.getApprovalSteps(), stepSnapshots);
            removeLastActionLog(instance);
            saveInstance(instance);
            throw rollbackFailure(e);
        }
    }

    @Override
    public ApprovalInstance editApprovalFlow(ApprovalFlowEditRequest request) {
        ApprovalInstance instance = getByFormSubmission(request.getSubmissionId(), request.getCollectionName());

        List<ApprovalInstanceStep> oldSteps = instance.getApprovalSteps();
        List<ApprovalInstanceStep> newSteps = copyRequestSteps(request.getSteps());
        int current = safeCurrent(instance);
        Document formSubmission = latestFormSubmission(request.getSubmissionId(), request.getCollectionName());
        FormSubmissionState formState = resolveLifecycleState(formSubmission);
        appendBuiltActionLog(instance, request, ApprovalAction.FLOW_EDITED, null, formSubmission);

        if (newSteps.isEmpty()) {
            instance.setApprovalSteps(newSteps);
        } else if (requiresFullReset(oldSteps, newSteps, current)) {
            initializeSteps(newSteps);
            instance.setApprovalSteps(newSteps);
            instance.setCurrentStepSequence(0);
            if (formState == FormSubmissionState.UNDER_REVIEW) {
                activateCurrentStep(instance);
            } else if (formState == FormSubmissionState.PENDING_REVISION) {
                markAwaitingRevision(instance.getApprovalSteps().get(0));
            }
        } else {
            instance.setApprovalSteps(mergeUnchangedStepProgress(oldSteps, newSteps));
        }
        saveWithAudit(instance, request.getUserId());
        if (newSteps.isEmpty()) {
            try {
                updateSubmissionStateAndRefresh(instance, FormSubmissionState.SUBMITTED, request.getUserId());
            } catch (Exception e) {
                instance.setApprovalSteps(oldSteps);
                removeLastActionLog(instance);
                saveInstance(instance);
                throw rollbackFailure(e);
            }
        }
        return instance;
    }

    @Override
    public void approve(FormSubmissionActionRequest request) {
        ActionableInstance actionable = resolveActionableInstance(request.getSubmissionId(), request.getCollectionName());
        ApprovalInstance instance = actionable.instance();
        Document formSubmission = actionable.formSubmission();
        Long updatedBy = request.getActorUserId();
        FormSubmissionState previousFormState = resolveLifecycleState(formSubmission);
        boolean movedToUnderReview = previousFormState != FormSubmissionState.UNDER_REVIEW;
        if (movedToUnderReview) {
            formSubmission = transitionSubmissionToUnderReview(instance, updatedBy);
        }
        ApprovalInstanceStep current = currentStep(instance);
        int previousSequence = safeCurrent(instance);
        List<StepStateSnapshot> stepSnapshots = snapshotAllSteps(instance.getApprovalSteps());
        ApprovalActor actor = actorFromRequest(request);
        ApprovalActionLog logEntry = appendBuiltActionLog(instance, request, actor, ApprovalAction.APPROVED, previousSequence, formSubmission);
        applyStepDecision(current, ApprovalStepState.APPROVED, logEntry);
        boolean completed = previousSequence == instance.getApprovalSteps().size() - 1;
        if (!completed) {
            instance.setCurrentStepSequence(previousSequence + 1);
            activateCurrentStep(instance);
        }
        saveWithAudit(instance, updatedBy);
        if (completed) {
            try {
                updateSubmissionStateAndRefresh(instance, FormSubmissionState.ARCHIVED, updatedBy);
            } catch (Exception e) {
                rollbackApprovalDecision(instance, stepSnapshots, previousSequence, previousFormState, movedToUnderReview, updatedBy);
                throw rollbackFailure(e);
            }
            return;
        }
        try {
            if (movedToUnderReview) {
                refreshSnapshot(instance, latestFormSubmission(instance));
                saveWithAudit(instance, updatedBy);
            }
        } catch (Exception e) {
            rollbackApprovalDecision(instance, stepSnapshots, previousSequence, previousFormState, movedToUnderReview, updatedBy);
            throw rollbackFailure(e);
        }
    }

    @Override
    public void forward(FormSubmissionActionRequest request) {
        ActionableInstance actionable = resolveActionableInstance(request.getSubmissionId(), request.getCollectionName());
        ApprovalInstance instance = actionable.instance();
        Document formSubmission = actionable.formSubmission();
        Long updatedBy = request.getActorUserId();
        FormSubmissionState previousFormState = resolveLifecycleState(formSubmission);
        boolean movedToUnderReview = previousFormState != FormSubmissionState.UNDER_REVIEW;
        if (movedToUnderReview) {
            formSubmission = transitionSubmissionToUnderReview(instance, updatedBy);
        }
        int currentSequence = safeCurrent(instance);
        List<StepStateSnapshot> stepSnapshots = snapshotAllSteps(instance.getApprovalSteps());
        guard(currentSequence < instance.getApprovalSteps().size() - 1, "Cannot forward because there is no next approval step.");
        ApprovalActor actor = actorFromRequest(request);
        ApprovalInstanceStep current = currentStep(instance);
        ApprovalActionLog logEntry = appendBuiltActionLog(instance, request, actor, ApprovalAction.FORWARDED, currentSequence, formSubmission);
        applyStepDecision(current, ApprovalStepState.FORWARDED, logEntry);
        instance.setCurrentStepSequence(currentSequence + 1);
        activateCurrentStep(instance);
        saveWithAudit(instance, updatedBy);
        try {
            if (movedToUnderReview) {
                formSubmission = latestFormSubmission(instance);
            }
            refreshSnapshot(instance, formSubmission);
            saveWithAudit(instance, updatedBy);
        } catch (Exception e) {
            rollbackApprovalDecision(instance, stepSnapshots, currentSequence, previousFormState, movedToUnderReview, updatedBy);
            throw rollbackFailure(e);
        }
    }

    @Override
    public void requestCorrection(FormSubmissionActionRequest request) {
        ActionableInstance actionable = resolveCorrectionActionableInstance(request);
        ApprovalInstance instance = actionable.instance();
        Document formSubmission = actionable.formSubmission();
        Long updatedBy = request.getActorUserId();
        int currentSequence = safeCurrent(instance);
        List<StepStateSnapshot> stepSnapshots = snapshotAllSteps(instance.getApprovalSteps());
        appendBuiltActionLog(instance, request, ApprovalAction.REQUESTED_CORRECTION, currentSequence, formSubmission);
        if (hasNoApprovalSteps(instance)) {
            saveWithAudit(instance, updatedBy);
        } else {
            ApprovalInstanceStep current = currentStep(instance);
            if (Boolean.TRUE.equals(request.getResetApprovalSteps())) {
                resetAllSteps(instance);
                markAwaitingRevision(currentStep(instance));
            } else {
                markAwaitingRevision(current);
            }
            saveWithAudit(instance, updatedBy);
        }
        try {
            updateSubmissionStateAndRefresh(instance, FormSubmissionState.PENDING_REVISION, updatedBy);
        } catch (Exception e) {
            instance.setCurrentStepSequence(currentSequence);
            restoreStepStates(instance.getApprovalSteps(), stepSnapshots);
            removeLastActionLog(instance);
            saveInstance(instance);
            throw rollbackFailure(e);
        }
    }

    @Override
    public LegacyMigrationResult backfillMissingApprovalInstances() {
        int totalProcessed = 0;
        int totalMigrated = 0;
        int totalSkipped = 0;
        int totalFailed = 0;
        List<LegacyMigrationResult.FailedRecord> failures = new ArrayList<>();

        List<String> formCollections = mongoTemplate.getCollectionNames().stream()
                .filter(name -> name.startsWith(FORM_COLLECTION_PREFIX))
                .sorted()
                .toList();

        log.info("Approval-instance backfill starting: {} form collections found", formCollections.size());

        for (String collectionName : formCollections) {
            ObjectId lastSeenId = null;
            while (true) {
                List<Document> batch = fetchBackfillBatch(collectionName, lastSeenId);
                if (batch.isEmpty()) {
                    break;
                }

                for (Document submission : batch) {
                    totalProcessed++;
                    String submissionId = submission.getObjectId("_id").toString();
                    try {
                        if (pairBackfillSubmissionIfMissing(submission, collectionName)) {
                            totalMigrated++;
                        } else {
                            totalSkipped++;
                        }
                    } catch (Exception e) {
                        totalFailed++;
                        failures.add(new LegacyMigrationResult.FailedRecord(collectionName, submissionId, e.getMessage()));
                        log.error("Approval-instance backfill failed for submission {} in {}", submissionId, collectionName, e);
                    }
                    lastSeenId = submission.getObjectId("_id");
                }

                if (batch.size() < BACKFILL_BATCH_SIZE) {
                    break;
                }
            }
        }

        log.info("Approval-instance backfill complete - processed={} migrated={} skipped={} failed={}",
                totalProcessed, totalMigrated, totalSkipped, totalFailed);
        return new LegacyMigrationResult(totalProcessed, totalMigrated, totalSkipped, totalFailed, failures);
    }

    @Override
    public ApprovalInstance getByFormSubmission(String formSubmissionId, String formSubmissionCollectionName) {
        return approvalInstanceRepository.findByFormSubmissionIdAndFormSubmissionCollectionName(formSubmissionId, formSubmissionCollectionName)
                .orElseThrow(() -> new ApprovalInstanceException("Active approval instance not found for form submission: " + formSubmissionId));
    }

    @Override
    public ApprovalInstance getById(String approvalInstanceId) {
        return approvalInstanceRepository.findById(approvalInstanceId)
                .orElseThrow(() -> new ApprovalInstanceException("Approval instance not found: " + approvalInstanceId));
    }

    @Override
    public List<ApprovalInstanceStep> getApprovalSteps(String formSubmissionId, String formSubmissionCollectionName) {
        return getByFormSubmission(formSubmissionId, formSubmissionCollectionName).getApprovalSteps();
    }

    @Override
    public void refreshFilterSnapshot(String formSubmissionId, String formSubmissionCollectionName, Long updatedBy) {
        ApprovalInstance instance = getByFormSubmission(formSubmissionId, formSubmissionCollectionName);
        refreshSnapshot(instance, latestFormSubmission(formSubmissionId, formSubmissionCollectionName));
        saveWithAudit(instance, updatedBy);
    }

    private record ActionableInstance(ApprovalInstance instance, Document formSubmission) {}

    private ActionableInstance resolveActionableInstance(String submissionId, String collectionName) {
        ApprovalInstance instance = getByFormSubmission(submissionId, collectionName);
        return new ActionableInstance(instance, latestFormSubmission(instance));
    }

    private boolean pairBackfillSubmissionIfMissing(Document submission, String collectionName) {
        String submissionId = submission.getObjectId("_id").toString();
        if (approvalInstanceRepository.findByFormSubmissionIdAndFormSubmissionCollectionName(submissionId, collectionName).isPresent()) {
            return false;
        }

        Long formTemplateId = resolveFormTemplateId(submission, collectionName);
        String approvalTemplateId = resolveApprovalTemplateId(formTemplateId);
        Long createdBy = asLong(submission.get("created_by"));
        create(submissionId, collectionName, formTemplateId, approvalTemplateId, createdBy);
        return true;
    }

    private ActionableInstance resolveCorrectionActionableInstance(FormSubmissionActionRequest request) {
        Document formSubmission = latestFormSubmission(request.getSubmissionId(), request.getCollectionName());
        ApprovalInstance instance = approvalInstanceRepository
                .findByFormSubmissionIdAndFormSubmissionCollectionName(request.getSubmissionId(), request.getCollectionName())
                .orElseGet(() -> createMissingApprovalInstanceForCorrection(request, formSubmission));
        return new ActionableInstance(instance, formSubmission);
    }

    private ApprovalInstance createMissingApprovalInstanceForCorrection(FormSubmissionActionRequest request, Document formSubmission) {
        Long formTemplateId = resolveFormTemplateId(formSubmission, request.getCollectionName());
        String approvalTemplateId = resolveApprovalTemplateId(formTemplateId);
        return create(
                request.getSubmissionId(),
                request.getCollectionName(),
                formTemplateId,
                approvalTemplateId,
                request.getActorUserId()
        );
    }

    private Long resolveFormTemplateId(Document formSubmission, String collectionName) {
        Long formTemplateId = asLong(formSubmission.get("form_template_id"));
        if (formTemplateId != null) {
            return formTemplateId;
        }
        String[] parts = collectionName.split("_");
        if (parts.length >= 3) {
            try {
                return Long.parseLong(parts[2]);
            } catch (NumberFormatException ignored) {
                // Fall through to the workflow-specific error below.
            }
        }
        throw new ApprovalInstanceException("Unable to resolve form template id for form submission: " + asString(formSubmission.get("_id")));
    }

    private String resolveApprovalTemplateId(Long formTemplateId) {
        Document formTemplateSnapshot = formSubmissionStateUpdater.getFormTemplateSnapshot(String.valueOf(formTemplateId));
        return formTemplateSnapshot == null ? null : asString(formTemplateSnapshot.get("approval_template_id"));
    }

    private List<Document> fetchBackfillBatch(String collectionName, ObjectId lastSeenId) {
        Query query = new Query();
        if (lastSeenId != null) {
            query.addCriteria(Criteria.where("_id").gt(lastSeenId));
        }
        query.with(Sort.by(Sort.Direction.ASC, "_id"));
        query.limit(BACKFILL_BATCH_SIZE);
        return mongoTemplate.find(query, Document.class, collectionName);
    }

    private boolean hasNoApprovalSteps(ApprovalInstance instance) {
        return instance.getApprovalSteps() == null || instance.getApprovalSteps().isEmpty();
    }

    private ApprovalInstanceStep currentStep(ApprovalInstance instance) {
        int current = safeCurrent(instance);
        guard(current >= 0 && current < instance.getApprovalSteps().size(), "Current approval step is out of range.");
        return instance.getApprovalSteps().get(current);
    }

    private ApprovalInstance saveInstance(ApprovalInstance instance) {
        syncApprovalProcessStatus(instance);
        return approvalInstanceRepository.save(instance);
    }

    private void syncApprovalProcessStatus(ApprovalInstance instance) {
        instance.setApprovalProcessStatus(ApprovalProcessStatusResolver.deriveDbValue(instance.getApprovalSteps()));
    }

    private Document transitionSubmissionToUnderReview(ApprovalInstance instance, Long updatedBy) {
        formSubmissionStateUpdater.updateFormSubmissionState(
                instance.getFormSubmissionId(),
                instance.getFormSubmissionCollectionName(),
                FormSubmissionState.UNDER_REVIEW,
                updatedBy
        );
        return latestFormSubmission(instance);
    }

    private void rollbackApprovalDecision(
            ApprovalInstance instance,
            List<StepStateSnapshot> stepSnapshots,
            int previousSequence,
            FormSubmissionState previousFormState,
            boolean movedToUnderReview,
            Long updatedBy
    ) {
        instance.setCurrentStepSequence(previousSequence);
        restoreStepStates(instance.getApprovalSteps(), stepSnapshots);
        removeLastActionLog(instance);
        if (movedToUnderReview) {
            formSubmissionStateUpdater.updateFormSubmissionState(
                    instance.getFormSubmissionId(),
                    instance.getFormSubmissionCollectionName(),
                    previousFormState,
                    updatedBy
            );
            refreshSnapshot(instance, latestFormSubmission(instance));
        }
        saveInstance(instance);
    }

    /**
     * Builds an immutable audit-log entry and captures snapshots only for step decisions.
     */
    private ApprovalActionLog buildActionLog(
            ApprovalInstance instance,
            FormSubmissionActionRequest request,
            ApprovalAction action,
            Integer stepSequence,
            boolean includeSnapshots,
            Document formSubmission
    ) {
        return buildActionLog(instance, request, actorFromRequest(request), action, stepSequence, includeSnapshots, formSubmission);
    }

    private ApprovalActionLog buildActionLog(
            ApprovalInstance instance,
            FormSubmissionActionRequest request,
            ApprovalActor actor,
            ApprovalAction action,
            Integer stepSequence,
            boolean includeSnapshots,
            Document formSubmission
    ) {
        SnapshotPair snapshots = includeSnapshots
                ? captureSnapshots(instance, formSubmission)
                : new SnapshotPair(null, null);
        ApprovalActionLog logEntry = new ApprovalActionLog();
        logEntry.setLogId(UUID.randomUUID().toString());
        logEntry.setAction(action);
        logEntry.setStepSequence(stepSequence);
        logEntry.setComments(request.getComment());
        logEntry.setESignature(request.getESignature());
        logEntry.setSuggestRetest(request.getSuggestRetest());
        logEntry.setActorUserId(actor.actorUserId());
        logEntry.setActorUserName(actor.actorUserName() != null ? actor.actorUserName() : actorUserName(actor.actorUserId()));
        logEntry.setActorRoleId(actorRoleIdForLog(instance, actor, stepSequence));
        logEntry.setActorRoleName(actorRoleNameForLog(instance, actor, stepSequence));
        logEntry.setFormSubmissionSnapshot(snapshots.formSubmissionSnapshot());
        logEntry.setFormTemplateSnapshot(snapshots.formTemplateSnapshot());
        logEntry.setActedAt(Instant.now());
        return logEntry;
    }

    private ApprovalActionLog buildActionLog(
            ApprovalInstance instance,
            ApprovalFlowEditRequest request,
            ApprovalAction action,
            Integer stepSequence,
            boolean includeSnapshots,
            Document formSubmission
    ) {
        FormSubmissionActionRequest actionRequest = new FormSubmissionActionRequest();
        actionRequest.setSubmissionId(request.getSubmissionId());
        actionRequest.setCollectionName(request.getCollectionName());
        actionRequest.setActorUserId(request.getUserId());
        actionRequest.setComment(request.getComment());
        return buildActionLog(instance, actionRequest, action, stepSequence, includeSnapshots, formSubmission);
    }

    /**
     * Appends to the audit trail; callers must never reorder or remove existing entries.
     */
    private void appendActionLog(ApprovalInstance instance, ApprovalActionLog logEntry) {
        if (instance.getActionLog() == null) {
            instance.setActionLog(new ArrayList<>());
        }
        instance.getActionLog().add(logEntry);
    }

    private void appendBuiltActionLog(
            ApprovalInstance instance,
            FormSubmissionActionRequest request,
            ApprovalAction action,
            Integer stepSequence,
            Document formSubmission
    ) {
        appendActionLog(instance, buildActionLog(instance, request, action, stepSequence, true, formSubmission));
    }

    private ApprovalActionLog appendBuiltActionLog(
            ApprovalInstance instance,
            FormSubmissionActionRequest request,
            ApprovalActor actor,
            ApprovalAction action,
            Integer stepSequence,
            Document formSubmission
    ) {
        ApprovalActionLog logEntry = buildActionLog(instance, request, actor, action, stepSequence, true, formSubmission);
        appendActionLog(instance, logEntry);
        return logEntry;
    }

    private void appendBuiltActionLog(
            ApprovalInstance instance,
            ApprovalFlowEditRequest request,
            ApprovalAction action,
            Integer stepSequence,
            Document formSubmission
    ) {
        appendActionLog(instance, buildActionLog(instance, request, action, stepSequence, true, formSubmission));
    }

    private void removeLastActionLog(ApprovalInstance instance) {
        if (instance.getActionLog() != null && !instance.getActionLog().isEmpty()) {
            instance.getActionLog().remove(instance.getActionLog().size() - 1);
        }
    }

    /**
     * Captures latest form-submission and template documents at the exact approval decision time.
     */
    private SnapshotPair captureSnapshots(ApprovalInstance instance, Document formSubmission) {
        Object formTemplate = formSubmissionStateUpdater.getFormTemplateSnapshot(instance.getFormTemplateId());
        return new SnapshotPair(formSubmission, formTemplate);
    }

    private List<ApprovalInstanceStep> copyTemplateSteps(List<ApprovalStep> approvalSteps) {
        if (approvalSteps == null) {
            return new ArrayList<>();
        }
        return approvalSteps.stream()
                .sorted(Comparator.comparing(step -> nullToZero(step.getSequence())))
                .map(this::copyTemplateStep)
                .toList();
    }

    private ApprovalInstanceStep copyTemplateStep(ApprovalStep step) {
        String userId = step.getUserId() == null ? null : String.valueOf(step.getUserId());
        String roleId = step.getRoleId() == null ? null : String.valueOf(step.getRoleId());
        guard((userId == null) != (roleId == null), "Approval template step must have exactly one required user or role.");

        ApprovalInstanceStep instanceStep = new ApprovalInstanceStep();
        instanceStep.setSequence(nullToZero(step.getSequence()));
        instanceStep.setRequiredUserId(userId);
        instanceStep.setRequiredRoleId(roleId);
        instanceStep.setRequiredType(requiredType(userId, roleId));
        instanceStep.setStepName(requiredApproverName(userId, roleId));
        instanceStep.setLastActionRecord(null);
        instanceStep.setStepState(ApprovalStepState.PENDING);
        instanceStep.setResetCounter(0);
        return instanceStep;
    }

    private List<ApprovalInstanceStep> copyRequestSteps(List<ApprovalStepRequest> rawSteps) {
        if (rawSteps == null) {
            return new ArrayList<>();
        }
        List<ApprovalInstanceStep> steps = new ArrayList<>();
        for (ApprovalStepRequest raw : rawSteps) {
            ApprovalInstanceStep step = new ApprovalInstanceStep();
            step.setSequence(raw.sequence());
            step.setRequiredUserId(raw.requiredUserId());
            step.setRequiredRoleId(raw.requiredRoleId());
            guard((step.getRequiredUserId() == null) != (step.getRequiredRoleId() == null), "Approval instance step must have exactly one required user or role.");
            step.setRequiredType(requiredType(step.getRequiredUserId(), step.getRequiredRoleId()));
            step.setStepName(requiredApproverName(step.getRequiredUserId(), step.getRequiredRoleId()));
            step.setLastActionRecord(null);
            step.setStepState(ApprovalStepState.PENDING);
            step.setResetCounter(0);
            steps.add(step);
        }
        steps.sort(Comparator.comparing(step -> nullToZero(step.getSequence())));
        return steps;
    }

    /**
     * Detects edits that invalidate the currently executing approval path.
     */
    private boolean requiresFullReset(List<ApprovalInstanceStep> oldSteps, List<ApprovalInstanceStep> newSteps, int current) {
        if (oldSteps == null || oldSteps.isEmpty()) {
            return false;
        }
        if (current >= newSteps.size()) {
            return true;
        }
        for (int i = 0; i <= current && i < oldSteps.size(); i++) {
            if (!sameStepIdentity(oldSteps.get(i), newSteps.get(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Carries forward step UI state only for unchanged steps in append-only flow edits.
     */
    private List<ApprovalInstanceStep> mergeUnchangedStepProgress(List<ApprovalInstanceStep> oldSteps, List<ApprovalInstanceStep> newSteps) {
        if (oldSteps == null) {
            return newSteps;
        }
        for (int i = 0; i < newSteps.size() && i < oldSteps.size(); i++) {
            if (sameStepIdentity(oldSteps.get(i), newSteps.get(i))) {
                newSteps.get(i).setLastActionRecord(oldSteps.get(i).getLastActionRecord());
                newSteps.get(i).setStepState(oldSteps.get(i).getStepState());
                newSteps.get(i).setResetCounter(nullToZero(oldSteps.get(i).getResetCounter()));
            }
        }
        return newSteps;
    }

    private boolean sameStepIdentity(ApprovalInstanceStep left, ApprovalInstanceStep right) {
        return Objects.equals(left.getRequiredUserId(), right.getRequiredUserId())
                && Objects.equals(left.getRequiredRoleId(), right.getRequiredRoleId())
                && Objects.equals(left.getRequiredType(), right.getRequiredType());
    }

    private void initializeSteps(List<ApprovalInstanceStep> steps) {
        for (ApprovalInstanceStep step : steps) {
            step.setLastActionRecord(null);
            step.setStepState(ApprovalStepState.PENDING);
            if (step.getResetCounter() == null) {
                step.setResetCounter(0);
            }
        }
    }

    private void activateCurrentStep(ApprovalInstance instance) {
        currentStep(instance).setStepState(ApprovalStepState.IN_PROGRESS);
    }

    private void markAwaitingRevision(ApprovalInstanceStep step) {
        step.setLastActionRecord(null);
        step.setStepState(ApprovalStepState.AWAITING_REVISION);
    }

    private void applyStepDecision(ApprovalInstanceStep step, ApprovalStepState stepState, ApprovalActionLog logEntry) {
        step.setLastActionRecord(logEntry);
        step.setStepState(stepState);
    }

    private void resetStep(ApprovalInstanceStep step) {
        step.setLastActionRecord(null);
        step.setStepState(ApprovalStepState.PENDING);
        step.setResetCounter(nullToZero(step.getResetCounter()) + 1);
    }

    private void resetAllSteps(ApprovalInstance instance) {
        if (instance.getApprovalSteps() == null || instance.getApprovalSteps().isEmpty()) {
            instance.setCurrentStepSequence(0);
            return;
        }
        for (ApprovalInstanceStep step : instance.getApprovalSteps()) {
            resetStep(step);
        }
        instance.setCurrentStepSequence(0);
    }

    private String actorUserName(String actorUserId) {
        if (actorUserId == null) {
            return null;
        }
        try {
            return userRepository.findById(Long.parseLong(actorUserId)).map(User::getFullName).orElse(null);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Records the role ID that matched the step when one is available.
     */
    private String actorRoleIdForLog(ApprovalInstance instance, ApprovalActor actor, Integer stepSequence) {
        ApprovalInstanceStep step = stepForLog(instance, stepSequence);
        if (step != null && step.getRequiredRoleId() != null && Objects.equals(step.getRequiredRoleId(), actor.actorRoleId())) {
            return step.getRequiredRoleId();
        }
        return actor.actorRoleId();
    }

    /**
     * Records the role name that matched the step when one is available.
     */
    private String actorRoleNameForLog(ApprovalInstance instance, ApprovalActor actor, Integer stepSequence) {
        ApprovalInstanceStep step = stepForLog(instance, stepSequence);
        if (step != null && step.getRequiredRoleId() != null) {
            String requiredRoleName = roleName(step.getRequiredRoleId());
            if (requiredRoleName != null && (Objects.equals(requiredRoleName, actor.actorRoleName())
                    || Objects.equals(step.getRequiredRoleId(), actor.actorRoleId()))) {
                return requiredRoleName;
            }
        }
        return actor.actorRoleName();
    }

    private ApprovalActor actorFromRequest(FormSubmissionActionRequest request) {
        String actorUserId = request.getActorUserId() == null ? null : String.valueOf(request.getActorUserId());
        guard(actorUserId != null, "Approval actor requires actorUserId.");

        String actorRoleId = asString(request.getActorRoleId());
        String actorRoleName = roleName(actorRoleId);
        return new ApprovalActor(actorUserId, actorUserName(actorUserId), actorRoleId, actorRoleName);
    }

    private ApprovalInstanceStep stepForLog(ApprovalInstance instance, Integer stepSequence) {
        if (stepSequence == null || instance.getApprovalSteps() == null || stepSequence < 0 || stepSequence >= instance.getApprovalSteps().size()) {
            return null;
        }
        return instance.getApprovalSteps().get(stepSequence);
    }

    private String roleName(String roleId) {
        try {
            return roleRepository.findById(Integer.parseInt(roleId)).map(Role::getName).orElse(null);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String requiredApproverName(String userId, String roleId) {
        if (userId != null) {
            String userName = actorUserName(userId);
            return userName != null ? userName : "User " + userId;
        }
        if (roleId != null) {
            String roleName = roleName(roleId);
            return roleName != null ? roleName : "Role " + roleId;
        }
        return null;
    }

    private String requiredType(String userId, String roleId) {
        if (userId != null) {
            return "user";
        }
        if (roleId != null) {
            return "role";
        }
        return null;
    }

    private Integer formSubmissionVersion(Document formSubmission) {
        Object version = formSubmission.get("version");
        if (version instanceof Number number) {
            return number.intValue();
        }
        return 1;
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

    private void touch(ApprovalInstance instance, Long updatedBy) {
        instance.setUpdatedAt(Instant.now());
        instance.setUpdatedBy(updatedBy);
    }

    private int safeCurrent(ApprovalInstance instance) {
        return nullToZero(instance.getCurrentStepSequence());
    }

    private int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }

    private String asString(Object value) {
        return value == null || value.toString().isBlank() ? null : value.toString();
    }

    private void guard(boolean condition, String message) {
        if (!condition) {
            throw new ApprovalInstanceException(message);
        }
    }

    private void populateFilterSnapshot(ApprovalInstance instance, Document formSubmission) {
        FormSubmissionSnapshotForFilter snapshot = new FormSubmissionSnapshotForFilter();
        snapshot.setFormTemplateId(asLong(formSubmission.get("form_template_id")));
        if (snapshot.getFormTemplateId() == null) {
            snapshot.setFormTemplateId(asLong(instance.getFormTemplateId()));
        }
        snapshot.setFormSubmissionState(resolveLifecycleState(formSubmission).dbValue());
        snapshot.setFormSubmissionVersion(formSubmissionVersion(formSubmission));
        snapshot.setVersionGroupId(asString(formSubmission.get("version_group_id")));
        snapshot.setCreatedAt(dateValue(formSubmission.get("created_at")));
        snapshot.setCreatedBy(asLong(formSubmission.get("created_by")));
        snapshot.setRelatedInspectorIds(asLongList(formSubmission.get("related_inspector_ids")));
        snapshot.setRelatedInspectors(formSubmission.get("related_inspectors"));
        snapshot.setRelatedProductIds(asLongList(formSubmission.get("related_product_ids")));
        snapshot.setRelatedProducts(formSubmission.get("related_products"));
        snapshot.setRelatedBatchIds(asLongList(formSubmission.get("related_batch_ids")));
        snapshot.setRelatedBatches(formSubmission.get("related_batches"));
        snapshot.setRelatedTeamId(asLong(formSubmission.get("related_team_id")));
        snapshot.setRelatedTeams(formSubmission.get("related_teams"));
        snapshot.setRelatedShiftId(asLong(formSubmission.get("related_shift_id")));
        snapshot.setRelatedShifts(formSubmission.get("related_shifts"));
        Boolean isAlarmTriggered = isAlarmTriggered(formSubmission.get("exceeded_info"));
        snapshot.setIsAlarmTriggered(isAlarmTriggered);
        instance.setIsAlarmTriggered(isAlarmTriggered);
        instance.setFilterSnapshot(snapshot);
    }

    private Boolean isAlarmTriggered(Object exceededInfo) {
        if (!(exceededInfo instanceof java.util.Map<?, ?> map) || map.isEmpty()) {
            return Boolean.FALSE;
        }
        for (Object value : map.values()) {
            String result = exceededResult(value);
            if ("high".equalsIgnoreCase(result) || "low".equalsIgnoreCase(result) || "invalid".equalsIgnoreCase(result)) {
                return Boolean.TRUE;
            }
        }
        return Boolean.FALSE;
    }

    private String exceededResult(Object exceededFieldInfo) {
        if (exceededFieldInfo instanceof Document document) {
            return asString(document.get("result"));
        }
        if (exceededFieldInfo instanceof java.util.Map<?, ?> map) {
            return asString(map.get("result"));
        }
        return null;
    }

    private List<Long> asLongList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream()
                    .map(this::asLong)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
        Long single = asLong(value);
        return single == null ? null : List.of(single);
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

    private java.util.Date dateValue(Object value) {
        if (value instanceof java.util.Date date) {
            return date;
        }
        if (value instanceof Number number) {
            return new java.util.Date(number.longValue());
        }
        return null;
    }

    private record SnapshotPair(Object formSubmissionSnapshot, Object formTemplateSnapshot) {
    }

    private record ApprovalActor(String actorUserId, String actorUserName, String actorRoleId, String actorRoleName) {
    }

    private record StepStateSnapshot(int index, ApprovalStepState stepState, ApprovalActionLog lastActionRecord, Integer resetCounter) {
    }

    private List<StepStateSnapshot> snapshotAllSteps(List<ApprovalInstanceStep> steps) {
        if (steps == null) return List.of();
        List<StepStateSnapshot> snapshots = new ArrayList<>();
        for (int i = 0; i < steps.size(); i++) {
            snapshots.add(new StepStateSnapshot(i, steps.get(i).getStepState(), steps.get(i).getLastActionRecord(), steps.get(i).getResetCounter()));
        }
        return snapshots;
    }

    private void restoreStepStates(List<ApprovalInstanceStep> steps, List<StepStateSnapshot> snapshots) {
        for (StepStateSnapshot snapshot : snapshots) {
            if (snapshot.index() >= 0 && snapshot.index() < steps.size()) {
                steps.get(snapshot.index()).setStepState(snapshot.stepState());
                steps.get(snapshot.index()).setLastActionRecord(snapshot.lastActionRecord());
                steps.get(snapshot.index()).setResetCounter(snapshot.resetCounter());
            }
        }
    }

    private void saveWithAudit(ApprovalInstance instance, Long updatedBy) {
        touch(instance, updatedBy);
        saveInstance(instance);
    }

    private Document latestFormSubmission(ApprovalInstance instance) {
        return latestFormSubmission(instance.getFormSubmissionId(), instance.getFormSubmissionCollectionName());
    }

    private Document latestFormSubmission(String submissionId, String collectionName) {
        return formSubmissionStateUpdater.getLatestFormSubmission(submissionId, collectionName);
    }

    private void refreshSnapshot(ApprovalInstance instance, Document formSubmission) {
        populateFilterSnapshot(instance, formSubmission);
    }

    private void updateSubmissionStateAndRefresh(ApprovalInstance instance, FormSubmissionState state, Long updatedBy) {
        formSubmissionStateUpdater.updateFormSubmissionState(
                instance.getFormSubmissionId(),
                instance.getFormSubmissionCollectionName(),
                state,
                updatedBy
        );
        refreshSnapshot(instance, latestFormSubmission(instance));
        saveWithAudit(instance, updatedBy);
    }

    private void rollbackSingleStepMutation(ApprovalInstance instance, ApprovalInstanceStep step, ApprovalStepState previousStepState) {
        step.setStepState(previousStepState);
        removeLastActionLog(instance);
        saveInstance(instance);
    }

    private ApprovalInstanceException rollbackFailure(Exception e) {
        return new ApprovalInstanceException("Action failed, approval state rolled back: " + e.getMessage(), e);
    }
}
