package com.fps.svmes.dto.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FormSubmissionLockHeartbeatRequest {
    @NotBlank(message = "submissionId is required")
    private String submissionId;

    @NotBlank(message = "collectionName is required")
    private String collectionName;

    @NotNull(message = "actorUserId is required")
    private Long actorUserId;

    @NotBlank(message = "sessionId is required")
    private String sessionId;

    @NotBlank(message = "lockToken is required")
    private String lockToken;
}
