package com.fps.svmes.controllers;

import com.fps.shared.dto.responses.ResponseResult;
import com.fps.shared.dto.responses.ResponseStatus;
import com.fps.svmes.dto.requests.FormSubmissionActionRequest;
import com.fps.svmes.enums.form.FormSubmissionState;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Form submission lifecycle: create, edit, void, state transitions, and version history.
 */
@RestController
@Slf4j
@RequestMapping("/qc-form-data")
@RequiredArgsConstructor
@Tag(name = "Form Submission API", description = "Form submission lifecycle and state transitions")
public class FormSubmissionController {

    private final QcFormDataService qcFormDataService;

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
}
