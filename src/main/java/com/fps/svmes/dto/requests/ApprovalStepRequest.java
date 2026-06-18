package com.fps.svmes.dto.requests;

import jakarta.validation.constraints.NotNull;

public record ApprovalStepRequest(
        @NotNull Integer sequence,
        String stepName,
        String stepDescription,
        String requiredRoleId,
        String requiredUserId
) {
}
