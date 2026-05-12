package com.fps.svmes.services.support;

import com.fps.svmes.models.nosql.approval.ApprovalInstance;
import org.bson.Document;

public record ResolvedFormSubmissionTarget(
        String submissionId,
        String collectionName,
        Document submission,
        ApprovalInstance approvalInstance
) {
}
