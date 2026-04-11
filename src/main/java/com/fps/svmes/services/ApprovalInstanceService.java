package com.fps.svmes.services;

import com.fps.svmes.dto.requests.ApprovalFlowEditRequest;
import com.fps.svmes.dto.requests.FormSubmissionActionRequest;
import com.fps.svmes.models.nosql.approval.ApprovalInstance;

import java.util.List;

/**
 * Owns live ApprovalInstance workflow state, step progress, and approval action history.
 */
public interface ApprovalInstanceService {
    /**
     * Creates a draft approval instance from the referenced approval template.
     */
    ApprovalInstance create(String formSubmissionId, String formSubmissionCollectionName, Long formTemplateId, String approvalTemplateId, Long createdBy);

    /**
     * Verifies that an approval template exists and has executable step data.
     */
    void validateApprovalTemplateExists(String approvalTemplateId);

    /**
     * Moves the approval instance pointer to the latest form-submission version after an edit.
     */
    void onFormSubmissionEdited(String oldFormSubmissionId, String newFormSubmissionId, String formSubmissionCollectionName, Long updatedBy);

    /**
     * Voids the approval instance when the owning form submission is voided.
     */
    void voidForFormSubmissionDelete(FormSubmissionActionRequest request);

    /**
     * Starts the approval workflow and signals the form submission as under review.
     */
    void submitForApproval(FormSubmissionActionRequest request);

    /**
     * Recalls an in-progress approval instance back to draft.
     */
    void recall(FormSubmissionActionRequest request);

    /**
     * Replaces the approval step list for a draft or in-progress instance.
     */
    ApprovalInstance editApprovalFlow(ApprovalFlowEditRequest request);

    /**
     * Approves the current step and archives the form submission when the final step completes.
     */
    void approve(FormSubmissionActionRequest request);

    /**
     * Forwards the current step to the next approval step.
     */
    void forward(FormSubmissionActionRequest request);

    /**
     * Rejects the current review and resets all approval steps.
     */
    void rejectFullReset(FormSubmissionActionRequest request);

    /**
     * Rejects the current review and rolls the workflow back one step.
     */
    void rejectPartialReset(FormSubmissionActionRequest request);

    /**
     * Rejects the current review and voids the approval instance.
     */
    void rejectDiscard(FormSubmissionActionRequest request);

    /**
     * Returns the active approval instance used by workflow mutations.
     */
    ApprovalInstance getActiveByFormSubmission(String formSubmissionId, String formSubmissionCollectionName);

    /**
     * Returns an approval instance for audit/history reads, including voided instances.
     */
    ApprovalInstance getByFormSubmissionIncludingVoid(String formSubmissionId, String formSubmissionCollectionName);

    /**
     * Returns an approval instance by id for audit/history reads, including voided instances.
     */
    ApprovalInstance getByIdIncludingVoid(String approvalInstanceId);

    /**
     * Returns approval steps for legacy approval-info compatibility endpoints.
     */
    List<?> getApprovalSteps(String formSubmissionId, String formSubmissionCollectionName);

    /**
     * Refreshes approval-instance filter snapshot fields from the latest form submission.
     */
    void refreshFilterSnapshot(String formSubmissionId, String formSubmissionCollectionName, Long updatedBy);
}
