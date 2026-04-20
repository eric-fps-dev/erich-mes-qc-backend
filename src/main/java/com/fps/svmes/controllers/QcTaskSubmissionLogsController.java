package com.fps.svmes.controllers;

import com.fps.svmes.dto.dtos.qcForm.QcTaskSubmissionLogsDTO;
import com.fps.svmes.dto.responses.ResponseResult;
import com.fps.svmes.services.QcTaskSubmissionLogsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.bson.Document;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@Slf4j
@RequestMapping("/qc-task-submission-logs")
@RequiredArgsConstructor
@Tag(name = "QC Task Submission Logs API", description = "API for managing QC task submission logs")
public class QcTaskSubmissionLogsController {

    private final QcTaskSubmissionLogsService service;
    private static final Logger logger = LoggerFactory.getLogger(QcTaskSubmissionLogsController.class);
    private final QcTaskSubmissionLogsService qcTaskSubmissionLogsService;

    @PostMapping
    @Operation(summary = "Create a new QC task submission log", description = "Creates a new QC task submission log.")
    public ResponseResult<QcTaskSubmissionLogsDTO> createLog(@RequestBody QcTaskSubmissionLogsDTO dto) {
        try {
            QcTaskSubmissionLogsDTO createdLog = service.insertLog(dto);
            logger.info("Log created: {}", createdLog);
            return ResponseResult.success(createdLog);
        } catch (Exception e) {
            logger.error("Error creating log", e);
            return ResponseResult.fail("Error creating log", e);
        }
    }

    @GetMapping
    @Operation(summary = "Get all logs by created_by and dispatched_task_id")
    public ResponseEntity<?> getAllByCreatedByAndDispatchedTaskId(
            @RequestParam Integer createdBy,
            @RequestParam Long dispatchedTaskId) {
        try {
            List<QcTaskSubmissionLogsDTO> logs = service.getAllByCreatedByAndTaskId(createdBy, dispatchedTaskId);
            return ResponseEntity.ok(logs);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error retrieving logs: " + e.getMessage());
        }
    }

    @GetMapping("/document")
    @Operation(summary = "Get Mongo document by submission ID")
    public ResponseEntity<?> getDocument(
            @RequestParam String submissionId,
            @RequestParam Long qcFormTemplateId,
            @RequestParam Integer createdBy,
            @RequestParam Optional<String> inputCollectionName
            ) {
        try {
            Document document = qcTaskSubmissionLogsService.getDocumentBySubmissionId(submissionId, qcFormTemplateId, createdBy, inputCollectionName);
            if (document == null) {
                return ResponseEntity.status(404).body("Document not found for submissionId: " + submissionId);
            }
            return ResponseEntity.ok(document);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error retrieving document: " + e.getMessage());
        }
    }

    // documents for user that accept the qcFormTemplateId and createdBy and return all the documents that match the criteria
    @GetMapping("/documents_for_user")
    @Operation(summary = "Get all documents by qcFormTemplateId and createdBy")
    public ResponseEntity<?> getDocuments(
            @RequestParam Long qcFormTemplateId,
            @RequestParam Integer createdBy) {
        try {
            List<Document> documents = qcTaskSubmissionLogsService.getDocumentsByQcFormTemplateIdAndCreatedBy(qcFormTemplateId, createdBy);
            return ResponseEntity.ok(documents);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error retrieving documents: " + e.getMessage());
        }
    }

    // write a controller called excel_documents_for_user to export all the list of documents to excel
    @GetMapping("/excel_documents_for_user")
    @Operation(summary = "Export all documents by qcFormTemplateId and createdBy to Excel")
    public ResponseEntity<?> exportDocumentsToExcel(
            @RequestParam Long qcFormTemplateId,
            @RequestParam Integer createdBy) {
        try {
            List<Document> documents = qcTaskSubmissionLogsService.getDocumentsByQcFormTemplateIdAndCreatedBy(qcFormTemplateId, createdBy);
            byte[] excelBytes = qcTaskSubmissionLogsService.exportDocumentsToExcel(documents);
            // the filename should be the current datetime
            String fileName = "documents_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".xlsx";
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=" + fileName)
                    .body(excelBytes);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error exporting documents to Excel: " + e.getMessage());
        }
    }

    // use itext to export result to pdf according to the /document endpoint and make that /document_pdf
    @GetMapping("/document_pdf")
    @Operation(summary = "Export Mongo document to PDF")
    public ResponseEntity<?> getDocumentPdf(
            @RequestParam String submissionId,
            @RequestParam Long qcFormTemplateId,
            @RequestParam Integer createdBy,
            @RequestParam Optional<String> inputCollectionName

    ) {
        try {
            Document document = qcTaskSubmissionLogsService.getDocumentBySubmissionId(submissionId, qcFormTemplateId, createdBy, inputCollectionName);
            if (document == null) {
                return ResponseEntity.status(404).body("Document not found for submissionId: " + submissionId);
            }
            // export the document to pdf
            byte[] pdfBytes = qcTaskSubmissionLogsService.exportDocumentToPdf(document);
            // the filename should be the current datetime
            String fileName = "document_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".pdf";
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=" + fileName)
                    .body(pdfBytes);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error exporting document to PDF: " + e.getMessage());
        }
    }

    // Delete the submission log by submissionId and createdAt
    @DeleteMapping("/{submissionId}")
    @Operation(summary = "Delete a submission log by submissionId and createdAt")
    public ResponseEntity<?> deleteSubmissionLog(
            @PathVariable String submissionId,
            @RequestParam Long qcFormTemplateId,
            @RequestParam String createdAt) {
        try {
            // determine the form_template_{id}_{YYYYMM} collection to look for according to createdAt example createdAt string 2025-02-05 19:31:58
            String collectionName = "form_template_" + qcFormTemplateId + "_" + createdAt.substring(0, 7).replace("-", "");
            qcTaskSubmissionLogsService.deleteSubmissionLog(submissionId, collectionName);
            return ResponseEntity.ok("Submission log deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error deleting submission log: " + e.getMessage());
        }
    }

    @GetMapping("/raw_document")
    @Operation(summary = "Get raw MongoDB document by submissionId and createdAt (raw, unformatted)")
    public ResponseEntity<?> getRawMongoDocument(
            @RequestParam String submissionId,
            @RequestParam Long qcFormTemplateId,
            @RequestParam String createdAt, // expected: 2025-05-01 16:50:52.427519100
            @RequestParam(required = false) String collectionName
    ) {
        try {
            String targetCollectionName;
            if (collectionName != null && !collectionName.isEmpty()) {
                targetCollectionName = collectionName;
            } else {
                // Format: "form_template_{templateId}_{yyyyMM}"
                String collectionSuffix = createdAt.substring(0, 7).replace("-", "");
                targetCollectionName = "form_template_" + qcFormTemplateId + "_" + collectionSuffix;
            }

            Document rawDoc = qcTaskSubmissionLogsService.getRawDocumentBySubmissionId(submissionId, targetCollectionName);

            if (rawDoc == null) {
                return ResponseEntity.status(404).body("Document not found in collection: " + targetCollectionName);
            }

            return ResponseEntity.ok(rawDoc);
        } catch (Exception e) {
            logger.error("Failed to get raw MongoDB document", e);
            return ResponseEntity.status(500).body("Error retrieving raw document: " + e.getMessage());
        }
    }

    @GetMapping("/form-template-fields")
    @Operation(summary = "Get all field key-label-optionItems for a given qcFormTemplateId")
    public ResponseEntity<?> getFormTemplateFieldKeyLabelPairs(@RequestParam Long qcFormTemplateId) {
        try {
            List<Map<String, Object>> result = qcTaskSubmissionLogsService.getFormTemplateFieldList(qcFormTemplateId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Failed to get form template field list", e);
            return ResponseEntity.status(500).body("Error retrieving field list: " + e.getMessage());
        }
    }


}
