package com.fps.svmes.services.impl;

import com.fps.shared.entity.primary.approval.ApprovalStep;
import com.fps.shared.entity.primary.approval.ApprovalTemplate;
import com.fps.shared.entity.primary.rbac.Role;
import com.fps.shared.entity.primary.user.User;
import com.fps.svmes.dto.requests.ApprovalStepRequest;
import com.fps.svmes.dto.requests.ApprovalFlowEditRequest;
import com.fps.svmes.dto.requests.FormSubmissionActionRequest;
import com.fps.svmes.enums.approval.ApprovalAction;
import com.fps.svmes.enums.approval.ApprovalStepState;
import com.fps.svmes.enums.form.FormSubmissionState;
import com.fps.svmes.exceptions.ApprovalInstanceException;
import com.fps.svmes.models.nosql.approval.ApprovalActionLog;
import com.fps.svmes.models.nosql.approval.ApprovalInstance;
import com.fps.svmes.models.nosql.approval.ApprovalInstanceFilterSnapshot;
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
import org.springframework.data.mongodb.core.MongoTemplate;
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
    private final MongoTemplate mongoTemplate;
    private final ApprovalInstanceRepository approvalInstanceRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ApprovalTemplateLookupService approvalTemplateLookupService;
    private final FormSubmissionStateUpdater formSubmissionStateUpdater;

    @Override
    public ApprovalInstance create(String formSubmissionId, String formSubmissionCollectionName, Long formTemplateId, String approvalTemplateId, Long createdBy) {
        ensureApprovalInstanceCollectionExists();
        ApprovalTemplate template = findApprovalTemplateById(approvalTemplateId);

        ApprovalInstance instance = new ApprovalInstance();
        instance.setFormSubmissionId(formSubmissionId);
        instance.setFormSubmissionCollectionName(formSubmissionCollectionName);
        instance.setApprovalTemplateId(template.getId());
        instance.setFormTemplateId(String.valueOf(formTemplateId));
        instance.setCurrentStepSequence(0);
        instance.setApprovalSteps(copyTemplateSteps(template.getApprovalSteps()));
        instance.setActionLog(new ArrayList<>());
        instance.setVersionNumber(1);
        populateFilterSnapshot(instance, formSubmissionStateUpdater.getLatestFormSubmission(formSubmissionId, formSubmissionCollectionName));
        instance.setCreatedAt(Instant.now());
        instance.setCreatedBy(createdBy);
        instance.setUpdatedAt(Instant.now());
        instance.setUpdatedBy(createdBy);
        instance.setStatus(1);
        return approvalInstanceRepository.save(instance);
    }

    private void ensureApprovalInstanceCollectionExists() {
        if (!mongoTemplate.collectionExists(ApprovalInstance.class)) {
            mongoTemplate.createCollection(ApprovalInstance.class);
        }
    }

    @Override
    public void validateApprovalTemplateExists(String approvalTemplateId) {
        ApprovalTemplate template = findApprovalTemplateById(approvalTemplateId);
        copyTemplateSteps(template.getApprovalSteps());
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
        ApprovalInstance instance = getActiveByFormSubmission(oldFormSubmissionId, formSubmissionCollectionName);
        instance.setFormSubmissionId(newFormSubmissionId);
        populateFilterSnapshot(instance, formSubmissionStateUpdater.getLatestFormSubmission(newFormSubmissionId, formSubmissionCollectionName));
        touch(instance, updatedBy);
        approvalInstanceRepository.save(instance);
    }

    @Override
    public void voidForFormSubmissionDelete(FormSubmissionActionRequest request) {
        ApprovalInstance instance = getActiveByFormSubmission(request.getSubmissionId(), request.getCollectionName());
        Document formSubmission = formSubmissionStateUpdater.getLatestFormSubmission(instance.getFormSubmissionId(), instance.getFormSubmissionCollectionName());
        FormSubmissionState formSubmissionState = resolveFormSubmissionState(formSubmission);
        guard(List.of(FormSubmissionState.DRAFT, FormSubmissionState.UNDER_REVIEW, FormSubmissionState.ARCHIVED).contains(formSubmissionState),
                "Only draft, under review, or archived form submissions can be voided.");
        markAllFutureStepsVoided(instance);
        instance.setStatus(0);
        touch(instance, userIdForAudit(request));
        approvalInstanceRepository.save(instance);
    }

    @Override
    public void submitForApproval(FormSubmissionActionRequest request) {
        ApprovalInstance instance = getActiveByFormSubmission(request.getSubmissionId(), request.getCollectionName());
        Document formSubmission = formSubmissionStateUpdater.getLatestFormSubmission(instance.getFormSubmissionId(), instance.getFormSubmissionCollectionName());
        validateFreshApprovalData(request, instance, formSubmission, false);
        requireFormSubmissionState(
                formSubmission,
                "Form submission must be draft or pending revision to submit for approval.",
                FormSubmissionState.DRAFT,
                FormSubmissionState.PENDING_REVISION
        );
        guard(instance.getApprovalSteps() != null && !instance.getApprovalSteps().isEmpty(), "Approval instance has no approval steps.");
        ApprovalInstanceStep current = currentStep(instance);
        int previousVersion = nullToOne(instance.getVersionNumber());
        ApprovalStepState previousStepState = current.getStepState();
        appendActionLog(instance, buildActionLog(instance, request, ApprovalAction.SUBMITTED_FOR_APPROVAL, null, true, formSubmission));
        activateCurrentStep(instance);
        incrementVersion(instance);
        touch(instance, userIdForAudit(request));
        approvalInstanceRepository.save(instance);
        try {
            formSubmissionStateUpdater.updateFormSubmissionState(
                    instance.getFormSubmissionId(),
                    instance.getFormSubmissionCollectionName(),
                    FormSubmissionState.UNDER_REVIEW,
                    userIdForAudit(request)
            );
            populateFilterSnapshot(instance, formSubmissionStateUpdater.getLatestFormSubmission(
                    instance.getFormSubmissionId(),
                    instance.getFormSubmissionCollectionName()
            ));
            touch(instance, userIdForAudit(request));
            approvalInstanceRepository.save(instance);
        } catch (Exception e) {
            current.setStepState(previousStepState);
            removeLastActionLog(instance);
            instance.setVersionNumber(previousVersion);
            approvalInstanceRepository.save(instance);
            throw new ApprovalInstanceException("Action failed, approval state rolled back: " + e.getMessage(), e);
        }
    }

    @Override
    public void recall(FormSubmissionActionRequest request) {
        ApprovalInstance instance = getActiveByFormSubmission(request.getSubmissionId(), request.getCollectionName());
        Document formSubmission = formSubmissionStateUpdater.getLatestFormSubmission(instance.getFormSubmissionId(), instance.getFormSubmissionCollectionName());
        validateFreshApprovalData(request, instance, formSubmission, false);
        requireFormSubmissionState(formSubmission, "Form submission must be under review for this approval action.", FormSubmissionState.UNDER_REVIEW);
        int previousVersion = nullToOne(instance.getVersionNumber());
        ApprovalInstanceStep current = currentStep(instance);
        ApprovalStepState previousStepState = current.getStepState();
        appendActionLog(instance, buildActionLog(instance, request, ApprovalAction.RECALLED, null, true, formSubmission));
        current.setStepState(ApprovalStepState.PENDING);
        incrementVersion(instance);
        touch(instance, userIdForAudit(request));
        approvalInstanceRepository.save(instance);
        try {
            formSubmissionStateUpdater.updateFormSubmissionState(
                    instance.getFormSubmissionId(),
                    instance.getFormSubmissionCollectionName(),
                    FormSubmissionState.DRAFT,
                    userIdForAudit(request)
            );
            populateFilterSnapshot(instance, formSubmissionStateUpdater.getLatestFormSubmission(
                    instance.getFormSubmissionId(),
                    instance.getFormSubmissionCollectionName()
            ));
            touch(instance, userIdForAudit(request));
            approvalInstanceRepository.save(instance);
        } catch (Exception e) {
            current.setStepState(previousStepState);
            removeLastActionLog(instance);
            instance.setVersionNumber(previousVersion);
            approvalInstanceRepository.save(instance);
            throw new ApprovalInstanceException("Action failed, approval state rolled back: " + e.getMessage(), e);
        }
    }

    @Override
    public ApprovalInstance editApprovalFlow(ApprovalFlowEditRequest request) {
        ApprovalInstance instance = getActiveByFormSubmission(request.getSubmissionId(), request.getCollectionName());
        int previousVersion = nullToOne(instance.getVersionNumber());

        List<ApprovalInstanceStep> oldSteps = instance.getApprovalSteps();
        List<ApprovalInstanceStep> newSteps = copyRequestSteps(request.getSteps());
        boolean structuralChange = hasStructuralChange(oldSteps, newSteps);
        int current = safeCurrent(instance);
        Document formSubmission = formSubmissionStateUpdater.getLatestFormSubmission(instance.getFormSubmissionId(), instance.getFormSubmissionCollectionName());
        FormSubmissionState formState = resolveFormSubmissionState(formSubmission);
        guard(List.of(FormSubmissionState.DRAFT, FormSubmissionState.PENDING_REVISION, FormSubmissionState.UNDER_REVIEW).contains(formState),
                "Approval flow can only be edited while the form is draft, pending revision, or under review.");
        appendActionLog(instance, buildActionLog(instance, request, ApprovalAction.FLOW_EDITED, null, true, formSubmission));

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
        if (structuralChange) {
            incrementVersion(instance);
        }
        touch(instance, request.getUserId());
        approvalInstanceRepository.save(instance);
        if (newSteps.isEmpty()) {
            try {
                formSubmissionStateUpdater.updateFormSubmissionState(
                        instance.getFormSubmissionId(),
                        instance.getFormSubmissionCollectionName(),
                        FormSubmissionState.ARCHIVED,
                        request.getUserId()
                );
                populateFilterSnapshot(instance, formSubmissionStateUpdater.getLatestFormSubmission(
                        instance.getFormSubmissionId(),
                        instance.getFormSubmissionCollectionName()
                ));
                touch(instance, request.getUserId());
                approvalInstanceRepository.save(instance);
            } catch (Exception e) {
                instance.setApprovalSteps(oldSteps);
                removeLastActionLog(instance);
                instance.setVersionNumber(previousVersion);
                approvalInstanceRepository.save(instance);
                throw new ApprovalInstanceException("Action failed, approval state rolled back: " + e.getMessage(), e);
            }
        }
        return instance;
    }

    @Override
    public void approve(FormSubmissionActionRequest request) {
        ApprovalInstance instance = requireActionableUnderReview(request);
        Document formSubmission = formSubmissionStateUpdater.getLatestFormSubmission(instance.getFormSubmissionId(), instance.getFormSubmissionCollectionName());
        validateFreshApprovalData(request, instance, formSubmission);
        ApprovalInstanceStep current = currentStep(instance);
        guard(current.getStepState() == ApprovalStepState.IN_PROGRESS, "Current approval step is not in progress.");
        ApprovalActor actor = actorFromRequest(request);
        guard(actorMatchesStep(current, actor), "Actor does not match the current approval step.");
        int previousSequence = safeCurrent(instance);
        int previousVersion = nullToOne(instance.getVersionNumber());
        ApprovalActionLog logEntry = buildActionLog(instance, request, actor, ApprovalAction.APPROVED, previousSequence, true, formSubmission);
        appendActionLog(instance, logEntry);
        current.setLastActionRecord(logEntry);
        current.setStepState(ApprovalStepState.APPROVED);
        boolean completed = previousSequence == instance.getApprovalSteps().size() - 1;
        if (!completed) {
            instance.setCurrentStepSequence(previousSequence + 1);
            activateCurrentStep(instance);
        }
        incrementVersion(instance);
        touch(instance, userIdForAudit(request));
        approvalInstanceRepository.save(instance);
        if (completed) {
            try {
                formSubmissionStateUpdater.updateFormSubmissionState(
                        instance.getFormSubmissionId(),
                        instance.getFormSubmissionCollectionName(),
                        FormSubmissionState.ARCHIVED,
                        userIdForAudit(request)
                );
                populateFilterSnapshot(instance, formSubmissionStateUpdater.getLatestFormSubmission(
                        instance.getFormSubmissionId(),
                        instance.getFormSubmissionCollectionName()
                ));
                touch(instance, userIdForAudit(request));
                approvalInstanceRepository.save(instance);
            } catch (Exception e) {
                current.setStepState(ApprovalStepState.IN_PROGRESS);
                current.setLastActionRecord(null);
                removeLastActionLog(instance);
                instance.setVersionNumber(previousVersion);
                approvalInstanceRepository.save(instance);
                throw new ApprovalInstanceException("Action failed, approval state rolled back: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public void forward(FormSubmissionActionRequest request) {
        ApprovalInstance instance = requireActionableUnderReview(request);
        Document formSubmission = formSubmissionStateUpdater.getLatestFormSubmission(instance.getFormSubmissionId(), instance.getFormSubmissionCollectionName());
        validateFreshApprovalData(request, instance, formSubmission);
        int currentSequence = safeCurrent(instance);
        guard(currentSequence < instance.getApprovalSteps().size() - 1, "Cannot forward because there is no next approval step.");
        ApprovalActor actor = actorFromRequest(request);
        guard(actorMatchesCurrentOrLater(instance, actor), "Actor must match current or later approval step to forward.");
        ApprovalInstanceStep current = currentStep(instance);
        guard(current.getStepState() == ApprovalStepState.IN_PROGRESS, "Current approval step is not in progress.");
        ApprovalActionLog logEntry = buildActionLog(instance, request, actor, ApprovalAction.FORWARDED, currentSequence, true, formSubmission);
        appendActionLog(instance, logEntry);
        current.setLastActionRecord(logEntry);
        current.setStepState(ApprovalStepState.FORWARDED);
        instance.setCurrentStepSequence(currentSequence + 1);
        activateCurrentStep(instance);
        incrementVersion(instance);
        touch(instance, userIdForAudit(request));
        approvalInstanceRepository.save(instance);
    }

    @Override
    public void rejectFullReset(FormSubmissionActionRequest request) {
        ApprovalInstance instance = requireActionableUnderReview(request);
        Document formSubmission = formSubmissionStateUpdater.getLatestFormSubmission(instance.getFormSubmissionId(), instance.getFormSubmissionCollectionName());
        validateFreshApprovalData(request, instance, formSubmission);
        ApprovalInstanceStep current = currentStep(instance);
        guard(current.getStepState() == ApprovalStepState.IN_PROGRESS, "Current approval step is not in progress.");
        ApprovalActor actor = actorFromRequest(request);
        guard(actorMatchesStep(current, actor), "Actor does not match the current approval step.");
        int previousVersion = nullToOne(instance.getVersionNumber());
        int previousSequence = safeCurrent(instance);
        List<StepStateSnapshot> stepSnapshots = snapshotAllSteps(instance.getApprovalSteps());
        ApprovalActionLog logEntry = buildActionLog(instance, request, ApprovalAction.REJECTED_FULL_RESET, previousSequence, true, formSubmission);
        appendActionLog(instance, logEntry);
        resetSteps(instance.getApprovalSteps());
        instance.setCurrentStepSequence(0);
        markAwaitingRevision(instance.getApprovalSteps().get(0));
        incrementVersion(instance);
        touch(instance, userIdForAudit(request));
        approvalInstanceRepository.save(instance);
        try {
            formSubmissionStateUpdater.updateFormSubmissionState(
                    instance.getFormSubmissionId(),
                    instance.getFormSubmissionCollectionName(),
                    FormSubmissionState.PENDING_REVISION,
                    userIdForAudit(request)
            );
            populateFilterSnapshot(instance, formSubmissionStateUpdater.getLatestFormSubmission(
                    instance.getFormSubmissionId(),
                    instance.getFormSubmissionCollectionName()
            ));
            touch(instance, userIdForAudit(request));
            approvalInstanceRepository.save(instance);
        } catch (Exception e) {
            instance.setCurrentStepSequence(previousSequence);
            restoreStepStates(instance.getApprovalSteps(), stepSnapshots);
            removeLastActionLog(instance);
            instance.setVersionNumber(previousVersion);
            approvalInstanceRepository.save(instance);
            throw new ApprovalInstanceException("Action failed, approval state rolled back: " + e.getMessage(), e);
        }
    }

    @Override
    public void rejectPartialReset(FormSubmissionActionRequest request) {
        ApprovalInstance instance = requireActionableUnderReview(request);
        Document formSubmission = formSubmissionStateUpdater.getLatestFormSubmission(instance.getFormSubmissionId(), instance.getFormSubmissionCollectionName());
        validateFreshApprovalData(request, instance, formSubmission);
        int currentSequence = safeCurrent(instance);
        guard(instance.getApprovalSteps().size() > 1 && currentSequence > 0, "Partial reset requires more than one step and a current step after the first.");
        ApprovalInstanceStep current = currentStep(instance);
        guard(current.getStepState() == ApprovalStepState.IN_PROGRESS, "Current approval step is not in progress.");
        ApprovalActor actor = actorFromRequest(request);
        guard(actorMatchesStep(current, actor), "Actor does not match the current approval step.");
        int previousVersion = nullToOne(instance.getVersionNumber());
        // Snapshot only the two affected steps for precise rollback
        List<StepStateSnapshot> stepSnapshots = List.of(
                snapshotStep(instance.getApprovalSteps(), currentSequence),
                snapshotStep(instance.getApprovalSteps(), currentSequence - 1)
        );
        ApprovalActionLog logEntry = buildActionLog(instance, request, ApprovalAction.REJECTED_PARTIAL_RESET, currentSequence, true, formSubmission);
        appendActionLog(instance, logEntry);
        resetStep(instance.getApprovalSteps().get(currentSequence));
        resetStep(instance.getApprovalSteps().get(currentSequence - 1));
        instance.setCurrentStepSequence(currentSequence - 1);
        markAwaitingRevision(instance.getApprovalSteps().get(currentSequence - 1));
        incrementVersion(instance);
        touch(instance, userIdForAudit(request));
        approvalInstanceRepository.save(instance);
        try {
            formSubmissionStateUpdater.updateFormSubmissionState(
                    instance.getFormSubmissionId(),
                    instance.getFormSubmissionCollectionName(),
                    FormSubmissionState.PENDING_REVISION,
                    userIdForAudit(request)
            );
            populateFilterSnapshot(instance, formSubmissionStateUpdater.getLatestFormSubmission(
                    instance.getFormSubmissionId(),
                    instance.getFormSubmissionCollectionName()
            ));
            touch(instance, userIdForAudit(request));
            approvalInstanceRepository.save(instance);
        } catch (Exception e) {
            instance.setCurrentStepSequence(currentSequence);
            restoreStepStates(instance.getApprovalSteps(), stepSnapshots);
            removeLastActionLog(instance);
            instance.setVersionNumber(previousVersion);
            approvalInstanceRepository.save(instance);
            throw new ApprovalInstanceException("Action failed, approval state rolled back: " + e.getMessage(), e);
        }
    }

    @Override
    public void rejectDiscard(FormSubmissionActionRequest request) {
        ApprovalInstance instance = requireActionableUnderReview(request);
        Document formSubmission = formSubmissionStateUpdater.getLatestFormSubmission(instance.getFormSubmissionId(), instance.getFormSubmissionCollectionName());
        validateFreshApprovalData(request, instance, formSubmission);
        ApprovalInstanceStep current = currentStep(instance);
        guard(current.getStepState() == ApprovalStepState.IN_PROGRESS, "Current approval step is not in progress.");
        ApprovalActor actor = actorFromRequest(request);
        guard(actorMatchesStep(current, actor), "Actor does not match the current approval step.");
        int previousVersion = nullToOne(instance.getVersionNumber());
        List<StepStateSnapshot> stepSnapshots = snapshotAllSteps(instance.getApprovalSteps());
        ApprovalActionLog logEntry = buildActionLog(instance, request, ApprovalAction.REJECTED_DISCARD, safeCurrent(instance), true, formSubmission);
        appendActionLog(instance, logEntry);
        current.setLastActionRecord(logEntry);
        markAllFutureStepsVoided(instance);
        instance.setStatus(0);
        incrementVersion(instance);
        touch(instance, userIdForAudit(request));
        approvalInstanceRepository.save(instance);
        try {
            formSubmissionStateUpdater.updateFormSubmissionState(
                    instance.getFormSubmissionId(),
                    instance.getFormSubmissionCollectionName(),
                    FormSubmissionState.VOID,
                    userIdForAudit(request)
            );
            populateFilterSnapshot(instance, formSubmissionStateUpdater.getLatestFormSubmission(
                    instance.getFormSubmissionId(),
                    instance.getFormSubmissionCollectionName()
            ));
            touch(instance, userIdForAudit(request));
            approvalInstanceRepository.save(instance);
        } catch (Exception e) {
            instance.setStatus(1);
            restoreStepStates(instance.getApprovalSteps(), stepSnapshots);
            removeLastActionLog(instance);
            instance.setVersionNumber(previousVersion);
            approvalInstanceRepository.save(instance);
            throw new ApprovalInstanceException("Action failed, approval state rolled back: " + e.getMessage(), e);
        }
    }

    @Override
    public ApprovalInstance getActiveByFormSubmission(String formSubmissionId, String formSubmissionCollectionName) {
        return approvalInstanceRepository.findByFormSubmissionIdAndFormSubmissionCollectionNameAndStatus(formSubmissionId, formSubmissionCollectionName, 1)
                .orElseThrow(() -> new ApprovalInstanceException("Active approval instance not found for form submission: " + formSubmissionId));
    }

    @Override
    public ApprovalInstance getByFormSubmissionIncludingVoid(String formSubmissionId, String formSubmissionCollectionName) {
        return approvalInstanceRepository.findByFormSubmissionIdAndFormSubmissionCollectionName(formSubmissionId, formSubmissionCollectionName)
                .orElseThrow(() -> new ApprovalInstanceException("Approval instance not found for form submission: " + formSubmissionId));
    }

    @Override
    public ApprovalInstance getByIdIncludingVoid(String approvalInstanceId) {
        return approvalInstanceRepository.findById(approvalInstanceId)
                .orElseThrow(() -> new ApprovalInstanceException("Approval instance not found: " + approvalInstanceId));
    }

    @Override
    public List<ApprovalInstanceStep> getApprovalSteps(String formSubmissionId, String formSubmissionCollectionName) {
        return getActiveByFormSubmission(formSubmissionId, formSubmissionCollectionName).getApprovalSteps();
    }

    @Override
    public void refreshFilterSnapshot(String formSubmissionId, String formSubmissionCollectionName, Long updatedBy) {
        ApprovalInstance instance = getByFormSubmissionIncludingVoid(formSubmissionId, formSubmissionCollectionName);
        populateFilterSnapshot(instance, formSubmissionStateUpdater.getLatestFormSubmission(formSubmissionId, formSubmissionCollectionName));
        touch(instance, updatedBy);
        approvalInstanceRepository.save(instance);
    }

    private ApprovalInstance requireActionableUnderReview(FormSubmissionActionRequest request) {
        ApprovalInstance instance = getActiveByFormSubmission(request.getSubmissionId(), request.getCollectionName());
        guard(instance.getApprovalSteps() != null && !instance.getApprovalSteps().isEmpty(), "Approval instance has no approval steps.");
        Document formSubmission = formSubmissionStateUpdater.getLatestFormSubmission(instance.getFormSubmissionId(), instance.getFormSubmissionCollectionName());
        requireFormSubmissionState(formSubmission, "Form submission must be under review for this approval action.", FormSubmissionState.UNDER_REVIEW);
        return instance;
    }

    private ApprovalInstanceStep currentStep(ApprovalInstance instance) {
        int current = safeCurrent(instance);
        guard(current >= 0 && current < instance.getApprovalSteps().size(), "Current approval step is out of range.");
        return instance.getApprovalSteps().get(current);
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
                ? captureSnapshots(instance.getFormSubmissionId(), instance.getFormSubmissionCollectionName(), instance.getFormTemplateId(), formSubmission)
                : new SnapshotPair(null, null);
        ApprovalActionLog logEntry = new ApprovalActionLog();
        logEntry.setLogId(UUID.randomUUID().toString());
        logEntry.setAction(action);
        logEntry.setStepSequence(stepSequence);
        logEntry.setComments(request.getComment());
        logEntry.setESignature(request.getESignature());
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
        ApprovalActionLog logEntry = buildActionLog(instance, actionRequest, action, stepSequence, includeSnapshots, formSubmission);
        return logEntry;
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

    private void removeLastActionLog(ApprovalInstance instance) {
        if (instance.getActionLog() != null && !instance.getActionLog().isEmpty()) {
            instance.getActionLog().remove(instance.getActionLog().size() - 1);
        }
    }

    /**
     * Captures latest form-submission and template documents at the exact approval decision time.
     */
    private SnapshotPair captureSnapshots(String formSubmissionId, String collectionName, String formTemplateId, Document formSubmission) {
        Object formTemplate = formSubmissionStateUpdater.getFormTemplateSnapshot(formTemplateId);
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

    private boolean hasStructuralChange(List<ApprovalInstanceStep> oldSteps, List<ApprovalInstanceStep> newSteps) {
        if (oldSteps == null || oldSteps.isEmpty()) {
            return newSteps != null && !newSteps.isEmpty();
        }
        if (newSteps == null || oldSteps.size() != newSteps.size()) {
            return true;
        }
        for (int i = 0; i < oldSteps.size(); i++) {
            if (!sameStepStructure(oldSteps.get(i), newSteps.get(i))) {
                return true;
            }
        }
        return false;
    }

    private boolean sameStepStructure(ApprovalInstanceStep left, ApprovalInstanceStep right) {
        return Objects.equals(left.getSequence(), right.getSequence())
                && Objects.equals(left.getRequiredUserId(), right.getRequiredUserId())
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

    private void resetSteps(List<ApprovalInstanceStep> steps) {
        for (ApprovalInstanceStep step : steps) {
            resetStep(step);
        }
    }

    private void resetStep(ApprovalInstanceStep step) {
        step.setLastActionRecord(null);
        step.setStepState(ApprovalStepState.PENDING);
        step.setResetCounter(nullToZero(step.getResetCounter()) + 1);
    }

    private void markAllFutureStepsVoided(ApprovalInstance instance) {
        if (instance.getApprovalSteps() == null) {
            return;
        }
        for (int i = safeCurrent(instance); i < instance.getApprovalSteps().size(); i++) {
            ApprovalInstanceStep step = instance.getApprovalSteps().get(i);
            if (step.getStepState() != ApprovalStepState.APPROVED && step.getStepState() != ApprovalStepState.FORWARDED) {
                step.setStepState(ApprovalStepState.VOIDED);
            }
        }
    }

    private boolean actorMatchesCurrentOrLater(ApprovalInstance instance, ApprovalActor actor) {
        for (int i = safeCurrent(instance); i < instance.getApprovalSteps().size(); i++) {
            if (actorMatchesStep(instance.getApprovalSteps().get(i), actor)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Validates the request actor against either the required user or role ID reference.
     */
    private boolean actorMatchesStep(ApprovalInstanceStep step, ApprovalActor actor) {
        if (step.getRequiredUserId() != null) {
            return Objects.equals(step.getRequiredUserId(), actor.actorUserId());
        }
        if (step.getRequiredRoleId() != null) {
            return Objects.equals(step.getRequiredRoleId(), actor.actorRoleId());
        }
        return false;
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

    private Long userIdForAudit(FormSubmissionActionRequest request) {
        return request.getActorUserId();
    }

    private void validateFreshApprovalData(FormSubmissionActionRequest request, ApprovalInstance instance, Document formSubmission) {
        validateFreshApprovalData(request, instance, formSubmission, true);
    }

    private void validateFreshApprovalData(FormSubmissionActionRequest request, ApprovalInstance instance, Document formSubmission,
                                           boolean requireApprovalInstanceVersion) {
        if (requireApprovalInstanceVersion) {
            guard(request.getExpectedApprovalInstanceVersion() != null, "expectedApprovalInstanceVersion is required.");
        }
        guard(request.getExpectedFormSubmissionVersion() != null, "expectedFormSubmissionVersion is required.");
        if (request.getExpectedApprovalInstanceVersion() != null) {
            guard(Objects.equals(request.getExpectedApprovalInstanceVersion(), nullToOne(instance.getVersionNumber())),
                    "Approval data is stale. Please refresh and try again.");
        }
        guard(Objects.equals(request.getExpectedFormSubmissionVersion(), formSubmissionVersion(formSubmission)),
                "Approval data is stale. Please refresh and try again.");
    }

    private Integer formSubmissionVersion(Document formSubmission) {
        Object version = formSubmission.get("version");
        if (version instanceof Number number) {
            return number.intValue();
        }
        return 1;
    }

    private void requireFormSubmissionState(Document formSubmission, String message, FormSubmissionState... allowedStates) {
        FormSubmissionState state = resolveFormSubmissionState(formSubmission);
        guard(List.of(allowedStates).contains(state), message);
    }

    private FormSubmissionState resolveFormSubmissionState(Document formSubmission) {
        String state = formSubmission.getString("state");
        if (state != null && !state.isBlank()) {
            return FormSubmissionState.fromValue(state);
        }
        return FormSubmissionState.ARCHIVED;
    }

    private void incrementVersion(ApprovalInstance instance) {
        instance.setVersionNumber(nullToOne(instance.getVersionNumber()) + 1);
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

    private int nullToOne(Integer value) {
        return value == null ? 1 : value;
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
        ApprovalInstanceFilterSnapshot snapshot = new ApprovalInstanceFilterSnapshot();
        snapshot.setFormTemplateId(asLong(formSubmission.get("form_template_id")));
        if (snapshot.getFormTemplateId() == null) {
            snapshot.setFormTemplateId(asLong(instance.getFormTemplateId()));
        }
        snapshot.setFormSubmissionState(resolveFormSubmissionState(formSubmission).dbValue());
        snapshot.setFormSubmissionVersion(formSubmissionVersion(formSubmission));
        snapshot.setVersionGroupId(asString(formSubmission.get("version_group_id")));
        snapshot.setCreatedAt(dateValue(formSubmission.get("created_at")));
        snapshot.setCreatedBy(asLong(formSubmission.get("created_by")));
        snapshot.setRelatedInspectorIds(asLongList(formSubmission.get("related_inspector_ids")));
        snapshot.setRelatedProductIds(asLongList(formSubmission.get("related_product_ids")));
        snapshot.setRelatedBatchIds(asLongList(formSubmission.get("related_batch_ids")));
        snapshot.setRelatedTeamId(asLong(formSubmission.get("related_team_id")));
        snapshot.setRelatedShiftId(asLong(formSubmission.get("related_shift_id")));
        instance.setFilterSnapshot(snapshot);
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

    private record StepStateSnapshot(int index, ApprovalStepState stepState, ApprovalActionLog lastActionRecord) {
    }

    private List<StepStateSnapshot> snapshotAllSteps(List<ApprovalInstanceStep> steps) {
        if (steps == null) return List.of();
        List<StepStateSnapshot> snapshots = new ArrayList<>();
        for (int i = 0; i < steps.size(); i++) {
            snapshots.add(new StepStateSnapshot(i, steps.get(i).getStepState(), steps.get(i).getLastActionRecord()));
        }
        return snapshots;
    }

    private StepStateSnapshot snapshotStep(List<ApprovalInstanceStep> steps, int index) {
        ApprovalInstanceStep step = steps.get(index);
        return new StepStateSnapshot(index, step.getStepState(), step.getLastActionRecord());
    }

    private void restoreStepStates(List<ApprovalInstanceStep> steps, List<StepStateSnapshot> snapshots) {
        for (StepStateSnapshot snapshot : snapshots) {
            if (snapshot.index() >= 0 && snapshot.index() < steps.size()) {
                steps.get(snapshot.index()).setStepState(snapshot.stepState());
                steps.get(snapshot.index()).setLastActionRecord(snapshot.lastActionRecord());
            }
        }
    }
}
