package com.fps.svmes.dto.requests;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(
        description = "Request body for form-submission workflow actions such as enter review, exit review, approve, forward, request correction, and delete-related operations.",
        requiredProperties = {
                "submissionId",
                "collectionName",
                "actorUserId",
                "expectedFormSubmissionVersion"
        }
)
public class FormSubmissionActionRequest {
    @NotBlank(message = "submissionId is required")
    @Schema(
            description = "Required. Mongo ObjectId of the target form submission.",
            example = "69d44796b8b3934d9cb382f7",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String submissionId;

    @NotBlank(message = "collectionName is required")
    @Schema(
            description = "Required. Dynamic Mongo collection containing the target form submission.",
            example = "form_template_695_202604",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String collectionName;

    @NotNull(message = "actorUserId is required")
    @Schema(
            description = "Required. MES user id of the actor performing the workflow action.",
            example = "274",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long actorUserId;

    @NotNull(message = "expectedFormSubmissionVersion is required")
    @Schema(
            description = "Required. Form submission version expected by the client. Used to reject stale actions.",
            example = "1",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Integer expectedFormSubmissionVersion;
    
    @Schema(description = "MES role id of the acting approver. Required for role-based approval steps.", example = "6")
    private String actorRoleId;

    @Schema(description = "Optional action comment stored in the approval action log.", example = "approve from role id 6")
    private String comment;

    @Schema(
            description = "Optional resume step index used only by the request-correction action. Must be between 0 and the current step index when provided.",
            example = "0"
    )
    private Integer resumedStepIndex;

    @Schema(
            description = "Optional retest recommendation captured for approve, forward, and request-correction workflow actions.",
            example = "false"
    )
    private Boolean suggestRetest;

    @JsonProperty("eSignature")
    @Schema(description = "Optional signature or e-signature payload captured with the action.", example = "Justin Li")
    private String eSignature;

    @Schema(description = "Optional. When true, suppress approval action-log writes and approval-step mutation for review entry/exit guard operations.", example = "false")
    private Boolean omitApprovalActionLog;

    @Schema(description = "Active form-submission lock token supplied via request header.", accessMode = Schema.AccessMode.READ_ONLY)
    private String lockToken;
}
