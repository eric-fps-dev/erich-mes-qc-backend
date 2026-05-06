package com.fps.svmes.controllers;

import com.fps.shared.dto.responses.ResponseResult;
import com.fps.shared.dto.responses.ResponseStatus;
import com.fps.svmes.dto.ApprovalInstanceSnapshotBackfillResult;
import com.fps.svmes.dto.LegacyMigrationResult;
import com.fps.svmes.services.ApprovalInstanceSnapshotBackfillService;
import com.fps.svmes.services.LegacyApprovalMigrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequestMapping("/admin/migration")
@RequiredArgsConstructor
@Tag(name = "Migration API", description = "One-shot data migration endpoints")
public class MigrationController {

    private final LegacyApprovalMigrationService legacyApprovalMigrationService;
    private final ApprovalInstanceSnapshotBackfillService approvalInstanceSnapshotBackfillService;

    @PostMapping("/legacy-approval")
    @Operation(
            summary = "Migrate legacy approval data to V2",
            description = "Backfills ApprovalInstance documents for all LEGACY form submissions that lack one, "
                    + "then stamps approvalModel='v2' and the inferred state on each migrated form document. "
                    + "Idempotent — already-migrated submissions are counted as skipped and not modified. "
                    + "flow_1 (auto-approved) submissions get an ApprovalInstance with no steps and state=archived. "
                    + "Role IDs and template IDs are read from qc.legacy-migration.* properties."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Migration completed; check totalFailed and failures for partial errors"),
            @ApiResponse(responseCode = "500", description = "Unexpected error during migration")
    })
    public ResponseEntity<ResponseResult<LegacyMigrationResult>> migrateLegacyApproval() {
        try {
            LegacyMigrationResult result = legacyApprovalMigrationService.migrate();
            return ResponseResult.of(result, ResponseStatus.SUCCESS);
        } catch (Exception e) {
            log.error("Legacy approval migration failed", e);
            return ResponseResult.fail("Migration failed: " + e.getMessage(), ResponseStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    @PostMapping("/approval-instance-snapshots")
    @Operation(
            summary = "Backfill approval instance filter snapshots",
            description = "Refreshes filterSnapshot for all existing ApprovalInstance documents from their current form submission documents. "
                    + "Use this after adding new snapshot display fields such as related entity names."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Backfill completed; check totalFailed and failures for partial errors"),
            @ApiResponse(responseCode = "500", description = "Unexpected error during snapshot backfill")
    })
    public ResponseEntity<ResponseResult<ApprovalInstanceSnapshotBackfillResult>> backfillApprovalInstanceSnapshots() {
        try {
            ApprovalInstanceSnapshotBackfillResult result = approvalInstanceSnapshotBackfillService.backfillAll();
            return ResponseResult.of(result, ResponseStatus.SUCCESS);
        } catch (Exception e) {
            log.error("Approval instance snapshot backfill failed", e);
            return ResponseResult.fail("Snapshot backfill failed: " + e.getMessage(), ResponseStatus.INTERNAL_SERVER_ERROR, e);
        }
    }
}
