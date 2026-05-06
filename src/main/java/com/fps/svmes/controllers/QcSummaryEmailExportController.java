package com.fps.svmes.controllers;

import com.fps.svmes.dto.requests.QcSummaryEmailExportRequest;
import com.fps.svmes.dto.responses.QcSummaryEmailExportResponse;
import com.fps.svmes.services.QcSummaryEmailExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "QC Summary Email Export API", description = "API for exporting QC summary artifacts and sending them by email")
public class QcSummaryEmailExportController {

    private final QcSummaryEmailExportService qcSummaryEmailExportService;

    @PostMapping("/export-send-email")
    @Operation(summary = "Generate QC summary exports and send them by email")
    public ResponseEntity<QcSummaryEmailExportResponse> exportSendEmail(
            @Valid @RequestBody QcSummaryEmailExportRequest request,
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage) {
        return ResponseEntity.ok(qcSummaryEmailExportService.exportAndSendEmail(request, acceptLanguage));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleUnreadableRequest(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest().body(Map.of("message", "Malformed request payload"));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException exception) {
        log.error("Failed to export QC summary by email", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Failed to export QC summary by email"));
    }
}
