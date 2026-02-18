package com.fps.svmes.services;

import com.fps.svmes.dto.dtos.qcForm.QcApprovalAssignmentDTO;
import org.bson.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface QcApprovalAssignmentService {
    void insertIfNotExists(QcApprovalAssignmentDTO dto);

    Page<QcApprovalAssignmentDTO> getAllAssignments(Pageable pageable);

    Page<QcApprovalAssignmentDTO> getFilteredAssignments(
            String state,
            String approvalType,
            String templateName,
            String startDate,
            String endDate,
            Pageable pageable
    );

    List<Document> getVersionHistory(String submissionId, String collectionName);

    void approveAction(String submissionId, String collectionName, Integer approverId, String comment, boolean suggestRetest, String eSignatureBase64);

    List<Document> getApprovalInfo(String submissionId, String collectionName);

    void updateSubmissionId(String oldSubmissionId, String newSubmissionId);

}