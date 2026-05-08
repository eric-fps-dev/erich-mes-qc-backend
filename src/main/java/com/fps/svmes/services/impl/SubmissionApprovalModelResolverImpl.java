package com.fps.svmes.services.impl;

import com.fps.svmes.enums.form.ApprovalModel;
import com.fps.svmes.enums.form.FormSubmissionState;
import com.fps.svmes.services.SubmissionApprovalModelResolver;
import org.bson.Document;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SubmissionApprovalModelResolverImpl implements SubmissionApprovalModelResolver {

    @Override
    public ApprovalModel resolveApprovalModel(Document submission) {
        String value = submission.getString(ApprovalModel.DOCUMENT_FIELD);
        return ApprovalModel.V2_VALUE.equalsIgnoreCase(value) ? ApprovalModel.V2 : ApprovalModel.LEGACY;
    }

    @Override
    public FormSubmissionState resolveLifecycleState(Document submission) {
        String state = submission.getString("state");
        if (state != null && !state.isBlank()) {
            return FormSubmissionState.fromValue(state);
        }
        // Legacy inference: derive state from embedded approval_info when state field is absent
        if (legacyApprovalComplete(submission)) {
            return FormSubmissionState.SUBMITTED;
        }
        if (legacyApprovalInProgress(submission)) {
            return FormSubmissionState.UNDER_REVIEW;
        }
        return FormSubmissionState.SUBMITTED;
    }

    private boolean legacyApprovalComplete(Document submission) {
        List<Document> steps = legacyApprovalInfo(submission);
        return !steps.isEmpty() && steps.stream().allMatch(s -> "completed".equals(s.getString("status")));
    }

    private boolean legacyApprovalInProgress(Document submission) {
        return legacyApprovalInfo(submission).stream().anyMatch(s -> "pending".equals(s.getString("status")));
    }

    @SuppressWarnings("unchecked")
    private List<Document> legacyApprovalInfo(Document submission) {
        Object raw = submission.get("approval_info");
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .filter(Document.class::isInstance)
                .map(Document.class::cast)
                .toList();
    }
}
