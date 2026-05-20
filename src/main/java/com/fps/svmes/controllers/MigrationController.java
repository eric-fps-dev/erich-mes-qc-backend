package com.fps.svmes.controllers;

import com.fps.shared.dto.responses.ResponseResult;
import com.fps.shared.dto.responses.ResponseStatus;
import com.fps.svmes.dto.LegacyMigrationResult;
import com.fps.svmes.services.ApprovalInstanceBackfillService;
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

    private final ApprovalInstanceBackfillService approvalInstanceBackfillService;

    @PostMapping("/approval-instances/backfill-missing")
    @Operation(
            summary = "Backfill missing approval instances",
            description = "Creates ApprovalInstance documents for existing form submissions that do not already "
                    + "have one paired. Approval steps are sourced from the form template approvalTemplateId "
                    + "using the normal approval-instance creation flow. Idempotent - already-paired submissions "
                    + "are counted as skipped."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Backfill completed; check totalFailed and failures for partial errors"),
            @ApiResponse(responseCode = "500", description = "Unexpected error during approval-instance backfill")
    })
    public ResponseEntity<ResponseResult<LegacyMigrationResult>> backfillMissingApprovalInstances() {
        try {
            LegacyMigrationResult result = approvalInstanceBackfillService.backfillMissingApprovalInstances();
            return ResponseResult.of(result, ResponseStatus.SUCCESS);
        } catch (Exception e) {
            log.error("Approval instance backfill failed", e);
            return ResponseResult.fail("Approval instance backfill failed: " + e.getMessage(), ResponseStatus.INTERNAL_SERVER_ERROR, e);
        }
    }
}
