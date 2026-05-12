package com.fps.svmes.services;

import com.fps.svmes.dto.PagedResultDTO;
import com.fps.svmes.dto.dtos.approval.ApprovalInstanceDTO;
import com.fps.svmes.dto.dtos.approval.ApprovalInstanceListItemDTO;
import com.fps.svmes.dto.requests.ApprovalFlowEditRequest;
import com.fps.svmes.dto.requests.ApprovalInstanceQueryRequest;
import org.bson.Document;

import java.util.List;
import java.util.Map;

/**
 * Owns form-submissions lifecycle, versioning, and integration with the approval workflow.
 */
public interface QcFormDataService {
    /**
     * Inserts a form submission and creates the corresponding approval instance.
     */
    Map<String, Object> insertFormData(String collectionName, Long userId, Map<String, Object> formData, boolean submitForApproval);

    /**
     * Creates a new version of an existing submitted or pending-revision form submission and
     * retains the prior version as submitted.
     */
    Map<String, Object> editFormData(String collectionName, Long userId, String parentSubmissionId, Long formTemplateId,
                                     Integer expectedFormSubmissionVersion, String lockToken, String lockSessionId,
                                     Map<String, Object> updatedData);

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
    ApprovalInstanceDTO getApprovalInstance(String submissionId, String collectionName);

    /**
     * Returns approval steps for legacy approval-info compatibility.
     */
    List<?> getApprovalSteps(String submissionId, String collectionName);

    /**
     * Returns the full approval instance by approval-instance id.
     */
    ApprovalInstanceDTO getApprovalInstanceById(String approvalInstanceId);

    /**
     * Returns paginated approval-instance list rows backed by approval-instance filter snapshots.
     */
    PagedResultDTO<ApprovalInstanceListItemDTO> getApprovalInstances(ApprovalInstanceQueryRequest request);
}
