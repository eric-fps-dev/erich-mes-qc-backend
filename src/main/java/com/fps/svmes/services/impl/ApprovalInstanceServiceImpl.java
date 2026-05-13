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
import com.fps.svmes.models.nosql.approval.FormSubmissionSnapshotForFilter;
import com.fps.svmes.models.nosql.approval.ApprovalInstanceStep;
import com.fps.svmes.repositories.jpaRepo.user.RoleRepository;
import com.fps.svmes.repositories.jpaRepo.user.UserRepository;
import com.fps.svmes.repositories.mongoRepo.ApprovalInstanceRepository;
import com.fps.svmes.services.ApprovalInstanceService;
import com.fps.svmes.services.ApprovalTemplateLookupService;
import com.fps.svmes.services.FormSubmissionStateUpdater;
import com.fps.svmes.services.FormSubmissionMutationGuard;
import com.fps.svmes.services.SubmissionApprovalModelResolver;
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
    private final SubmissionApprovalModelResolver approvalModelResolver;
    private final FormSubmissionMutationGuard formSubmissionMutationGuard;

    @Override
    public ApprovalInstance create(String formSubmissionId, String formSubmissionCollectionName, Long formTemplateId, String approvalTemplateId, Long createdBy) {
        boolean hasTemplate = approvalTemplateId != null && !approvalTemplateId.isBlank();

        ApprovalInstance instance = new ApprovalInstance();
        instance.setFormSubmissionId(formSubmissionId);
        instance.setFormSubmissionCollectionName(formSubmissionCollectionName);
        instance.setFormTemplateId(String.valueOf(formTemplateId));
        instance.setCurrentStepSequence(0);
        instance.setActionLog(new ArrayList<>());
        instance.setVersionNumber(1);

        if (hasTemplate) {
            ApprovalTemplate template = findApprovalTemplateById(approvalTemplateId);
            instance.setApprovalTemplateId(template.getId());
            instance.setApprovalSteps(copyTemplateSteps(template.getApprovalSteps()));
            activateCurrentStep(instance);
        } else {
            instance.setApprovalTemplateId(null);
            instance.setApprovalSteps(new ArrayList<>());
        }

        populateFilterSnapshot(instance, formSubmissionStateUpdater.getLatestFormSubmission(formSubmissionId, formSubmissionCollectionName));
        instance.setCreatedAt(Instant.now());
        instance.setCreatedBy(createdBy);
        instance.setUpdatedAt(Instant.now());
        instance.setUpdatedBy(createdBy);
        return approvalInstanceRepository.save(instance);
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
        ApprovalInstance instance = getByFormSubmission(oldFormSubmissionId, formSubmissionCollectionName);
        instance.setFormSubmissionId(newFormSubmissionId);
        populateFilterSnapshot(instance, formSubmissionStateUpdater.getLatestFormSubmission(newFormSubmissionId, formSubmissionCollectionName));
        touch(instance, updatedBy);
        approvalInstanceRepository.save(instance);
    }

    @Override
    public void enterReview(FormSubmissionActionRequest request) {
        ApprovalInstance instance = getByFormSubmission(request.getSubmissionId(), request.getCollectionName());
        Document formSubmission = formSubmissionStateUpdater.getLatestFormSubmission(instance.getFormSubmissionId(), instance.getFormSubmissionCollectionName());
        validateFreshApprovalData(request, instance, formSubmission, false);
        FormSubmissionState formState = approvalModelResolver.resolveLifecycleState(formSubmission);
        requireFormSubmissionState(
                formSubmission,
                "Form submission must be submitted or pending revision to enter review.",
                FormSubmissionState.SUBMITTED,
                FormSubmissionState.PENDING_REVISION
        );
        if (omitApprovalActionLog(request)) {
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
            return;
        }
        guard(instance.getApprovalSteps() != null && !instance.getApprovalSteps().isEmpty(), "Approval instance has no approval steps.");
        ApprovalInstanceStep current = currentStep(instance);
        ApprovalStepState previousStepState = current.getStepState();
        appendActionLog(instance, buildActionLog(instance, request, ApprovalAction.SUBMITTED_FOR_APPROVAL, null, true, formSubmission));
        guard(canActivateForSubmission(formState, current.getStepState()),
                "Current approval step cannot be resumed from its present state.");
        activateCurrentStep(instance);
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
            approvalInstanceRepository.save(instance);
            throw new ApprovalInstanceException("Action failed, approval state rolled back: " + e.getMessage(), e);
        }
    }

    @Override
    public void exitReview(FormSubmissionActionRequest request) {
        ApprovalInstance instance = getByFormSubmission(request.getSubmissionId(), request.getCollectionName());
        Document formSubmission = formSubmissionStateUpdater.getLatestFormSubmission(instance.getFormSubmissionId(), instance.getFormSubmissionCollectionName());
        validateFreshApprovalData(request, instance, formSubmission, false);
        requireFormSubmissionState(formSubmission, "Form submission must be under review for this approval action.", FormSubmissionState.UNDER_REVIEW);
        if (hasApprovalRecord(instance)) {
            populateFilterSnapshot(instance, formSubmission);
            touch(instance, userIdForAudit(request));
            approvalInstanceRepository.save(instance);
            return;
        }
        FormSubmissionState targetState = fallbackExitReviewState(instance);
        if (omitApprovalActionLog(request)) {
            formSubmissionStateUpdater.updateFormSubmissionState(
                    instance.getFormSubmissionId(),
                    instance.getFormSubmissionCollectionName(),
                    targetState,
                    userIdForAudit(request)
            );
            populateFilterSnapshot(instance, formSubmissionStateUpdater.getLatestFormSubmission(
                    instance.getFormSubmissionId(),
                    instance.getFormSubmissionCollectionName()
            ));
            touch(instance, userIdForAudit(request));
            approvalInstanceRepository.save(instance);
            return;
        }
        guard(instance.getApprovalSteps() != null && !instance.getApprovalSteps().isEmpty(), "Approval instance has no approval steps.");
        ApprovalInstanceStep current = currentStep(instance);
        ApprovalStepState previousStepState = current.getStepState();
        appendActionLog(instance, buildActionLog(instance, request, ApprovalAction.RECALLED, null, true, formSubmission));
        current.setStepState(ApprovalStepState.PENDING);
        touch(instance, userIdForAudit(request));
        approvalInstanceRepository.save(instance);
        try {
            formSubmissionStateUpdater.updateFormSubmissionState(
                    instance.getFormSubmissionId(),
                    instance.getFormSubmissionCollectionName(),
                    targetState,
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
            approvalInstanceRepository.save(instance);
            throw new ApprovalInstanceException("Action failed, approval state rolled back: " + e.getMessage(), e);
        }
    }

    @Override
    public ApprovalInstance editApprovalFlow(ApprovalFlowEditRequest request) {
        var guardContext = formSubmissionMutationGuard.validateWorkflowMutation(request);
        ApprovalInstance instance = guardContext.target().approvalInstance();
        int previousVersion = nullToOne(instance.getVersionNumber());

        List<ApprovalInstanceStep> oldSteps = instance.getApprovalSteps();
        List<ApprovalInstanceStep> newSteps = copyRequestSteps(request.getSteps());
        boolean structuralChange = hasStructuralChange(oldSteps, newSteps);
        int current = safeCurrent(instance);
        Document formSubmission = guardContext.target().submission();
        FormSubmissionState formState = guardContext.state();
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
                        FormSubmissionState.SUBMITTED,
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
        ActionableInstance actionable = requireActionableUnderReview(request, true);
        ApprovalInstance instance = actionable.instance();
        Document formSubmission = actionable.formSubmission();
        validateFreshApprovalData(request, instance, formSubmission);
        ApprovalInstanceStep current = currentStep(instance);
        ApprovalStepState previousStepState = current.getStepState();
        guard(isResolvableCurrentStepState(previousStepState), "Current approval step cannot be resolved in its present state.");
        ApprovalActor actor = actorFromRequest(request);
        guard(actorMatchesStep(current, actor), "Actor does not match the current approval step.");
        int previousSequence = safeCurrent(instance);
        ApprovalActionLog logEntry = buildActionLog(instance, request, actor, ApprovalAction.APPROVED, previousSequence, true, formSubmission);
        appendActionLog(instance, logEntry);
        current.setLastActionRecord(logEntry);
        current.setStepState(ApprovalStepState.APPROVED);
        boolean completed = previousSequence == instance.getApprovalSteps().size() - 1;
        if (!completed) {
            instance.setCurrentStepSequence(previousSequence + 1);
            activateCurrentStep(instance);
        }
        touch(instance, userIdForAudit(request));
        approvalInstanceRepository.save(instance);
        if (completed) {
            try {
                formSubmissionStateUpdater.updateFormSubmissionState(
                        instance.getFormSubmissionId(),
                        instance.getFormSubmissionCollectionName(),
                        FormSubmissionState.SUBMITTED,
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
                current.setLastActionRecord(null);
                removeLastActionLog(instance);
                approvalInstanceRepository.save(instance);
                throw new ApprovalInstanceException("Action failed, approval state rolled back: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public void forward(FormSubmissionActionRequest request) {
        ActionableInstance actionable = requireActionableUnderReview(request, true);
        ApprovalInstance instance = actionable.instance();
        Document formSubmission = actionable.formSubmission();
        validateFreshApprovalData(request, instance, formSubmission);
        int currentSequence = safeCurrent(instance);
        guard(currentSequence < instance.getApprovalSteps().size() - 1, "Cannot forward because there is no next approval step.");
        ApprovalActor actor = actorFromRequest(request);
        guard(actorMatchesCurrentOrLater(instance, actor), "Actor must match current or later approval step to forward.");
        ApprovalInstanceStep current = currentStep(instance);
        guard(isResolvableCurrentStepState(current.getStepState()), "Current approval step cannot be resolved in its present state.");
        ApprovalActionLog logEntry = buildActionLog(instance, request, actor, ApprovalAction.FORWARDED, currentSequence, true, formSubmission);
        appendActionLog(instance, logEntry);
        current.setLastActionRecord(logEntry);
        current.setStepState(ApprovalStepState.FORWARDED);
        instance.setCurrentStepSequence(currentSequence + 1);
        activateCurrentStep(instance);
        touch(instance, userIdForAudit(request));
        approvalInstanceRepository.save(instance);
        populateFilterSnapshot(instance, formSubmission);
        touch(instance, userIdForAudit(request));
        approvalInstanceRepository.save(instance);
    }

    @Override
    public void requestCorrection(FormSubmissionActionRequest request) {
        ActionableInstance actionable = requireActionableUnderReview(request, true);
        ApprovalInstance instance = actionable.instance();
        Document formSubmission = actionable.formSubmission();
        validateFreshApprovalData(request, instance, formSubmission);
        int currentSequence = safeCurrent(instance);
        ApprovalInstanceStep current = currentStep(instance);
        guard(isResolvableCurrentStepState(current.getStepState()), "Current approval step cannot be resolved in its present state.");
        ApprovalActor actor = actorFromRequest(request);
        guard(actorMatchesStep(current, actor), "Actor does not match the current approval step.");
        Integer resumedStepIndex = request.getResumedStepIndex();
        guard(resumedStepIndex == null || resumedStepIndex >= 0,
                "resumedStepIndex must be between 0 and the current step index.");
        guard(resumedStepIndex == null || resumedStepIndex <= currentSequence,
                "resumedStepIndex cannot be greater than the current step index.");
        int targetSequence = resumedStepIndex == null ? currentSequence : resumedStepIndex;
        List<StepStateSnapshot> stepSnapshots = snapshotAllSteps(instance.getApprovalSteps());
        ApprovalActionLog logEntry = buildActionLog(instance, request, ApprovalAction.REQUESTED_CORRECTION, currentSequence, true, formSubmission);
        appendActionLog(instance, logEntry);
        if (resumedStepIndex == null) {
            markAwaitingRevision(current);
        } else {
            for (int i = targetSequence; i < instance.getApprovalSteps().size(); i++) {
                resetStep(instance.getApprovalSteps().get(i));
            }
            instance.setCurrentStepSequence(targetSequence);
            markAwaitingRevision(instance.getApprovalSteps().get(targetSequence));
        }
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
            approvalInstanceRepository.save(instance);
            throw new ApprovalInstanceException("Action failed, approval state rolled back: " + e.getMessage(), e);
        }
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
        populateFilterSnapshot(instance, formSubmissionStateUpdater.getLatestFormSubmission(formSubmissionId, formSubmissionCollectionName));
        touch(instance, updatedBy);
        approvalInstanceRepository.save(instance);
    }

    private record ActionableInstance(ApprovalInstance instance, Document formSubmission) {}

    private ActionableInstance requireActionableUnderReview(FormSubmissionActionRequest request, boolean requireLock) {
        if (requireLock) {
            var guardContext = formSubmissionMutationGuard.validateApprovalMutation(request);
            ApprovalInstance instance = guardContext.target().approvalInstance();
            guard(instance != null && instance.getApprovalSteps() != null && !instance.getApprovalSteps().isEmpty(), "Approval instance has no approval steps.");
            return new ActionableInstance(instance, guardContext.target().submission());
        }
        ApprovalInstance instance = getByFormSubmission(request.getSubmissionId(), request.getCollectionName());
        guard(instance.getApprovalSteps() != null && !instance.getApprovalSteps().isEmpty(), "Approval instance has no approval steps.");
        Document formSubmission = formSubmissionStateUpdater.getLatestFormSubmission(instance.getFormSubmissionId(), instance.getFormSubmissionCollectionName());
        requireFormSubmissionState(formSubmission, "Form submission must be under review for this approval action.", FormSubmissionState.UNDER_REVIEW);
        return new ActionableInstance(instance, formSubmission);
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

    private boolean canActivateForSubmission(FormSubmissionState formState, ApprovalStepState stepState) {
        if (formState == FormSubmissionState.SUBMITTED) {
            return stepState == ApprovalStepState.PENDING;
        }
        if (formState == FormSubmissionState.PENDING_REVISION) {
            return stepState == ApprovalStepState.PENDING || stepState == ApprovalStepState.AWAITING_REVISION;
        }
        return false;
    }

    private boolean hasApprovalRecord(ApprovalInstance instance) {
        return instance.getActionLog() != null && !instance.getActionLog().isEmpty();
    }

    private FormSubmissionState fallbackExitReviewState(ApprovalInstance instance) {
        if (instance.getApprovalSteps() == null || instance.getApprovalSteps().isEmpty()) {
            return FormSubmissionState.SUBMITTED;
        }
        for (ApprovalInstanceStep step : instance.getApprovalSteps()) {
            if (step.getStepState() == ApprovalStepState.AWAITING_REVISION) {
                return FormSubmissionState.PENDING_REVISION;
            }
        }
        return FormSubmissionState.SUBMITTED;
    }

    private boolean omitApprovalActionLog(FormSubmissionActionRequest request) {
        return Boolean.TRUE.equals(request.getOmitApprovalActionLog());
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

    private boolean isResolvableCurrentStepState(ApprovalStepState stepState) {
        return stepState == ApprovalStepState.IN_PROGRESS
                || stepState == ApprovalStepState.PENDING
                || stepState == ApprovalStepState.AWAITING_REVISION;
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
        FormSubmissionState state = approvalModelResolver.resolveLifecycleState(formSubmission);
        guard(List.of(allowedStates).contains(state), message);
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
        FormSubmissionSnapshotForFilter snapshot = new FormSubmissionSnapshotForFilter();
        snapshot.setFormTemplateId(asLong(formSubmission.get("form_template_id")));
        if (snapshot.getFormTemplateId() == null) {
            snapshot.setFormTemplateId(asLong(instance.getFormTemplateId()));
        }
        snapshot.setFormSubmissionState(approvalModelResolver.resolveLifecycleState(formSubmission).dbValue());
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

    private StepStateSnapshot snapshotStep(List<ApprovalInstanceStep> steps, int index) {
        ApprovalInstanceStep step = steps.get(index);
        return new StepStateSnapshot(index, step.getStepState(), step.getLastActionRecord(), step.getResetCounter());
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
}
