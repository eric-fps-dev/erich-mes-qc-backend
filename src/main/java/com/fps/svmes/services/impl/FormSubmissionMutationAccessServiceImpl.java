package com.fps.svmes.services.impl;

import com.fps.svmes.enums.approval.ApprovalStepState;
import com.fps.svmes.enums.form.FormSubmissionLockPurpose;
import com.fps.svmes.enums.form.FormSubmissionState;
import com.fps.svmes.exceptions.ApprovalInstanceException;
import com.fps.svmes.repositories.mongoRepo.ApprovalInstanceRepository;
import com.fps.svmes.services.FormSubmissionMutationAccessService;
import com.fps.svmes.services.SubmissionApprovalModelResolver;
import com.fps.svmes.services.support.ResolvedFormSubmissionTarget;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class FormSubmissionMutationAccessServiceImpl implements FormSubmissionMutationAccessService {
    private final MongoTemplate mongoTemplate;
    private final ApprovalInstanceRepository approvalInstanceRepository;
    private final SubmissionApprovalModelResolver approvalModelResolver;

    @Override
    public ResolvedFormSubmissionTarget resolveTarget(String submissionId, String requestedCollectionName, FormSubmissionLockPurpose purpose) {
        if (purpose == FormSubmissionLockPurpose.APPROVAL_ACTION
                || purpose == FormSubmissionLockPurpose.WORKFLOW_ACTION
                || purpose == FormSubmissionLockPurpose.REVIEW) {
            return resolveApprovalTarget(submissionId, requestedCollectionName);
        }
        return resolveSubmissionTarget(submissionId, requestedCollectionName);
    }

    @Override
    public boolean canAccess(ResolvedFormSubmissionTarget target,
                             Long actorUserId,
                             String actorRoleId,
                             List<String> actorRoleIds,
                             FormSubmissionLockPurpose purpose) {
        FormSubmissionState state = approvalModelResolver.resolveLifecycleState(target.submission());
        return switch (purpose) {
            case EDIT -> List.of(FormSubmissionState.SUBMITTED, FormSubmissionState.PENDING_REVISION).contains(state)
                    || (state == FormSubmissionState.UNDER_REVIEW && actorMatchesCurrentApprovalStep(target, actorUserId, actorRoleId, actorRoleIds));
            case DELETE -> List.of(FormSubmissionState.SUBMITTED, FormSubmissionState.PENDING_REVISION).contains(state)
                    || (state == FormSubmissionState.UNDER_REVIEW && actorMatchesCurrentApprovalStep(target, actorUserId, actorRoleId, actorRoleIds));
            case APPROVAL_ACTION -> actorMatchesCurrentApprovalStep(target, actorUserId, actorRoleId, actorRoleIds);
            case WORKFLOW_ACTION -> List.of(FormSubmissionState.SUBMITTED, FormSubmissionState.PENDING_REVISION).contains(state)
                    || (state == FormSubmissionState.UNDER_REVIEW && actorMatchesCurrentApprovalStep(target, actorUserId, actorRoleId, actorRoleIds));
            case REVIEW -> actorMatchesCurrentApprovalStep(target, actorUserId, actorRoleId, actorRoleIds);
        };
    }

    private ResolvedFormSubmissionTarget resolveSubmissionTarget(String submissionId, String requestedCollectionName) {
        validateCollectionName(requestedCollectionName);
        validateSubmissionId(submissionId);
        Document submission = mongoTemplate.findOne(
                new Query(Criteria.where("_id").is(new ObjectId(submissionId))),
                Document.class,
                requestedCollectionName
        );
        if (submission == null) {
            throw new IllegalArgumentException("Submission not found: " + submissionId);
        }
        var approvalInstance = approvalInstanceRepository
                .findByFormSubmissionIdAndFormSubmissionCollectionName(submissionId, requestedCollectionName)
                .orElse(null);
        return new ResolvedFormSubmissionTarget(submissionId, requestedCollectionName, submission, approvalInstance);
    }

    private ResolvedFormSubmissionTarget resolveApprovalTarget(String submissionId, String requestedCollectionName) {
        validateSubmissionId(submissionId);
        var approvalInstance = approvalInstanceRepository.findByFormSubmissionId(submissionId)
                .orElseThrow(() -> new ApprovalInstanceException("Approval instance not found for form submission: " + submissionId));
        if (requestedCollectionName != null && !requestedCollectionName.isBlank()
                && !Objects.equals(requestedCollectionName, approvalInstance.getFormSubmissionCollectionName())) {
            throw new IllegalArgumentException("collectionName does not match the active approval instance target.");
        }
        String collectionName = approvalInstance.getFormSubmissionCollectionName();
        Document submission = mongoTemplate.findOne(
                new Query(Criteria.where("_id").is(new ObjectId(submissionId))),
                Document.class,
                collectionName
        );
        if (submission == null) {
            throw new IllegalArgumentException("Submission not found: " + submissionId);
        }
        return new ResolvedFormSubmissionTarget(submissionId, collectionName, submission, approvalInstance);
    }

    private boolean actorMatchesCurrentApprovalStep(ResolvedFormSubmissionTarget target,
                                                    Long actorUserId,
                                                    String actorRoleId,
                                                    List<String> actorRoleIds) {
        if (target.approvalInstance() == null
                || target.approvalInstance().getApprovalSteps() == null
                || target.approvalInstance().getApprovalSteps().isEmpty()) {
            return false;
        }
        int currentSequence = target.approvalInstance().getCurrentStepSequence() == null
                ? 0
                : target.approvalInstance().getCurrentStepSequence();
        if (currentSequence < 0 || currentSequence >= target.approvalInstance().getApprovalSteps().size()) {
            return false;
        }
        var currentStep = target.approvalInstance().getApprovalSteps().get(currentSequence);
        if (currentStep.getStepState() != ApprovalStepState.IN_PROGRESS
                && currentStep.getStepState() != ApprovalStepState.AWAITING_REVISION
                && currentStep.getStepState() != ApprovalStepState.PENDING) {
            return false;
        }
        boolean matchesUser = actorUserId != null
                && currentStep.getRequiredUserId() != null
                && Objects.equals(currentStep.getRequiredUserId(), String.valueOf(actorUserId));
        List<String> allRoleIds = new ArrayList<>();
        if (actorRoleId != null && !actorRoleId.isBlank()) {
            allRoleIds.add(actorRoleId);
        }
        if (actorRoleIds != null) {
            allRoleIds.addAll(actorRoleIds.stream().filter(Objects::nonNull).toList());
        }
        boolean matchesRole = currentStep.getRequiredRoleId() != null
                && allRoleIds.stream().anyMatch(currentStep.getRequiredRoleId()::equals);
        return matchesUser || matchesRole;
    }

    private void validateSubmissionId(String submissionId) {
        if (!ObjectId.isValid(submissionId)) {
            throw new IllegalArgumentException("Invalid submissionId format: " + submissionId);
        }
    }

    private void validateCollectionName(String collectionName) {
        if (collectionName == null || collectionName.isBlank()) {
            throw new IllegalArgumentException("collectionName is required.");
        }
    }
}
