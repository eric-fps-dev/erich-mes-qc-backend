package com.fps.svmes.dto.responses;

import com.fps.svmes.enums.form.FormSubmissionLockPurpose;
import com.fps.svmes.enums.form.FormSubmissionLockStatus;
import lombok.Data;

import java.util.Date;

@Data
public class FormSubmissionLockResponse {
    private String submissionId;
    private String collectionName;
    private FormSubmissionLockStatus status;
    private String lockToken;
    private String sessionId;
    private FormSubmissionLockPurpose lockPurpose;
    private Date expiresAt;
    private Long lockedByUserId;
    private boolean ownedByCurrentCaller;
    private boolean readOnly;
}
