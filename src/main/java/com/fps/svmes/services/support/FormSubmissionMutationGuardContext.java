package com.fps.svmes.services.support;

import com.fps.svmes.enums.form.FormSubmissionState;

public record FormSubmissionMutationGuardContext(
        ResolvedFormSubmissionTarget target,
        FormSubmissionState state
) {
}
