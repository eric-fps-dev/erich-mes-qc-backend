package com.fps.svmes.controllers;

import com.fps.shared.dto.responses.ResponseResult;
import com.fps.shared.dto.responses.ResponseStatus;
import com.fps.svmes.dto.PagedResultDTO;
import com.fps.svmes.dto.dtos.qcForm.ApprovalInstanceListItemDTO;
import com.fps.svmes.dto.requests.ApprovalFlowEditRequest;
import com.fps.svmes.dto.requests.ApprovalInstanceQueryRequest;
import com.fps.svmes.dto.requests.FormSubmissionActionRequest;
import com.fps.svmes.exceptions.ApprovalInstanceException;
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
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Approval instance workflow: query, inspect, approve, forward, reject, and flow edits.
 */
@RestController
@Slf4j
@RequestMapping("/qc-form-data")
@RequiredArgsConstructor
@Tag(name = "Approval Instance API", description = "Approval instance workflow actions and queries")
public class ApprovalInstanceController {

    private final QcFormDataService qcFormDataService;

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
            @RequestParam(name = "submission_id", required = false) String submissionId,
            @RequestParam(name = "submitter_user_id", required = false) Long submitterUserId,
            @RequestParam(name = "inspector_user_id", required = false) Long inspectorUserId,
            @RequestParam(name = "suggested_product_id", required = false) Long suggestedProductId,
            @RequestParam(name = "suggested_batch_id", required = false) Long suggestedBatchId,
            @RequestParam(name = "team_id", required = false) Long teamId,
            @RequestParam(name = "shift_id", required = false) Long shiftId,
            @RequestParam(name = "form_template_id", required = false) Long formTemplateId,
            @RequestParam(name = "approval_template_id", required = false) String approvalTemplateId,
            @RequestParam(name = "current_required_user_id", required = false) String currentRequiredUserId,
            @RequestParam(name = "current_required_role_id", required = false) String currentRequiredRoleId,
            @RequestParam(name = "created_at_start", required = false) String createdAtStart,
            @RequestParam(name = "created_at_end", required = false) String createdAtEnd,
            @RequestParam(name = "form_submission_state", required = false) String formSubmissionState,
            @RequestParam(name = "is_alarm_triggered", required = false) Boolean isAlarmTriggered
    ) {
        try {
            ApprovalInstanceQueryRequest request = new ApprovalInstanceQueryRequest();
            request.setPage(page);
            request.setSize(size);
            request.setSortField(sortField);
            request.setSortDirection(sortDirection);
            request.setIncludeFormData(includeFormData);
            request.setSubmissionId(submissionId);
            request.setSubmitterUserId(submitterUserId);
            request.setInspectorUserId(inspectorUserId);
            request.setSuggestedProductId(suggestedProductId);
            request.setSuggestedBatchId(suggestedBatchId);
            request.setTeamId(teamId);
            request.setShiftId(shiftId);
            request.setFormTemplateId(formTemplateId);
            request.setApprovalTemplateId(approvalTemplateId);
            request.setCurrentRequiredUserId(currentRequiredUserId);
            request.setCurrentRequiredRoleId(currentRequiredRoleId);
            request.setCreatedAtStart(createdAtStart);
            request.setCreatedAtEnd(createdAtEnd);
            request.setFormSubmissionState(formSubmissionState);
            request.setIsAlarmTriggered(isAlarmTriggered);
            return ResponseResult.of(qcFormDataService.getApprovalInstances(request), ResponseStatus.SUCCESS);
        } catch (IllegalArgumentException e) {
            return ResponseResult.fail(e.getMessage(), ResponseStatus.BAD_REQUEST, e);
        } catch (Exception e) {
            log.error("Error retrieving approval instances", e);
            return ResponseResult.fail("Error retrieving approval instances: " + e.getMessage(), ResponseStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    @GetMapping("/approval-instance")
    @Operation(
            summary = "Get approval instance by form submission id and collection",
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

    @PostMapping("/approval/request-correction")
    @Operation(
            summary = "Request correction",
            description = "Requests correction on the current approval step, moves the form submission to pending revision, and can optionally resume from an earlier or current step index."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Correction requested successfully"),
            @ApiResponse(responseCode = "409", description = "Invalid approval state or stale submitted versions"),
            @ApiResponse(responseCode = "500", description = "Unexpected error requesting correction")
    })
    public ResponseEntity<ResponseResult<String>> requestCorrection(@Valid @RequestBody FormSubmissionActionRequest request) {
        try {
            qcFormDataService.requestCorrection(request);
            return ResponseResult.of("Correction requested successfully", ResponseStatus.SUCCESS);
        } catch (IllegalStateException | ApprovalInstanceException e) {
            return ResponseResult.fail(e.getMessage(), ResponseStatus.CONFLICT, e);
        } catch (Exception e) {
            log.error("Error requesting correction", e);
            return ResponseResult.fail("Error requesting correction: " + e.getMessage(), ResponseStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

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
}
