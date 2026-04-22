package com.fps.svmes.controllers;

import com.fps.svmes.dto.dtos.qcForm.QcApprovalAssignmentDTO;
import com.fps.svmes.dto.requests.ApprovalActionRequest;
import com.fps.svmes.dto.responses.ResponseResult;
import com.fps.svmes.services.QcApprovalAssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import org.bson.Document;

import java.util.List;

@RestController
@RequestMapping("/approval")
@Tag(name = "QC Approval Assignment API", description = "API for viewing QC approval assignments")
@RequiredArgsConstructor
public class ApprovalController {

    private final QcApprovalAssignmentService approvalAssignmentService;

    private static final Logger logger = LoggerFactory.getLogger(ApprovalController.class);

    /**
     * Get all approval assignments with pagination.
     *
     * @param page Page number (starting from 0)
     * @param size Page size (default 10)
     * @return paginated QcApprovalAssignmentDTO list
     */
    @GetMapping("/assignments")
    @Operation(summary = "Get paginated approval assignments", description = "Returns paginated list of QC approval assignments")
    public ResponseResult<Page<QcApprovalAssignmentDTO>> getAllApprovalAssignments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<QcApprovalAssignmentDTO> result = approvalAssignmentService.getAllAssignments(pageable);
            return ResponseResult.success(result);
        } catch (Exception e) {
            logger.error("Error retrieving approval assignments", e);
            return ResponseResult.fail("Failed to retrieve approval assignments", e);
        }
    }

    @GetMapping("/assignments-filter")
    @Operation(summary = "Get paginated approval assignments", description = "Returns paginated list of QC approval assignments")
    public ResponseResult<Page<QcApprovalAssignmentDTO>> getFilteredApprovalAssignments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String approvalType,
            @RequestParam(required = false) String templateName,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection
    ) {
        try {
            Sort sort = Sort.unsorted();
            if (sortBy != null && !sortBy.isBlank()) {
                sort = "desc".equalsIgnoreCase(sortDirection) ?
                        Sort.by(sortBy).descending() :
                        Sort.by(sortBy).ascending();
            }
            Pageable pageable = PageRequest.of(page, size, sort);
            Page<QcApprovalAssignmentDTO> result = approvalAssignmentService.getFilteredAssignments(
                    state, approvalType, templateName, startDate, endDate, pageable
            );
            return ResponseResult.success(result);
        } catch (Exception e) {
            logger.error("Error retrieving approval assignments", e);
            return ResponseResult.fail("Failed to retrieve approval assignments", e);
        }
    }

    @GetMapping("/version-history")
    @Operation(summary = "Get version history by submission ID", description = "Returns all versions of a submission sorted by version desc")
    public ResponseResult<List<Document>> getVersionHistoryBySubmissionId(
            @RequestParam String submissionId,
            @RequestParam String collectionName
            ) {
        try {
            List<Document> versions = approvalAssignmentService.getVersionHistory(submissionId, collectionName);
            return ResponseResult.success(versions);
        } catch (Exception e) {
            logger.error("Error retrieving version history", e);
            return ResponseResult.fail("Failed to retrieve version history", e);
        }
    }

    @PostMapping("/approve")
    @Operation(summary = "Approve action for current approver", description = "Sets signature, comment, status and updates next step")
    public ResponseResult<String> approve(@RequestBody ApprovalActionRequest request) {
        try {
            approvalAssignmentService.approveAction(
                    request.getSubmissionId(),
                    request.getCollectionName(),
                    request.getApproverId(),
                    request.getComment(),
                    request.isSuggestRetest(),
                    request.getESignature()
            );
            return ResponseResult.success("Approval submitted successfully");
        } catch (Exception e) {
            logger.error("Error during approval action", e);
            return ResponseResult.fail("Approval action failed", e);
        }
    }

    @GetMapping("/approval-info")
    @Operation(summary = "Get approval_info for a submission", description = "Returns the approval steps for a given submission")
    public ResponseResult<List<Document>> getApprovalInfoBySubmissionId(
            @RequestParam String submissionId,
            @RequestParam String collectionName
    ) {
        try {
            List<Document> approvalInfo = approvalAssignmentService.getApprovalInfo(submissionId, collectionName);
            return ResponseResult.success(approvalInfo);
        } catch (Exception e) {
            logger.error("Error retrieving approval_info", e);
            return ResponseResult.fail("Failed to retrieve approval_info", e);
        }
    }


}
