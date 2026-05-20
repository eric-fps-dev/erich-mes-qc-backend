package com.fps.svmes.services;

import com.fps.svmes.dto.LegacyMigrationResult;
import com.fps.svmes.dto.requests.ApprovalFlowEditRequest;
import com.fps.svmes.dto.requests.FormSubmissionActionRequest;
import com.fps.svmes.models.nosql.approval.ApprovalInstance;

import java.util.List;

/**
 * Owns live ApprovalInstance workflow state, step progress, and approval action history.
 */
public interface ApprovalInstanceService {
    /**
     * Creates an approval instance from the referenced approval template.
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
     * Starts review for a form submission, optionally as a UI-only review guard.
     */
    void enterReview(FormSubmissionActionRequest request);

    /**
     * Exits a UI review guard when allowed by approval history rules.
     */
    void exitReview(FormSubmissionActionRequest request);

    /**
     * Replaces the approval step list for a submitted, pending-revision, or in-progress instance.
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
     * Requests correction and moves the form submission to pending revision.
     */
    void requestCorrection(FormSubmissionActionRequest request);

    /**
     * Creates missing approval instances for legacy form submissions across existing form collections.
     */
    LegacyMigrationResult backfillMissingApprovalInstances();

    /**
     * Returns the approval instance attached to a form submission.
     */
    ApprovalInstance getByFormSubmission(String formSubmissionId, String formSubmissionCollectionName);

    /**
     * Returns an approval instance by id.
     */
    ApprovalInstance getById(String approvalInstanceId);

    /**
     * Returns approval steps for legacy approval-info compatibility endpoints.
     */
    List<?> getApprovalSteps(String formSubmissionId, String formSubmissionCollectionName);

    /**
     * Refreshes approval-instance filter snapshot fields from the latest form submission.
     */
    void refreshFilterSnapshot(String formSubmissionId, String formSubmissionCollectionName, Long updatedBy);
}
