package com.fps.svmes.dto;

import java.util.List;

public record ApprovalInstanceSnapshotBackfillResult(
        int totalProcessed,
        int totalUpdated,
        int totalFailed,
        List<FailedRecord> failures
) {
    public record FailedRecord(String approvalInstanceId, String collectionName, String submissionId, String error) {}
}
