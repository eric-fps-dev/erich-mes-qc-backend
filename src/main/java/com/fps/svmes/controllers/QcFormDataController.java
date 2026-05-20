package com.fps.svmes.controllers;

import com.fps.shared.dto.responses.ResponseResult;
import com.fps.shared.dto.responses.ResponseStatus;
import com.fps.svmes.dto.requests.FormSubmissionActionRequest;
import com.fps.svmes.exceptions.ApprovalInstanceException;
import com.fps.svmes.services.QcFormDataService;
import com.fps.svmes.services.QcTaskSubmissionLogsService;
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
 * Form submission lifecycle: create, edit, delete, state transitions, and version history.
 */
@RestController
@Slf4j
@RequestMapping("/qc-form-data")
@RequiredArgsConstructor
@Tag(name = "QC Form Data API", description = "Form submission lifecycle and state transitions")
public class QcFormDataController {

    private final QcFormDataService qcFormDataService;
    private final QcTaskSubmissionLogsService qcTaskSubmissionLogsService;
    @PostMapping("/insert-form/{userId}/{collectionName}")
    @Operation(
            summary = "Create form submission",
            description = "Creates a form submission in the dynamic Mongo collection, initializes version to 1, and creates the corresponding approval instance. "
                    + "New submissions start in submitted state. "
                    + "When submitForApproval=true and the form has approval steps, the form is also moved into under-review in the same call."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Form submission created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid collection name, form template, or form data"),
            @ApiResponse(responseCode = "500", description = "Unexpected error inserting form data")
    })
    public ResponseEntity<ResponseResult<Map<String, Object>>> insertFormData(
            @PathVariable String collectionName,
            @PathVariable Long userId,
            @RequestParam(name = "submitForApproval", defaultValue = "false") boolean submitForApproval,
            @RequestBody Map<String, Object> formData) {
        try {
            return ResponseResult.of(qcFormDataService.insertFormData(collectionName, userId, formData, submitForApproval), ResponseStatus.SUCCESS);
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
            description = "Creates a new version of an existing submitted, archived, or pending revision form submission while preserving version history. "
                    + "Requires parentId and templateId. Archived prior versions remain archived; other prior versions are moved back to submitted."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Form submission version created successfully"),
            @ApiResponse(responseCode = "404", description = "Original form submission was not found"),
            @ApiResponse(responseCode = "409", description = "Form submission cannot be edited in its current state"),
            @ApiResponse(responseCode = "500", description = "Unexpected error editing form data")
    })
    public ResponseEntity<ResponseResult<Map<String, Object>>> editFormData(
            @PathVariable String collectionName,
            @PathVariable Long userId,
            @RequestParam("parentId") String parentSubmissionId,
            @RequestParam("templateId") Long formTemplateId,
            @RequestParam("expectedFormSubmissionVersion") Integer expectedFormSubmissionVersion,
            @RequestBody Map<String, Object> updatedData) {
        try {
            return ResponseResult.of(
                    qcFormDataService.editFormData(
                            collectionName,
                            userId,
                            parentSubmissionId,
                            formTemplateId,
                            expectedFormSubmissionVersion,
                            updatedData
                    ),
                    ResponseStatus.SUCCESS
            );
        } catch (IllegalArgumentException e) {
            ResponseStatus status = ResponseStatus.NOT_FOUND;
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
            summary = "Delete form submission",
            description = "Hard deletes a form submission and related approval-instance records."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Form submission deleted successfully"),
            @ApiResponse(responseCode = "500", description = "Unexpected error deleting form submission")
    })
    public ResponseEntity<ResponseResult<String>> deleteFormSubmission(@Valid @RequestBody FormSubmissionActionRequest request) {
        try {
            qcTaskSubmissionLogsService.deleteSubmissionLog(request);
            return ResponseResult.of("Form submission deleted successfully", ResponseStatus.SUCCESS);
        } catch (IllegalStateException | ApprovalInstanceException e) {
            return ResponseResult.fail(e.getMessage(), ResponseStatus.CONFLICT, e);
        } catch (Exception e) {
            log.error("Error deleting form submission", e);
            return ResponseResult.fail("Error deleting form submission: " + e.getMessage(), ResponseStatus.INTERNAL_SERVER_ERROR, e);
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

}
