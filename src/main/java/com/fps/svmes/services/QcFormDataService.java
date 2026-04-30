package com.fps.svmes.services;

import com.fps.svmes.dto.PagedResultDTO;
import com.fps.svmes.dto.dtos.qcForm.ApprovalInstanceListItemDTO;
import com.fps.svmes.dto.requests.ApprovalFlowEditRequest;
import com.fps.svmes.dto.requests.ApprovalInstanceQueryRequest;
import com.fps.svmes.dto.requests.FormSubmissionActionRequest;
import com.fps.svmes.enums.form.FormSubmissionState;
import com.fps.svmes.models.nosql.approval.ApprovalInstance;
import org.bson.Document;

import java.util.List;
import java.util.Map;

/**
 * Owns form-submissions lifecycle, versioning, and integration with the approval workflow.
 */
public interface QcFormDataService {
    /**
     * Inserts a form submission and creates the corresponding approval instance. When the form
     * template has no approval template configured, the form is auto-archived with an empty-step
     * approval instance. When {@code submitForApproval} is true and the instance has approval
     * steps, the form is also moved into the approval workflow in the same call.
     */
    Map<String, Object> insertFormData(String collectionName, Long userId, Map<String, Object> formData, boolean submitForApproval);

    /**
     * Creates a new version of an existing draft or pending-revision form submission and
     * retains the prior version as either void or archived.
     */
    Map<String, Object> editFormData(String collectionName, Long userId, String parentSubmissionId, Long formTemplateId,
                                     FormSubmissionState previousRecordState, Map<String, Object> updatedData);

    /**
     * Transitions a form submission to void instead of physically deleting it.
     */
    void voidFormSubmission(FormSubmissionActionRequest request);

    /**
     * Submits a draft form submission into the approval workflow.
     */
    void submitForApproval(FormSubmissionActionRequest request);

    /**
     * Recalls an under-review form submission back to draft.
     */
    void recall(FormSubmissionActionRequest request);

    /**
     * Approves the current approval step for a form submission.
     */
    void approve(FormSubmissionActionRequest request);

    /**
     * Forwards the current approval step to the next step.
     */
    void forward(FormSubmissionActionRequest request);

    /**
     * Requests correction for a form submission and moves it to pending revision.
     */
    void requestCorrection(FormSubmissionActionRequest request);

    /**
     * Rejects a form submission and voids it.
     */
    void rejectDiscard(FormSubmissionActionRequest request);

    /**
     * Edits the approval flow instance attached to a form submission.
     */
    Document editApprovalFlow(ApprovalFlowEditRequest request);

    /**
     * Returns all versions for a form submission version group.
     */
    List<Document> getVersionHistory(String submissionId, String collectionName);

    /**
     * Returns the approval instance attached to a form submission for audit/history reads.
     */
    Object getApprovalInstance(String submissionId, String collectionName);

    /**
     * Returns approval steps for legacy approval-info compatibility.
     */
    List<?> getApprovalSteps(String submissionId, String collectionName);

    /**
     * Returns the full approval instance by approval-instance id.
     */
    ApprovalInstance getApprovalInstanceById(String approvalInstanceId);

    /**
     * Returns paginated approval-instance list rows backed by approval-instance filter snapshots.
     */
    PagedResultDTO<ApprovalInstanceListItemDTO> getApprovalInstances(ApprovalInstanceQueryRequest request);
}
