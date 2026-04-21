package com.fps.svmes.services;

import com.fps.svmes.dto.LegacyMigrationResult;

/**
 * One-shot migration that backfills ApprovalInstance documents for LEGACY form submissions.
 * Safe to call multiple times — already-migrated submissions are skipped.
 */
public interface LegacyApprovalMigrationService {
    LegacyMigrationResult migrate();
}
