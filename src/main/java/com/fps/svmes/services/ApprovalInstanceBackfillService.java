package com.fps.svmes.services;

import com.fps.svmes.dto.LegacyMigrationResult;

/**
 * One-shot backfill that creates missing approval-instance documents for existing form submissions.
 * Safe to call multiple times because already-paired submissions are skipped.
 */
public interface ApprovalInstanceBackfillService {
    LegacyMigrationResult backfillMissingApprovalInstances();
}
