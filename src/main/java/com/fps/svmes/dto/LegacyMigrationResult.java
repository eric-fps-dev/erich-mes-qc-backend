package com.fps.svmes.dto;

import java.util.List;

public record LegacyMigrationResult(
        int totalProcessed,
        int totalMigrated,
        int totalSkipped,
        int totalFailed,
        List<FailedRecord> failures
) {
    public record FailedRecord(String collectionName, String submissionId, String error) {}
}
