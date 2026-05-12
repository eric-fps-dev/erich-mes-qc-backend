package com.fps.svmes.services;

import com.fps.svmes.enums.form.FormSubmissionLockPurpose;
import com.fps.svmes.services.support.ResolvedFormSubmissionTarget;

import java.util.List;

public interface FormSubmissionMutationAccessService {
    ResolvedFormSubmissionTarget resolveTarget(String submissionId, String requestedCollectionName, FormSubmissionLockPurpose purpose);

    boolean canAccess(ResolvedFormSubmissionTarget target,
                      Long actorUserId,
                      String actorRoleId,
                      List<String> actorRoleIds,
                      FormSubmissionLockPurpose purpose);
}
