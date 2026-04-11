package com.fps.svmes.controllers;

import com.fps.shared.dto.responses.ResponseResult;
import com.fps.shared.dto.responses.ResponseStatus;
import com.fps.svmes.dto.PagedResultDTO;
import com.fps.svmes.dto.dtos.qcForm.ApprovalInstanceListItemDTO;
import com.fps.svmes.dto.dtos.qcForm.QcApprovalAssignmentDTO;
import com.fps.svmes.dto.requests.ApprovalInstanceQueryRequest;
import com.fps.svmes.dto.requests.ApprovalFlowEditRequest;
import com.fps.svmes.dto.requests.FormSubmissionActionRequest;
import com.fps.svmes.enums.form.FormSubmissionState;
import com.fps.svmes.exceptions.ApprovalInstanceException;
import com.fps.svmes.services.QcApprovalAssignmentService;
import com.fps.svmes.services.QcFormDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Exposes form-submission lifecycle and approval-instance workflow endpoints.
 */
@RestController
@Slf4j
@RequestMapping("/qc-form-data")
@RequiredArgsConstructor
@Tag(name = "QC Form Data API", description = "API for QC Form Data")
public class QcFormDataController {
    private final QcFormDataService qcFormDataService;
    private final QcApprovalAssignmentService approvalAssignmentService;

    private static final Logger logger = LoggerFactory.getLogger(QcFormDataController.class);

    /**
     * Get all approval assignments with pagination.
     *
     * @param page Page number (starting from 0)
     * @param size Page size (default 10)
     * @return paginated QcApprovalAssignmentDTO list
     */
    @GetMapping("/assignments")
    @Deprecated(since = "2026-04-07", forRemoval = true)
    @Operation(summary = "Get paginated approval assignments", description = "Returns paginated list of QC approval assignments")
    public ResponseEntity<ResponseResult<Page<QcApprovalAssignmentDTO>>> getAllApprovalAssignments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<QcApprovalAssignmentDTO> result = approvalAssignmentService.getAllAssignments(pageable);
            return ResponseResult.of(result, ResponseStatus.SUCCESS);
        } catch (Exception e) {
            logger.error("Error retrieving approval assignments", e);
            return ResponseResult.fail("Failed to retrieve approval assignments" + e.getMessage(), ResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Deprecated(since = "2026-04-07", forRemoval = true)
    @GetMapping("/assignments-filter")
    @Operation(summary = "Get paginated approval assignments", description = "Returns paginated list of QC approval assignments")
    public ResponseEntity<ResponseResult<Page<QcApprovalAssignmentDTO>>> getFilteredApprovalAssignments(
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
            return ResponseResult.of(result, ResponseStatus.SUCCESS);
        } catch (Exception e) {
            logger.error("Error retrieving approval assignments", e);
            return ResponseResult.fail(e.getMessage(), ResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Deprecated(since = "2026-04-07", forRemoval = true)
    @GetMapping("/approval-info")
    @Operation(summary = "Get approval_info for a submission", description = "Returns the approval steps for a given submission")
    public ResponseEntity<ResponseResult<List<?>>> getApprovalInfoBySubmissionId(
            @RequestParam String submissionId,
            @RequestParam String collectionName
    ) {
        try {
            List<?> approvalInfo = qcFormDataService.getApprovalSteps(submissionId, collectionName);
            return ResponseResult.of(approvalInfo, ResponseStatus.SUCCESS);
        } catch (Exception e) {
            logger.error("Error retrieving approval_info", e);
            return ResponseResult.fail("Failed to retrieve approval_info" + e.getMessage(), ResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Returns paginated approval instances using approval-instance filter snapshots.
     */
    @GetMapping("/approval-instances")
    @Operation(
            summary = "Search approval instances",
            description = "Returns paginated approval-instance rows using DB-level pagination and approval-instance filter snapshots. "
                    + "The row shape matches the previous form-submission list contract. "
                    + "When include_form_data=true, the endpoint hydrates the full latest form document for the returned page items and may be more expensive."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Approval instances returned successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid filter or pagination parameter"),
            @ApiResponse(responseCode = "500", description = "Unexpected error retrieving approval instances")
    })
    public ResponseEntity<ResponseResult<PagedResultDTO<ApprovalInstanceListItemDTO>>> getApprovalInstances(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "created_at") String sortField,
            @RequestParam(defaultValue = "DESC") Sort.Direction sortDirection,
            @RequestParam(name = "include_form_data", defaultValue = "false") boolean includeFormData,
            @RequestParam(name = "submitter_user_id", required = false) Long submitterUserId,
            @RequestParam(name = "inspector_user_id", required = false) Long inspectorUserId,
            @RequestParam(name = "suggested_product_id", required = false) Long suggestedProductId,
            @RequestParam(name = "suggested_batch_id", required = false) Long suggestedBatchId,
            @RequestParam(name = "team_id", required = false) Long teamId,
            @RequestParam(name = "shift_id", required = false) Long shiftId,
            @RequestParam(name = "form_template_id", required = false) Long formTemplateId,
            @RequestParam(name = "approval_template_id", required = false) String approvalTemplateId,
            @RequestParam(name = "created_at_start", required = false) String createdAtStart,
            @RequestParam(name = "created_at_end", required = false) String createdAtEnd,
            @RequestParam(name = "form_submission_state", required = false) String formSubmissionState
    ) {
        try {
            return ResponseResult.of(qcFormDataService.getApprovalInstances(buildApprovalInstanceQueryRequest(
                    page,
                    size,
                    sortField,
                    sortDirection,
                    includeFormData,
                    submitterUserId,
                    inspectorUserId,
                    suggestedProductId,
                    suggestedBatchId,
                    teamId,
                    shiftId,
                    formTemplateId,
                    approvalTemplateId,
                    createdAtStart,
                    createdAtEnd,
                    formSubmissionState
            )), ResponseStatus.SUCCESS);
        } catch (IllegalArgumentException e) {
            return ResponseResult.fail(e.getMessage(), ResponseStatus.BAD_REQUEST, e);
        } catch (Exception e) {
            log.error("Error retrieving approval instances", e);
            return ResponseResult.fail("Error retrieving approval instances: " + e.getMessage(), ResponseStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    /**
     * Returns paginated form submissions with workflow metadata for list views.
     */
    @GetMapping("/form-submissions")
    @Deprecated(since = "2026-04-09", forRemoval = true)
    @Operation(
            summary = "Search form submissions",
            description = "Deprecated compatibility endpoint. Returns approval-instance-backed list rows using the legacy route."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Form submissions returned successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid filter or pagination parameter"),
            @ApiResponse(responseCode = "500", description = "Unexpected error retrieving form submissions")
    })
    public ResponseEntity<ResponseResult<PagedResultDTO<ApprovalInstanceListItemDTO>>> getFormSubmissions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "created_at") String sortField,
            @RequestParam(defaultValue = "DESC") Sort.Direction sortDirection,
            @RequestParam(name = "include_form_data", defaultValue = "false") boolean includeFormData,
            @RequestParam(name = "submitter_user_id", required = false) Long submitterUserId,
            @RequestParam(name = "inspector_user_id", required = false) Long inspectorUserId,
            @RequestParam(name = "suggested_product_id", required = false) Long suggestedProductId,
            @RequestParam(name = "suggested_batch_id", required = false) Long suggestedBatchId,
            @RequestParam(name = "team_id", required = false) Long teamId,
            @RequestParam(name = "shift_id", required = false) Long shiftId,
            @RequestParam(name = "form_template_id", required = false) Long formTemplateId,
            @RequestParam(name = "approval_template_id", required = false) String approvalTemplateId,
            @RequestParam(name = "created_at_start", required = false) String createdAtStart,
            @RequestParam(name = "created_at_end", required = false) String createdAtEnd,
            @RequestParam(name = "form_submission_state", required = false) String formSubmissionState
    ) {
        try {
            return ResponseResult.of(qcFormDataService.getApprovalInstances(buildApprovalInstanceQueryRequest(
                    page,
                    size,
                    sortField,
                    sortDirection,
                    includeFormData,
                    submitterUserId,
                    inspectorUserId,
                    suggestedProductId,
                    suggestedBatchId,
                    teamId,
                    shiftId,
                    formTemplateId,
                    approvalTemplateId,
                    createdAtStart,
                    createdAtEnd,
                    formSubmissionState
            )), ResponseStatus.SUCCESS);
        } catch (IllegalArgumentException e) {
            return ResponseResult.fail(e.getMessage(), ResponseStatus.BAD_REQUEST, e);
        } catch (Exception e) {
            log.error("Error retrieving form submissions", e);
            return ResponseResult.fail("Error retrieving form submissions: " + e.getMessage(), ResponseStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    /**
     * Creates a draft form submission in the dynamic form collection.
     */
    @PostMapping("/insert-form/{userId}/{collectionName}")
    @Operation(
            summary = "Create draft form submission",
            description = "Creates a draft form submission in the dynamic Mongo collection, initializes version to 1, and creates the approval instance when an approval template is configured."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Draft form submission created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid collection name, form template, or form data"),
            @ApiResponse(responseCode = "500", description = "Unexpected error inserting form data")
    })
    public ResponseEntity<ResponseResult<Map<String, Object>>> insertFormData(
            @PathVariable String collectionName,
            @PathVariable Long userId,
            @RequestBody Map<String, Object> formData) {
        try {
            return ResponseResult.of(qcFormDataService.insertFormData(collectionName, userId, formData), ResponseStatus.SUCCESS);
        } catch (IllegalArgumentException e) {
            return ResponseResult.fail(e.getMessage(), ResponseStatus.BAD_REQUEST, e);
        } catch (Exception e) {
            log.error("Error inserting form data", e);
            return ResponseResult.fail("Error inserting form data: " + e.getMessage(), ResponseStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    /**
     * Creates a new version of a draft or pending-revision form submission while preserving version history.
     */
    @PostMapping("/edit-form/{userId}/{collectionName}")
    @Operation(
            summary = "Edit a form submission",
            description = "Creates a new version of an existing draft or pending revision form submission while preserving version history. "
                    + "Requires parentId and templateId. Optional previous_record_state accepts 'void' or 'archived' and defaults to 'void'. "
                    + "Archived old versions are kept for history and excluded from normal form-submission list results."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Form submission version created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid previous_record_state value"),
            @ApiResponse(responseCode = "404", description = "Original form submission was not found"),
            @ApiResponse(responseCode = "409", description = "Form submission cannot be edited in its current state"),
            @ApiResponse(responseCode = "500", description = "Unexpected error editing form data")
    })
    public ResponseEntity<ResponseResult<Map<String, Object>>> editFormData(
            @PathVariable String collectionName,
            @PathVariable Long userId,
            @RequestParam("parentId") String parentSubmissionId,
            @RequestParam("templateId") Long formTemplateId,
            @RequestParam(name = "previous_record_state", defaultValue = "void") String previousRecordState,
            @RequestBody Map<String, Object> updatedData) {
        try {
            return ResponseResult.of(
                    qcFormDataService.editFormData(
                            collectionName,
                            userId,
                            parentSubmissionId,
                            formTemplateId,
                            parsePreviousRecordState(previousRecordState),
                            updatedData
                    ),
                    ResponseStatus.SUCCESS
            );
        } catch (IllegalArgumentException e) {
            ResponseStatus status = isBadEditRequest(e) ? ResponseStatus.BAD_REQUEST : ResponseStatus.NOT_FOUND;
            return ResponseResult.fail(e.getMessage(), status, e);
        } catch (IllegalStateException e) {
            return ResponseResult.fail(e.getMessage(), ResponseStatus.CONFLICT, e);
        } catch (Exception e) {
            log.error("Error editing form data", e);
            return ResponseResult.fail("Error editing form data: " + e.getMessage(), ResponseStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    /**
     * Voids a form submission and its approval instance when the state allows deletion.
     */
    @DeleteMapping("/form-submission")
    @Operation(
            summary = "Void form submission",
            description = "Transitions a form submission and its approval instance to void instead of physically deleting data. Request requires actorUserId."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Form submission voided successfully"),
            @ApiResponse(responseCode = "409", description = "Form submission or approval instance cannot be voided in its current state"),
            @ApiResponse(responseCode = "500", description = "Unexpected error voiding form submission")
    })
    public ResponseEntity<ResponseResult<String>> voidFormSubmission(@Valid @RequestBody FormSubmissionActionRequest request) {
        try {
            qcFormDataService.voidFormSubmission(request);
            return ResponseResult.of("Form submission voided successfully", ResponseStatus.SUCCESS);
        } catch (IllegalStateException | ApprovalInstanceException e) {
            return ResponseResult.fail(e.getMessage(), ResponseStatus.CONFLICT, e);
        } catch (Exception e) {
            log.error("Error voiding form submission", e);
            return ResponseResult.fail("Error voiding form submission: " + e.getMessage(), ResponseStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    /**
     * Moves a draft or pending-revision form submission into the active approval workflow.
     */
    @PostMapping("/submit-for-approval")
    @Operation(
            summary = "Submit form submission for approval",
            description = "Moves a draft or pending revision form submission into the approval workflow. Request requires actorUserId and expectedFormSubmissionVersion. expectedApprovalInstanceVersion is optional for this action."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Submit-for-approval payload. Workers do not need approval-instance version access for this action.",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = FormSubmissionActionRequest.class),
                    examples = @ExampleObject(
                            name = "SubmitForApproval",
                            summary = "Submit without approval instance version",
                            value = """
                                    {
                                      "submissionId": "69d44796b8b3934d9cb382f7",
                                      "collectionName": "form_template_695_202604",
                                      "actorUserId": 274,
                                      "expectedFormSubmissionVersion": 1,
                                      "comment": "submit for approval"
                                    }
                                    """
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Form submission submitted for approval"),
            @ApiResponse(responseCode = "409", description = "State is invalid or the submitted versions are stale"),
            @ApiResponse(responseCode = "500", description = "Unexpected error submitting for approval")
    })
    public ResponseEntity<ResponseResult<String>> submitForApproval(@Valid @RequestBody FormSubmissionActionRequest request) {
        try {
            qcFormDataService.submitForApproval(request);
            return ResponseResult.of("Form submission submitted for approval", ResponseStatus.SUCCESS);
        } catch (IllegalStateException | ApprovalInstanceException e) {
            return ResponseResult.fail(e.getMessage(), ResponseStatus.CONFLICT, e);
        } catch (Exception e) {
            log.error("Error submitting form submission for approval", e);
            return ResponseResult.fail("Error submitting for approval: " + e.getMessage(), ResponseStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    /**
     * Recalls an under-review form submission back to draft for editing.
     */
    @PostMapping("/recall")
    @Operation(
            summary = "Recall form submission to draft",
            description = "Recalls an in-progress approval instance back to draft without resetting the current step sequence. Request must include expectedFormSubmissionVersion. expectedApprovalInstanceVersion is optional for this action."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Recall payload. Workers do not need approval-instance version access for this action.",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = FormSubmissionActionRequest.class),
                    examples = @ExampleObject(
                            name = "RecallToDraft",
                            summary = "Recall without approval instance version",
                            value = """
                                    {
                                      "submissionId": "69d44796b8b3934d9cb382f7",
                                      "collectionName": "form_template_695_202604",
                                      "actorUserId": 274,
                                      "expectedFormSubmissionVersion": 1,
                                      "comment": "need to revise"
                                    }
                                    """
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Form submission recalled to draft"),
            @ApiResponse(responseCode = "409", description = "State is invalid or the submitted versions are stale"),
            @ApiResponse(responseCode = "500", description = "Unexpected error recalling form submission")
    })
    public ResponseEntity<ResponseResult<String>> recall(@Valid @RequestBody FormSubmissionActionRequest request) {
        try {
            qcFormDataService.recall(request);
            return ResponseResult.of("Form submission recalled to draft", ResponseStatus.SUCCESS);
        } catch (IllegalStateException | ApprovalInstanceException e) {
            return ResponseResult.fail(e.getMessage(), ResponseStatus.CONFLICT, e);
        } catch (Exception e) {
            log.error("Error recalling form submission", e);
            return ResponseResult.fail("Error recalling form submission: " + e.getMessage(), ResponseStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    /**
     * Approves the current approval step.
     */
    @PostMapping("/approval/approve")
    @Operation(
            summary = "Approve current step",
            description = "Approves the current approval step. Role-based steps validate actorRoleId only; actorRoleName is optional display context. Request must include actorUserId and expected versions."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Approval action payload. For role-based steps provide actorRoleId; actorRoleName is optional and used only for display/audit context.",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = FormSubmissionActionRequest.class),
                    examples = @ExampleObject(
                            name = "ApproveRoleStep",
                            summary = "Approve a role-based step",
                            value = """
                                    {
                                      "submissionId": "69d44796b8b3934d9cb382f7",
                                      "collectionName": "form_template_695_202604",
                                      "actorUserId": 274,
                                      "expectedApprovalInstanceVersion": 1,
                                      "expectedFormSubmissionVersion": 1,
                                      "actorRoleId": "6",
                                      "comment": "approve from role id 6",
                                      "suggestRetest": false
                                    }
                                    """
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Current approval step approved successfully"),
            @ApiResponse(responseCode = "409", description = "Actor does not match the current step, state is invalid, or submitted versions are stale"),
            @ApiResponse(responseCode = "500", description = "Unexpected error approving form submission")
    })
    public ResponseEntity<ResponseResult<String>> approve(@Valid @RequestBody FormSubmissionActionRequest request) {
        try {
            qcFormDataService.approve(request);
            return ResponseResult.of("Form submission approved successfully", ResponseStatus.SUCCESS);
        } catch (IllegalStateException | ApprovalInstanceException e) {
            return ResponseResult.fail(e.getMessage(), ResponseStatus.CONFLICT, e);
        } catch (Exception e) {
            log.error("Error approving form submission", e);
            return ResponseResult.fail("Error approving form submission: " + e.getMessage(), ResponseStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    /**
     * Forwards the current approval step to the next step.
     */
    @PostMapping("/approval/forward")
    @Operation(
            summary = "Forward current approval step",
            description = "Forwards the approval workflow from the current step to the next step. Actor must match the current or a later approval step by actorUserId or actorRoleId."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Form submission forwarded to next approval step"),
            @ApiResponse(responseCode = "409", description = "No next step, actor mismatch, invalid state, or stale submitted versions"),
            @ApiResponse(responseCode = "500", description = "Unexpected error forwarding form submission")
    })
    public ResponseEntity<ResponseResult<String>> forward(@Valid @RequestBody FormSubmissionActionRequest request) {
        try {
            qcFormDataService.forward(request);
            return ResponseResult.of("Form submission forwarded to next approval step", ResponseStatus.SUCCESS);
        } catch (IllegalStateException | ApprovalInstanceException e) {
            return ResponseResult.fail(e.getMessage(), ResponseStatus.CONFLICT, e);
        } catch (Exception e) {
            log.error("Error forwarding form submission", e);
            return ResponseResult.fail("Error forwarding form submission: " + e.getMessage(), ResponseStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    /**
     * Rejects an approval and resets the workflow to the first step.
     */
    @PostMapping("/approval/reject-full-redo")
    @Operation(
            summary = "Reject for full redo",
            description = "Rejects the current approval step, resets all approval steps, sets currentStepSequence to 0, and moves the form submission to pending revision."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Form submission rejected for full redo"),
            @ApiResponse(responseCode = "409", description = "Invalid approval state or stale submitted versions"),
            @ApiResponse(responseCode = "500", description = "Unexpected error rejecting form submission")
    })
    public ResponseEntity<ResponseResult<String>> rejectFullRedo(@Valid @RequestBody FormSubmissionActionRequest request) {
        try {
            qcFormDataService.rejectFullRedo(request);
            return ResponseResult.of("Form submission rejected for full redo", ResponseStatus.SUCCESS);
        } catch (IllegalStateException | ApprovalInstanceException e) {
            return ResponseResult.fail(e.getMessage(), ResponseStatus.CONFLICT, e);
        } catch (Exception e) {
            log.error("Error rejecting form submission", e);
            return ResponseResult.fail("Error rejecting form submission: " + e.getMessage(), ResponseStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    /**
     * Rejects an approval and rolls the workflow back one step.
     */
    @PostMapping("/approval/reject-partial-redo")
    @Operation(
            summary = "Reject for partial redo",
            description = "Rejects the current approval step, resets the current and previous steps, moves currentStepSequence back one step, and moves the form submission to pending revision."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Form submission rejected for partial redo"),
            @ApiResponse(responseCode = "409", description = "Current step cannot be partially reset, state is invalid, or submitted versions are stale"),
            @ApiResponse(responseCode = "500", description = "Unexpected error rejecting form submission")
    })
    public ResponseEntity<ResponseResult<String>> rejectPartialRedo(@Valid @RequestBody FormSubmissionActionRequest request) {
        try {
            qcFormDataService.rejectPartialRedo(request);
            return ResponseResult.of("Form submission rejected for partial redo", ResponseStatus.SUCCESS);
        } catch (IllegalStateException | ApprovalInstanceException e) {
            return ResponseResult.fail(e.getMessage(), ResponseStatus.CONFLICT, e);
        } catch (Exception e) {
            log.error("Error rejecting form submission", e);
            return ResponseResult.fail("Error rejecting form submission: " + e.getMessage(), ResponseStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    /**
     * Rejects an approval and voids the form submission.
     */
    @PostMapping("/approval/reject-discard")
    @Operation(
            summary = "Reject and discard",
            description = "Rejects the current approval step and voids the form submission and approval instance. This is the discard path, not a redo path."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Form submission rejected and voided"),
            @ApiResponse(responseCode = "409", description = "Invalid approval state or stale submitted versions"),
            @ApiResponse(responseCode = "500", description = "Unexpected error rejecting form submission")
    })
    public ResponseEntity<ResponseResult<String>> rejectDiscard(@Valid @RequestBody FormSubmissionActionRequest request) {
        try {
            qcFormDataService.rejectDiscard(request);
            return ResponseResult.of("Form submission rejected and voided", ResponseStatus.SUCCESS);
        } catch (IllegalStateException | ApprovalInstanceException e) {
            return ResponseResult.fail(e.getMessage(), ResponseStatus.CONFLICT, e);
        } catch (Exception e) {
            log.error("Error rejecting form submission", e);
            return ResponseResult.fail("Error rejecting form submission: " + e.getMessage(), ResponseStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    /**
     * Replaces the approval steps for the live approval instance.
     */
    @PostMapping("/approval-flow")
    @Operation(
            summary = "Edit approval flow",
            description = "Replaces the approval steps for the active approval instance and writes an approval action log snapshot for audit history."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Replacement approval flow payload. Each step must provide exactly one approver target: requiredRoleId or requiredUserId.",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApprovalFlowEditRequest.class),
                    examples = @ExampleObject(
                            name = "ReplaceApprovalFlow",
                            summary = "Replace flow with role and user steps",
                            value = """
                                    {
                                      "submissionId": "69d44796b8b3934d9cb382f7",
                                      "collectionName": "form_template_695_202604",
                                      "userId": 274,
                                      "comment": "Updated flow after approver reassignment.",
                                      "steps": [
                                        {
                                          "sequence": 1,
                                          "requiredRoleId": "6"
                                        },
                                        {
                                          "sequence": 2,
                                          "requiredRoleId": "7"
                                        },
                                        {
                                          "sequence": 3,
                                          "requiredUserId": "186"
                                        }
                                      ]
                                    }
                                    """
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Approval flow updated successfully"),
            @ApiResponse(responseCode = "409", description = "Approval flow cannot be edited in the current state"),
            @ApiResponse(responseCode = "500", description = "Unexpected error editing approval flow")
    })
    public ResponseEntity<ResponseResult<Document>> editApprovalFlow(@Valid @RequestBody ApprovalFlowEditRequest request) {
        try {
            return ResponseResult.of(qcFormDataService.editApprovalFlow(request), ResponseStatus.SUCCESS);
        } catch (IllegalStateException | ApprovalInstanceException e) {
            return ResponseResult.fail(e.getMessage(), ResponseStatus.CONFLICT, e);
        } catch (Exception e) {
            log.error("Error editing approval flow", e);
            return ResponseResult.fail("Error editing approval flow: " + e.getMessage(), ResponseStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    /**
     * Returns all form-submission versions in the same version group.
     */
    @GetMapping("/version-history")
    @Operation(
            summary = "Get form submission version history",
            description = "Returns all form submission documents in the same version group for the supplied submission and collection."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Version history returned successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid submissionId or collectionName"),
            @ApiResponse(responseCode = "500", description = "Unexpected error retrieving version history")
    })
    public ResponseEntity<ResponseResult<List<Document>>> getVersionHistory(
            @RequestParam String submissionId,
            @RequestParam String collectionName) {
        try {
            List<Document> versions = qcFormDataService.getVersionHistory(submissionId, collectionName);
            return ResponseResult.of(versions, ResponseStatus.SUCCESS);
        } catch (IllegalArgumentException e) {
            return ResponseResult.fail(e.getMessage(), ResponseStatus.BAD_REQUEST, e);
        } catch (Exception e) {
            log.error("Error retrieving version history", e);
            return ResponseResult.fail("Error retrieving version history: " + e.getMessage(), ResponseStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    /**
     * Returns the approval instance, including voided instances for audit history.
     */
    @GetMapping({"/approval-instance"})
    @Operation(
            summary = "Get approval instance",
            description = "Returns the approval instance for a form submission, including voided approval instances when needed for audit history."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Approval instance returned successfully"),
            @ApiResponse(responseCode = "404", description = "Approval instance was not found"),
            @ApiResponse(responseCode = "500", description = "Unexpected error retrieving approval instance")
    })
    public ResponseEntity<ResponseResult<Object>> getApprovalInstance(
            @RequestParam String submissionId,
            @RequestParam String collectionName) {
        try {
            return ResponseResult.of(qcFormDataService.getApprovalInstance(submissionId, collectionName), ResponseStatus.SUCCESS);
        } catch (ApprovalInstanceException e) {
            return ResponseResult.fail(e.getMessage(), ResponseStatus.NOT_FOUND, e);
        } catch (Exception e) {
            log.error("Error retrieving approval instance", e);
            return ResponseResult.fail("Error retrieving approval instance: " + e.getMessage(), ResponseStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    /**
     * Returns the full approval instance by approval-instance id, including voided instances for audit history.
     */
    @GetMapping("/approval-instance/{approvalInstanceId}")
    @Operation(
            summary = "Get approval instance by id",
            description = "Returns the full approval instance document by approval-instance id, including voided approval instances when needed for audit history."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Approval instance returned successfully"),
            @ApiResponse(responseCode = "404", description = "Approval instance was not found"),
            @ApiResponse(responseCode = "500", description = "Unexpected error retrieving approval instance")
    })
    public ResponseEntity<ResponseResult<Object>> getApprovalInstanceById(@PathVariable String approvalInstanceId) {
        try {
            return ResponseResult.of(qcFormDataService.getApprovalInstanceById(approvalInstanceId), ResponseStatus.SUCCESS);
        } catch (ApprovalInstanceException e) {
            return ResponseResult.fail(e.getMessage(), ResponseStatus.NOT_FOUND, e);
        } catch (Exception e) {
            log.error("Error retrieving approval instance by id", e);
            return ResponseResult.fail("Error retrieving approval instance by id: " + e.getMessage(), ResponseStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    private FormSubmissionState parsePreviousRecordState(String previousRecordState) {
        FormSubmissionState state = FormSubmissionState.fromValue(previousRecordState);
        if (!state.equals(FormSubmissionState.VOID) && !state.equals(FormSubmissionState.ARCHIVED)) {
            throw new IllegalArgumentException("previous_record_state must be either 'void' or 'archived'.");
        }
        return state;
    }

    private boolean isBadEditRequest(IllegalArgumentException e) {
        String message = e.getMessage();
        return message != null && (message.startsWith("previous_record_state") || message.startsWith("Unknown form submission state"));
    }

    private ApprovalInstanceQueryRequest buildApprovalInstanceQueryRequest(
            int page,
            int size,
            String sortField,
            Sort.Direction sortDirection,
            boolean includeFormData,
            Long submitterUserId,
            Long inspectorUserId,
            Long suggestedProductId,
            Long suggestedBatchId,
            Long teamId,
            Long shiftId,
            Long formTemplateId,
            String approvalTemplateId,
            String createdAtStart,
            String createdAtEnd,
            String formSubmissionState
    ) {
        ApprovalInstanceQueryRequest request = new ApprovalInstanceQueryRequest();
        request.setPage(page);
        request.setSize(size);
        request.setSortField(sortField);
        request.setSortDirection(sortDirection);
        request.setIncludeFormData(includeFormData);
        request.setSubmitterUserId(submitterUserId);
        request.setInspectorUserId(inspectorUserId);
        request.setSuggestedProductId(suggestedProductId);
        request.setSuggestedBatchId(suggestedBatchId);
        request.setTeamId(teamId);
        request.setShiftId(shiftId);
        request.setFormTemplateId(formTemplateId);
        request.setApprovalTemplateId(approvalTemplateId);
        request.setCreatedAtStart(createdAtStart);
        request.setCreatedAtEnd(createdAtEnd);
        request.setFormSubmissionState(formSubmissionState);
        return request;
    }
}
