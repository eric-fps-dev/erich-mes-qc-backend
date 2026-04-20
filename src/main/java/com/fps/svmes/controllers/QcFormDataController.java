package com.fps.svmes.controllers;

import com.fps.svmes.dto.dtos.alert.ExceededFieldInfoDTO;
import com.fps.svmes.dto.dtos.qcForm.QcApprovalAssignmentDTO;
import com.fps.svmes.dto.dtos.qcForm.QcFormTemplateDTO;
import com.fps.svmes.services.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Date;

@RestController
@Slf4j
@RequestMapping("/qc-form-data")
@RequiredArgsConstructor
@Tag(name = "QC Form Data API", description = "API for QC Form Data")
public class QcFormDataController {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private QcFormTemplateService qcFormTemplateService;

    @Autowired
    private RecipeService recipeService;

    @Autowired
    private AlertRecordService alertRecordService;

    @Autowired
    private ControlLimitEvaluationService controlLimitEvaluationService;

    @Autowired
    private ApprovalInfoGeneratorService approvalInfoGeneratorService;

    @Autowired
    private QcApprovalAssignmentService qcApprovalAssignmentService;

    @Autowired
    private QcSnapshotSubmissionService qcSnapshotSubmissionService;

    @PostMapping("/insert-form/{userId}/{collectionName}")
    public ResponseEntity<?> insertFormData(
            @PathVariable String collectionName,
            @PathVariable Long userId,
            @RequestBody Map<String, Object> formData) {
        try {
            // Extract formTemplate ID from collection name (e.g., form_template_<ID>_<YEARMONTH>)
            String[] parts = collectionName.split("_");
            if (parts.length < 3) {
                return ResponseEntity.status(400).body("Invalid collection name format: " + collectionName);
            }

            Long formTemplateId;
            try {
                formTemplateId = Long.parseLong(parts[2]);
            } catch (NumberFormatException e) {
                return ResponseEntity.status(400).body("Invalid form template ID in collection name: " + collectionName);
            }

            // Check if the collection exists
            if (!mongoTemplate.collectionExists(collectionName)) {
                // Check if the template exists using QcFormTemplateService
                QcFormTemplateDTO template = qcFormTemplateService.getTemplateById(formTemplateId);
                if (template == null) {
                    return ResponseEntity.status(400).body("Error: Template ID " + formTemplateId + " does not exist. Cannot create collection.");
                }

                // Create the collection dynamically
                mongoTemplate.createCollection(collectionName);
                log.info("Created new collection: {}", collectionName);
            }

            // Insert the form data
            Map<String, Object> document = new HashMap<>(formData);
            document.put("created_at", new Date());
            document.put("created_by", userId);

            Map<String, ExceededFieldInfoDTO> exceededInfoMap = controlLimitEvaluationService.evaluateExceededInfo(formTemplateId, formData);
            document.put("exceeded_info", exceededInfoMap);

            String approvalType = qcFormTemplateService.getApprovalTypeByFormId(formTemplateId);
            List<Map<String, Object>> approvalInfo = approvalInfoGeneratorService.generateApprovalInfo(approvalType, userId);
            document.put("approval_info", approvalInfo);

            // 👉 Insert the document into MongoDB
            Document insertedDocument = mongoTemplate.insert(new Document(document), collectionName);

            // ✅ Approval snapshot logic comes AFTER insertedDocument is available
            QcApprovalAssignmentDTO assignmentDTO = new QcApprovalAssignmentDTO();
            assignmentDTO.setSubmissionId(insertedDocument.getObjectId("_id").toString());
            assignmentDTO.setQcFormTemplateId(formTemplateId);
            assignmentDTO.setQcFormTemplateName(qcFormTemplateService.getTemplateById(formTemplateId).getName());
            assignmentDTO.setMongoCollection(collectionName);
            assignmentDTO.setApprovalType(approvalType);

            // Hardcoded flow initial state for now
            if ("flow_1".equals(approvalType)) {
                assignmentDTO.setState("fully_approved");
            } else if ("flow_3".equals(approvalType)) {
                assignmentDTO.setState("pending_supervisor");
            } else {
                assignmentDTO.setState("pending_leader");
            }

            qcApprovalAssignmentService.insertIfNotExists(assignmentDTO);

            // Evaluate control limits and trigger alerts if needed
            controlLimitEvaluationService.evaluateAndTriggerAlerts(formTemplateId, userId, formData, insertedDocument.getObjectId("_id").toString());

            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("object_id", insertedDocument.getObjectId("_id").toString());
            response.put("message", "Form data inserted successfully to " + collectionName);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error inserting form data", e);
            return ResponseEntity.status(500).body("Error inserting form data: " + e.getMessage());
        }
    }

    @PostMapping("/edit-form/{userId}/{collectionName}")
    public ResponseEntity<?> editFormData(
            @PathVariable String collectionName,
            @PathVariable Long userId,
            @RequestParam("parentId") String parentSubmissionId,
            @RequestParam("templateId") Long formTemplateId,
            @RequestBody Map<String, Object> updatedData) {
        try {
            // 1. Construct new document (no approval_info here)
            Map<String, Object> newDoc = new HashMap<>(updatedData);
            // Fetch parent document
            Document parent = mongoTemplate.findById(parentSubmissionId, Document.class, collectionName);
            if (parent == null) {
                return ResponseEntity.status(404).body("Parent record not found.");
            }

            enrichNewDocWithParentData(newDoc, parent);

            // Determine version group and version
            String versionGroupId = parent.getString("version_group_id");
            Integer parentVersion = parent.getInteger("version");

            if (versionGroupId == null || parentVersion == null) {
                // First time versioning is introduced
                versionGroupId = java.util.UUID.randomUUID().toString();
                parentVersion = 1;

                parent.put("version_group_id", versionGroupId);
                parent.put("version", parentVersion);
                mongoTemplate.save(parent, collectionName);  // update parent with version info
            }

            // Set version info for the new version
            newDoc.put("version_group_id", versionGroupId);
            newDoc.put("version", parentVersion + 1);
            newDoc.put("created_at", new Date());
            newDoc.put("created_by", userId);

            Map<String, ExceededFieldInfoDTO> exceededInfoMap =
                    controlLimitEvaluationService.evaluateExceededInfo(formTemplateId, updatedData);
            newDoc.put("exceeded_info", exceededInfoMap);

            Document inserted = mongoTemplate.insert(new Document(newDoc), collectionName);
            String newSubmissionId = inserted.getObjectId("_id").toString();

            // Update the approval assignment to point to the new submission id
            qcApprovalAssignmentService.updateSubmissionId(parentSubmissionId, newSubmissionId);

            // Delete the old snapshot submission to prevent duplication (the new one will be picked up by the next snapshot run)
            qcSnapshotSubmissionService.deleteBySubmissionId(parentSubmissionId);

            // 3. Trigger alerts if needed
            controlLimitEvaluationService.evaluateAndTriggerAlerts(formTemplateId, userId, newDoc, newSubmissionId);

            // 4. Return response
            Map<String, Object> res = new HashMap<>();
            res.put("new_submission_id", newSubmissionId);
            res.put("message", "Edited form data inserted with parent linkage.");
            return ResponseEntity.ok(res);

        } catch (Exception e) {
            log.error("Error editing form data", e);
            return ResponseEntity.status(500).body("Error editing form data: " + e.getMessage());
        }
    }

    /**
     * Enrich new document with data from parent document.
     *
     * For related_* fields (basic fields): Only copy from parent if NOT already present in newDoc.
     * This allows the frontend to pass updated basic field values when editing.
     *
     * For approval_info: Always copy from parent to maintain approval workflow state.
     */
    private void enrichNewDocWithParentData(Map<String, Object> newDoc, Document parent) {
        for (Map.Entry<String, Object> entry : parent.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // Only copy related_* fields from parent if NOT already provided in the request
            if (key.startsWith("related_") && !newDoc.containsKey(key)) {
                newDoc.put(key, value);
            }
        }

        // Always copy approval_info from parent to maintain approval workflow
        if (parent.containsKey("approval_info")) {
            newDoc.put("approval_info", parent.get("approval_info"));
        }
    }


}
