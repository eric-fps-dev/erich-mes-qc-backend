package com.fps.svmes.dto.requests;

import com.fps.svmes.enums.form.FormSubmissionLockPurpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class FormSubmissionLockAcquireRequest {
    @NotBlank(message = "submissionId is required")
    private String submissionId;

    @NotBlank(message = "collectionName is required")
    private String collectionName;

    @NotNull(message = "actorUserId is required")
    private Long actorUserId;

    private List<String> actorRoleIds;

    private String sessionId;

    @NotNull(message = "lockPurpose is required")
    private FormSubmissionLockPurpose lockPurpose;

    private Boolean takeOverExistingLock;
}
