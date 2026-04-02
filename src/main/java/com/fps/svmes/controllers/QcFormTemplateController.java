// Controller
package com.fps.svmes.controllers;

import com.fps.svmes.dto.dtos.qcForm.QcFormTemplateDTO;
import com.fps.svmes.dto.dtos.qcForm.QcFormTemplateEditLogDTO;
import com.fps.svmes.dto.requests.TemplateFormRequest;
import com.fps.svmes.dto.responses.ResponseResult;
import com.fps.svmes.models.sql.qcForm.QcFormTemplateEditLog;
import com.fps.svmes.repositories.jpaRepo.qcForm.QcFormTemplateEditLogRepository;
import com.fps.svmes.models.nosql.FormNode;
import com.fps.svmes.services.FormNodeService;
import com.fps.svmes.services.MongoService;
import com.fps.svmes.services.QcFormTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@Slf4j
@RequestMapping("/qc-form-templates")
@RequiredArgsConstructor
@Tag(name = "QC Form Templates API", description = "API for QC Form Templates")
public class QcFormTemplateController {

    private final QcFormTemplateService service;

    private final FormNodeService formNodeService;

    private final MongoService mongoService;
    private final QcFormTemplateEditLogRepository editLogRepository;
    private static final Logger logger = LoggerFactory.getLogger(QcFormTemplateController.class);

    @GetMapping
    @Operation(summary = "Get all active QC form templates", description = "Returns a list of all active QC form templates.")
    public ResponseResult<List<QcFormTemplateDTO>> getAllActiveTemplates() {
        try {
            List<QcFormTemplateDTO> templates = service.getAllActiveTemplates();
            logger.info("Retrieved {} active templates", templates.size());
            return ResponseResult.success(templates);
        } catch (Exception e) {
            logger.error("Error retrieving active templates", e);
            return ResponseResult.fail("Error retrieving active templates", e);
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a QC form template by ID", description = "Fetches the QC form template by its ID.")
    public ResponseResult<QcFormTemplateDTO> getTemplateById(@PathVariable Long id) {
        try {
            QcFormTemplateDTO template = service.getTemplateById(id);
            logger.info("Template retrieved with ID: {}", id);
            return ResponseResult.success(template);
        } catch (Exception e) {
            logger.error("Error retrieving template with ID: {}", id, e);
            return ResponseResult.fail("Error retrieving template", e);
        }
    }

    @PostMapping
    @Operation(summary = "Create a new QC form template", description = "Creates a new QC form template.")
    public ResponseResult<QcFormTemplateDTO> createTemplate(@RequestBody QcFormTemplateDTO dto) {
        try {
            QcFormTemplateDTO createdTemplate = service.createTemplate(dto);
            logger.info("Template created: {}", createdTemplate);
            return ResponseResult.success(createdTemplate);
        } catch (Exception e) {
            logger.error("Error creating template", e);
            return ResponseResult.fail("Error creating template", e);
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a QC form template", description = "Updates an existing QC form template.")
    public ResponseResult<QcFormTemplateDTO> updateTemplate(@PathVariable Long id, @RequestBody QcFormTemplateDTO dto) {
        try {
            QcFormTemplateDTO updatedTemplate = service.updateTemplate(id, dto);
            logger.info("Template updated with ID: {}", id);
            return ResponseResult.success(updatedTemplate);
        } catch (Exception e) {
            logger.error("Error updating template with ID: {}", id, e);
            return ResponseResult.fail("Error updating template", e);
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a QC form template", description = "Marks a QC form template as inactive by setting its status to 0.")
    public ResponseResult<Void> deleteTemplate(@PathVariable Long id) {
        try {
            service.deleteTemplate(id);
            logger.info("Template deleted with ID: {}", id);
            return ResponseResult.success(null);
        } catch (Exception e) {
            logger.error("Error deleting template with ID: {}", id, e);
            return ResponseResult.fail("Error deleting template", e);
        }
    }

    @PostMapping("/create-with-nodes")
    @Operation(summary = "Create a QC form template with nodes and collections",
            description = "Creates a QC form template, adds nodes under multiple selected folders, and initializes MongoDB collections.")
    public ResponseResult<Void> createTemplateWithNodes(@RequestBody TemplateFormRequest request) {
        try {
            // Step 1: Create Template
            QcFormTemplateDTO template = service.createTemplate(request.getForm());

            // Step 2: Create Nodes for all parent folders
            for (String parentFolderId : request.getParentFolderIds()) {
                // Step 2.1: Create FormNode payload
                FormNode childNode = new FormNode();
                childNode.setLabel(template.getName()); // Use the template name as label
                childNode.setNodeType("document"); // Assuming "document" type for form nodes
                childNode.setQcFormTemplateId(template.getId()); // Associate with the template ID

                // Step 2.2: Add child node under the parent folder
                Optional<FormNode> node = formNodeService.addChildNode(parentFolderId, childNode);
                if (node.isEmpty()) {
                    // Return a bad request error if node creation fails
                    return ResponseResult.failBadRequest("Failed to create a node for folder ID: " + parentFolderId, null);
                }
            }

            // Step 3: Initialize MongoDB Collection
            String collectionName = "form_template_" + template.getId() + "_" + getCurrentYearMonth();
            mongoService.createCollection(collectionName);

            // Step 4: Store key-label mappings into MongoDB
            service.extractAndStoreKeyLabelPairs(template);

            // Step 5: Create the control limit setting in the recipe library
            service.createControlLimitSetting(template);

            // Step 6: Write creation audit log
            QcFormTemplateEditLog creationLog = new QcFormTemplateEditLog();
            creationLog.setTemplateId(template.getId());
            creationLog.setEditedBy(request.getForm().getCreatedBy() != null
                    ? request.getForm().getCreatedBy().longValue() : 0L);
            creationLog.setEditedAt(OffsetDateTime.now());
            creationLog.setChangeSummary("Template created");
            editLogRepository.save(creationLog);

            logger.info("Template, nodes, and collection created successfully for multiple parent folders!");
            return ResponseResult.success(null);
        } catch (Exception e) {
            logger.error("Error creating template, nodes, or collection", e);
            return ResponseResult.fail("Failed to create template with nodes and collection", e);
        }
    }

    @PutMapping("/{id}/full-update")
    @Operation(summary = "Full update with node sync and audit log",
            description = "Updates a QC form template, syncs FormNode labels, and writes an audit log entry.")
    public ResponseEntity<?> fullUpdateTemplate(
            @PathVariable Long id,
            @RequestBody QcFormTemplateDTO dto) {
        try {
            QcFormTemplateDTO result = service.updateTemplateWithNodeSync(id, dto);
            return ResponseEntity.ok(ResponseResult.success(result));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error updating template", e);
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/edit-log")
    @Operation(summary = "Get edit log for a template",
            description = "Returns the audit log entries for a QC form template.")
    public ResponseEntity<?> getEditLog(@PathVariable Long id) {
        try {
            List<QcFormTemplateEditLogDTO> logs = service.getEditLog(id);
            return ResponseEntity.ok(ResponseResult.success(logs));
        } catch (Exception e) {
            log.error("Error fetching edit log", e);
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/fields")
    @Operation(summary = "Get all fields for a template including soft-deleted ones")
    public ResponseEntity<?> getTemplateFields(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ResponseResult.success(service.getTemplateFields(id)));
        } catch (Exception e) {
            log.error("Error fetching template fields", e);
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    private String getCurrentYearMonth() {
        LocalDate now = LocalDate.now();
        return now.getYear() + String.format("%02d", now.getMonthValue());
    }

}
