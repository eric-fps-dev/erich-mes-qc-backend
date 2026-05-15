package com.fps.svmes.services.impl;

import com.fps.svmes.dto.requests.ApprovalFlowEditRequest;
import com.fps.svmes.dto.requests.FormSubmissionActionRequest;
import com.fps.svmes.enums.form.FormSubmissionLockPurpose;
import com.fps.svmes.enums.form.FormSubmissionState;
import com.fps.svmes.exceptions.ApprovalInstanceException;
import com.fps.svmes.services.FormSubmissionLockService;
import com.fps.svmes.services.FormSubmissionMutationAccessService;
import com.fps.svmes.services.FormSubmissionMutationGuard;
import com.fps.svmes.services.SubmissionApprovalModelResolver;
import com.fps.svmes.services.support.FormSubmissionMutationGuardContext;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class FormSubmissionMutationGuardImpl implements FormSubmissionMutationGuard {
    private final FormSubmissionMutationAccessService mutationAccessService;
    private final FormSubmissionLockService lockService;
    private final SubmissionApprovalModelResolver approvalModelResolver;

    @Override
    public FormSubmissionMutationGuardContext validateEditMutation(FormSubmissionActionRequest request) {
        var target = mutationAccessService.resolveTarget(request.getSubmissionId(), request.getCollectionName(), FormSubmissionLockPurpose.EDIT);
        guard(mutationAccessService.canAccess(target, request.getActorUserId(), request.getActorRoleId(), null, FormSubmissionLockPurpose.EDIT),
                "Only submitted or pending revision form entries can be edited.");
        validateExpectedFormVersion(target.submission(), request.getExpectedFormSubmissionVersion());
        lockService.validateActiveLockOwnership(target.submissionId(), target.collectionName(), request.getActorUserId(), request.getLockToken());
        return new FormSubmissionMutationGuardContext(target, approvalModelResolver.resolveLifecycleState(target.submission()));
    }

    @Override
    public FormSubmissionMutationGuardContext validateDeleteMutation(FormSubmissionActionRequest request) {
        var target = mutationAccessService.resolveTarget(request.getSubmissionId(), request.getCollectionName(), FormSubmissionLockPurpose.DELETE);
        guard(mutationAccessService.canAccess(target, request.getActorUserId(), request.getActorRoleId(), null, FormSubmissionLockPurpose.DELETE),
                "Form submission cannot be deleted in its current state or by this actor.");
        validateExpectedFormVersion(target.submission(), request.getExpectedFormSubmissionVersion());
        lockService.validateActiveLockOwnership(target.submissionId(), target.collectionName(), request.getActorUserId(), request.getLockToken());
        return new FormSubmissionMutationGuardContext(target, approvalModelResolver.resolveLifecycleState(target.submission()));
    }

    @Override
    public FormSubmissionMutationGuardContext validateReviewMutation(FormSubmissionActionRequest request) {
        var target = mutationAccessService.resolveTarget(request.getSubmissionId(), request.getCollectionName(), FormSubmissionLockPurpose.REVIEW);
        guard(mutationAccessService.canAccess(target, request.getActorUserId(), request.getActorRoleId(), null, FormSubmissionLockPurpose.REVIEW),
                "Current user is not allowed to open or close review for this approval step.");
        validateExpectedFormVersion(target.submission(), request.getExpectedFormSubmissionVersion());
        lockService.validateActiveLockOwnership(target.submissionId(), target.collectionName(), request.getActorUserId(), request.getLockToken());
        return new FormSubmissionMutationGuardContext(target, approvalModelResolver.resolveLifecycleState(target.submission()));
    }

    @Override
    public FormSubmissionMutationGuardContext validateApprovalMutation(FormSubmissionActionRequest request) {
        var target = mutationAccessService.resolveTarget(request.getSubmissionId(), request.getCollectionName(), FormSubmissionLockPurpose.APPROVAL_ACTION);
        guard(mutationAccessService.canAccess(target, request.getActorUserId(), request.getActorRoleId(), null, FormSubmissionLockPurpose.APPROVAL_ACTION),
                "Current user is not allowed to act on this approval step.");
        validateExpectedFormVersion(target.submission(), request.getExpectedFormSubmissionVersion());
        lockService.validateActiveLockOwnership(target.submissionId(), target.collectionName(), request.getActorUserId(), request.getLockToken());
        return new FormSubmissionMutationGuardContext(target, approvalModelResolver.resolveLifecycleState(target.submission()));
    }

    @Override
    public FormSubmissionMutationGuardContext validateWorkflowMutation(ApprovalFlowEditRequest request) {
        var target = mutationAccessService.resolveTarget(request.getSubmissionId(), request.getCollectionName(), FormSubmissionLockPurpose.WORKFLOW_ACTION);
        guard(mutationAccessService.canAccess(target, request.getUserId(), null, null, FormSubmissionLockPurpose.WORKFLOW_ACTION),
                "Current user is not allowed to edit this approval flow.");
        validateExpectedFormVersion(target.submission(), request.getExpectedFormSubmissionVersion());
        lockService.validateActiveLockOwnership(target.submissionId(), target.collectionName(), request.getUserId(), request.getLockToken());
        FormSubmissionState state = approvalModelResolver.resolveLifecycleState(target.submission());
        guard(List.of(FormSubmissionState.SUBMITTED, FormSubmissionState.PENDING_REVISION, FormSubmissionState.UNDER_REVIEW).contains(state),
                "Approval flow can only be edited while the form is submitted, pending revision, or under review.");
        return new FormSubmissionMutationGuardContext(target, state);
    }

    private void validateExpectedFormVersion(Document submission, Integer expectedFormVersion) {
        guard(expectedFormVersion != null, "expectedFormSubmissionVersion is required.");
        Object rawVersion = submission.get("version");
        Integer actualVersion = rawVersion instanceof Number number ? number.intValue() : 1;
        guard(Objects.equals(expectedFormVersion, actualVersion), "Form submission data is stale. Please refresh and try again.");
    }

    private void guard(boolean condition, String message) {
        if (!condition) {
            throw new ApprovalInstanceException(message);
        }
    }
}
