package com.fps.svmes.services;

import com.fps.svmes.dto.requests.ApprovalFlowEditRequest;
import com.fps.svmes.dto.requests.FormSubmissionActionRequest;
import com.fps.svmes.services.support.FormSubmissionMutationGuardContext;

public interface FormSubmissionMutationGuard {
    FormSubmissionMutationGuardContext validateEditMutation(FormSubmissionActionRequest request);

    FormSubmissionMutationGuardContext validateDeleteMutation(FormSubmissionActionRequest request);

    FormSubmissionMutationGuardContext validateReviewMutation(FormSubmissionActionRequest request);

    FormSubmissionMutationGuardContext validateApprovalMutation(FormSubmissionActionRequest request);

    FormSubmissionMutationGuardContext validateWorkflowMutation(ApprovalFlowEditRequest request);
}
