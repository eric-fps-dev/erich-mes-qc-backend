package com.fps.svmes.dto.requests;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Request body for replacing the approval steps of an active approval instance.")
public class ApprovalFlowEditRequest {
    @NotBlank(message = "submissionId is required")
    @Schema(description = "Mongo ObjectId of the form submission whose approval flow will be replaced.", example = "69d44796b8b3934d9cb382f7")
    private String submissionId;

    @NotBlank(message = "collectionName is required")
    @Schema(description = "Dynamic Mongo collection containing the form submission.", example = "form_template_695_202604")
    private String collectionName;

    @Schema(description = "MES user id of the actor editing the approval flow.", example = "274")
    private Long userId;

    @NotNull(message = "expectedFormSubmissionVersion is required")
    @Schema(description = "Form submission version expected by the client. Used to reject stale mutations.", example = "1")
    private Integer expectedFormSubmissionVersion;

    @Schema(description = "Optional audit comment explaining why the approval flow is being changed.", example = "Updated approvers after role reassignment.")
    private String comment;

    @NotNull(message = "steps is required; use an empty array only if intentionally removing all approval steps")
    @ArraySchema(
            arraySchema = @Schema(
                    description = "Replacement approval steps in execution order. Each step must provide exactly one approver reference: either requiredRoleId or requiredUserId. Use an empty array only when intentionally removing all approval steps."
            ),
            schema = @Schema(implementation = ApprovalStepRequest.class)
    )
    private List<@Valid ApprovalStepRequest> steps;

    @Schema(description = "Active form-submission lock token supplied via request header.", accessMode = Schema.AccessMode.READ_ONLY)
    private String lockToken;

    @Schema(description = "Active form-submission lock session id supplied via request header.", accessMode = Schema.AccessMode.READ_ONLY)
    private String lockSessionId;
}
