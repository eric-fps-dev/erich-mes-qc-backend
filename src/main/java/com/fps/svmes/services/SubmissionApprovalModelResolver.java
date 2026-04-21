package com.fps.svmes.services;

import com.fps.svmes.enums.form.ApprovalModel;
import com.fps.svmes.enums.form.FormSubmissionState;
import org.bson.Document;

/**
 * Single place that determines approval-model ownership and lifecycle state for a form submission.
 *
 * Rules:
 * - approvalModel field = "v2"  → V2
 * - anything else (missing, "legacy", unknown) → LEGACY
 *
 * Lifecycle state resolution:
 * - V2 and LEGACY: use state field when present
 * - LEGACY only: if state is absent, infer from embedded approval_info
 */
public interface SubmissionApprovalModelResolver {

    ApprovalModel resolveApprovalModel(Document submission);

    FormSubmissionState resolveLifecycleState(Document submission);
}
