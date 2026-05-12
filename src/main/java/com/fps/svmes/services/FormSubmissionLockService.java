package com.fps.svmes.services;

import com.fps.svmes.dto.requests.FormSubmissionLockAcquireRequest;
import com.fps.svmes.dto.requests.FormSubmissionLockHeartbeatRequest;
import com.fps.svmes.dto.requests.FormSubmissionLockReleaseRequest;
import com.fps.svmes.dto.requests.FormSubmissionLockStatusRequest;
import com.fps.svmes.dto.responses.FormSubmissionLockResponse;

public interface FormSubmissionLockService {
    FormSubmissionLockResponse acquireLock(FormSubmissionLockAcquireRequest request);

    FormSubmissionLockResponse renewLock(FormSubmissionLockHeartbeatRequest request);

    FormSubmissionLockResponse releaseLock(FormSubmissionLockReleaseRequest request);

    FormSubmissionLockResponse getLockStatus(FormSubmissionLockStatusRequest request);

    void validateActiveLockOwnership(String submissionId, String collectionName, Long actorUserId, String sessionId, String lockToken);
}
