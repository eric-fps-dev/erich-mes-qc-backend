package com.fps.svmes.services.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fps.svmes.dto.dtos.qcForm.QcFormTemplateDTO;
import com.fps.svmes.dto.dtos.qcForm.QcFormTemplateEditLogDTO;
import com.fps.svmes.models.sql.qcForm.QcFormTemplate;
import com.fps.svmes.models.sql.qcForm.QcFormTemplateEditLog;
import com.fps.svmes.repositories.jpaRepo.qcForm.QcFormTemplateEditLogRepository;
import com.fps.svmes.repositories.jpaRepo.qcForm.QcFormTemplateRepository;
import com.fps.svmes.repositories.jpaRepo.user.UserRepository;
import com.fps.svmes.services.FormNodeService;
import com.fps.svmes.services.MongoService;
import com.fps.svmes.services.QcFormTemplateService;
import com.mongodb.client.MongoClient;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.bson.Document;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class QcFormTemplateServiceImpl implements QcFormTemplateService {

    @Autowired
    private QcFormTemplateRepository qcFormTemplateRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private MongoService mongoService;

    @Autowired
    private MongoClient mongoClient;

    @Autowired
    private QcFormTemplateEditLogRepository editLogRepository;

    @Autowired
    private FormNodeService formNodeService;

    @Autowired
    private UserRepository userRepository;

    @Override
    public List<QcFormTemplateDTO> getAllActiveTemplates() {
        return qcFormTemplateRepository.findAllByStatus(1).stream()
                .map(template -> modelMapper.map(template, QcFormTemplateDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public QcFormTemplateDTO getTemplateById(Long id) {
        QcFormTemplate template = qcFormTemplateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found"));
        QcFormTemplateDTO dto = modelMapper.map(template, QcFormTemplateDTO.class);
        long editCount = editLogRepository.countByTemplateId(id);
        dto.setHasEditHistory(editCount > 0);
        return dto;
    }

    @Override
    public QcFormTemplateDTO createTemplate(QcFormTemplateDTO dto) {
        QcFormTemplate template = modelMapper.map(dto, QcFormTemplate.class);
        template.setCreatedAt(OffsetDateTime.now());
        template.setStatus(1);
        template.setApprovalType(dto.getApprovalType());
        return modelMapper.map(qcFormTemplateRepository.save(template), QcFormTemplateDTO.class);
    }

    @Override
    public QcFormTemplateDTO updateTemplate(Long id, QcFormTemplateDTO dto) {
        QcFormTemplate template = qcFormTemplateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found"));

        boolean formStructureChanged = false;

        if (dto.getName() != null) {
            template.setName(dto.getName());
        }
        if (dto.getFormTemplateJson() != null) {
            template.setFormTemplateJson(dto.getFormTemplateJson());
            formStructureChanged = true;
        } else {
            template.setFormTemplateJson(template.getFormTemplateJson());
        }
        if (dto.getApprovalType() != null) {
            template.setApprovalType(dto.getApprovalType());
        }
        template.setUpdatedAt(OffsetDateTime.now());
        template.setUpdatedBy(dto.getUpdatedBy());

        QcFormTemplateDTO updatedTemplate = modelMapper.map(qcFormTemplateRepository.save(template), QcFormTemplateDTO.class);

        if (formStructureChanged) {
            try {
                extractAndStoreKeyLabelPairs(updatedTemplate);
                mongoService.deleteOne("control_limit_setting", new Document("qc_form_template_id", id));
                createControlLimitSetting(updatedTemplate);
            } catch (Exception e) {
                throw new RuntimeException("Failed to update MongoDB documents after template update", e);
            }
        }

        return updatedTemplate;
    }

    @Override
    public QcFormTemplateDTO updateTemplateWithNodeSync(Long id, QcFormTemplateDTO dto) {
        // 1. Capture old state before saving
        QcFormTemplate oldTemplate = qcFormTemplateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found"));
        String oldName = oldTemplate.getName();

        // 2. Build change summary from old vs new (before save)
        String summary = buildChangeSummary(oldTemplate, dto);

        // 4. Perform the standard update
        QcFormTemplateDTO updated = updateTemplate(id, dto);

        // 5. Sync FormNode labels if name changed
        if (!oldName.equals(updated.getName())) {
            formNodeService.updateLabelByQcFormTemplateId(id, updated.getName());
        }

        // 6. Write audit log
        QcFormTemplateEditLog logEntry = new QcFormTemplateEditLog();
        logEntry.setTemplateId(id);
        logEntry.setEditedBy(dto.getUpdatedBy() != null ? dto.getUpdatedBy().longValue() : 0L);
        logEntry.setEditedAt(OffsetDateTime.now());
        logEntry.setChangeSummary(summary);
        editLogRepository.save(logEntry);

        return updated;
    }

    @Override
    public void deleteTemplate(Long id) {
        QcFormTemplate template = qcFormTemplateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found"));
        template.setStatus(0);
        qcFormTemplateRepository.save(template);
    }

    @Override
    public void extractControlLimits(JsonNode widgetList, ObjectNode controlLimits) {
        if (widgetList == null || !widgetList.isArray()) return;

        ObjectMapper mapper = new ObjectMapper();

        for (JsonNode widget : widgetList) {
            String type = widget.get("type").asText();

            if ("grid".equals(type)) {
                for (JsonNode col : widget.get("cols")) {
                    extractControlLimits(col.get("widgetList"), controlLimits);
                }
            } else if ("number".equals(type)) {
                JsonNode options = widget.get("options");
                if (options != null && options.has("name") && options.has("label")) {
                    String name = options.get("name").asText();
                    String label = options.get("label").asText();

                    ObjectNode limit = mapper.createObjectNode();
                    limit.put("upper_control_limit", 99999.00);
                    limit.put("lower_control_limit", 0.00);
                    limit.put("label", label);

                    controlLimits.set(name, limit);
                }
            } else if (type.equals("select") || type.equals("radio") || type.equals("checkbox")) {
                JsonNode options = widget.get("options");
                if (options != null && options.has("name") && options.has("label") && options.has("optionItems")) {
                    String name = options.get("name").asText();
                    String label = options.get("label").asText();
                    JsonNode optionItems = options.get("optionItems");

                    ObjectNode limit = mapper.createObjectNode();
                    limit.put("label", label);

                    List<String> validKeys = new ArrayList<>();
                    for (JsonNode item : optionItems) {
                        validKeys.add(item.get("value").asText());
                    }
                    limit.putPOJO("valid_keys", validKeys);

                    List<Map<String, String>> optionList = new ArrayList<>();
                    for (JsonNode item : optionItems) {
                        Map<String, String> optionMap = new HashMap<>();
                        optionMap.put("label", item.path("label").asText());
                        optionMap.put("value", item.path("value").asText());
                        optionList.add(optionMap);
                    }
                    limit.putPOJO("optionItems", optionList);

                    controlLimits.set(name, limit);
                }
            }
        }
    }

    @Override
    public void createControlLimitSetting(QcFormTemplateDTO template) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(template.getFormTemplateJson());
            JsonNode widgetList = root.get("widgetList");

            ObjectNode controlLimitDoc = mapper.createObjectNode();
            controlLimitDoc.put("qc_form_template_id", template.getId());
            ObjectNode controlLimits = mapper.createObjectNode();

            extractControlLimits(widgetList, controlLimits);
            controlLimitDoc.set("control_limits", controlLimits);
            Document mongoDoc = Document.parse(mapper.writeValueAsString(controlLimitDoc));
            mongoService.insertOne("control_limit_setting", mongoDoc);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create control limit setting", e);
        }
    }

    @Override
    public String resolveLabelFromTemplateByKey(Long templateId, String fieldKey) {
        QcFormTemplate template = qcFormTemplateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found"));

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(template.getFormTemplateJson());
            JsonNode widgetList = root.get("widgetList");
            return findLabelInWidgetList(widgetList, fieldKey);
        } catch (Exception e) {
            throw new RuntimeException("Failed to resolve label from form template", e);
        }
    }

    @Override
    public String getApprovalTypeByFormId(Long formTemplateId) {
        return qcFormTemplateRepository.findApprovalTypeById(formTemplateId);
    }

    @Override
    public void extractAndStoreKeyLabelPairs(QcFormTemplateDTO template) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(template.getFormTemplateJson());
            JsonNode widgetList = root.get("widgetList");

            List<Map<String, String>> fieldList = new ArrayList<>();
            extractInputKeyLabelPairs(widgetList, fieldList);

            // --- SOFT-DELETE MERGE ---
            Document existing = mongoService.findOne("form_template_key_label_pairs",
                    new Document("qc_form_template_id", template.getId()));

            Map<String, Map<String, String>> previousFields = new LinkedHashMap<>();
            if (existing != null && existing.containsKey("fields")) {
                List<Document> oldFields = existing.getList("fields", Document.class);
                for (Document f : oldFields) {
                    Map<String, String> entry = new HashMap<>();
                    entry.put("key", f.getString("key"));
                    entry.put("label", f.getString("label"));
                    entry.put("type", f.getOrDefault("type", "other").toString());
                    if (f.containsKey("deleted")) {
                        entry.put("deleted", f.getString("deleted"));
                    }
                    previousFields.put(f.getString("key"), entry);
                }
            }

            Set<String> activeKeys = fieldList.stream()
                    .map(f -> f.get("key")).collect(Collectors.toSet());

            // Re-add previously known fields that are now absent, marked deleted
            for (Map.Entry<String, Map<String, String>> entry : previousFields.entrySet()) {
                if (!activeKeys.contains(entry.getKey())) {
                    Map<String, String> deletedEntry = new HashMap<>(entry.getValue());
                    deletedEntry.put("deleted", "true");
                    fieldList.add(deletedEntry);
                }
            }
            // --- END SOFT-DELETE MERGE ---

            Document mongoDoc = new Document();
            mongoDoc.put("qc_form_template_id", template.getId());
            mongoDoc.put("fields", fieldList);

            mongoService.replaceOne("form_template_key_label_pairs",
                    new Document("qc_form_template_id", template.getId()),
                    mongoDoc);
        } catch (Exception e) {
            throw new RuntimeException("Failed to store key-label pairs", e);
        }
    }

    @Override
    public List<QcFormTemplateEditLogDTO> getEditLog(Long templateId) {
        List<QcFormTemplateEditLog> logs = editLogRepository.findByTemplateIdOrderByEditedAtDesc(templateId);
        return logs.stream().map(log -> {
            QcFormTemplateEditLogDTO dto = new QcFormTemplateEditLogDTO();
            dto.setId(log.getId());
            dto.setTemplateId(log.getTemplateId());
            dto.setEditedAt(log.getEditedAt());
            dto.setChangeSummary(log.getChangeSummary());
            dto.setEditorName(resolveEditorName(log.getEditedBy()));
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public List<java.util.Map<String, String>> getTemplateFields(Long templateId) {
        try {
            Document doc = mongoService.findOne("form_template_key_label_pairs",
                new Document("qc_form_template_id", templateId));
            if (doc == null || !doc.containsKey("fields")) return java.util.Collections.emptyList();
            List<Document> fields = doc.getList("fields", Document.class);
            return fields.stream().map(f -> {
                java.util.Map<String, String> entry = new java.util.LinkedHashMap<>();
                entry.put("key", f.getString("key"));
                entry.put("label", f.getString("label"));
                if (f.containsKey("deleted")) entry.put("deleted", f.getString("deleted"));
                return entry;
            }).collect(Collectors.toList());
        } catch (Exception e) {
            return java.util.Collections.emptyList();
        }
    }

    private String resolveEditorName(Long userId) {
        if (userId == null || userId == 0L) return "Unknown";
        try {
            String name = userRepository.findNameById(userId.intValue());
            return name != null ? name : "Unknown";
        } catch (Exception e) {
            return "Unknown";
        }
    }

    private String buildChangeSummary(QcFormTemplate old, QcFormTemplateDTO dto) {
        List<String> parts = new ArrayList<>();

        if (dto.getName() != null && !old.getName().equals(dto.getName())) {
            parts.add("Renamed: '" + old.getName() + "' → '" + dto.getName() + "'");
        }
        if (dto.getApprovalType() != null
                && !dto.getApprovalType().equals(old.getApprovalType())) {
            parts.add("Approval flow: " + old.getApprovalType()
                    + " → " + dto.getApprovalType());
        }
        if (dto.getFormTemplateJson() != null) {
            Set<String> oldKeys = extractAllFieldKeys(old.getFormTemplateJson());
            Set<String> newKeys = extractAllFieldKeys(dto.getFormTemplateJson());

            Set<String> added = newKeys.stream()
                    .filter(k -> !oldKeys.contains(k)).collect(Collectors.toSet());
            Set<String> removed = oldKeys.stream()
                    .filter(k -> !newKeys.contains(k)).collect(Collectors.toSet());

            if (!added.isEmpty()) {
                List<String> labels = added.stream()
                        .map(k -> getLabelForKey(k, dto.getFormTemplateJson()))
                        .collect(Collectors.toList());
                parts.add("Fields added: " + String.join(", ", labels));
            }
            if (!removed.isEmpty()) {
                List<String> labels = removed.stream()
                        .map(k -> getLabelForKey(k, old.getFormTemplateJson()))
                        .collect(Collectors.toList());
                parts.add("Fields removed: " + String.join(", ", labels));
            }
            if (added.isEmpty() && removed.isEmpty()
                    && !old.getFormTemplateJson().equals(dto.getFormTemplateJson())) {
                parts.add("Field settings updated");
            }
        }
        return parts.isEmpty() ? "No structural changes" : String.join("; ", parts);
    }

    private Set<String> extractAllFieldKeys(String formTemplateJson) {
        Set<String> keys = new HashSet<>();
        try {
            JsonNode root = new ObjectMapper().readTree(formTemplateJson);
            collectKeys(root.get("widgetList"), keys);
        } catch (Exception ignored) {}
        return keys;
    }

    private void collectKeys(JsonNode widgetList, Set<String> keys) {
        if (widgetList == null || !widgetList.isArray()) return;
        for (JsonNode widget : widgetList) {
            if (widget.has("formItemFlag") && widget.get("formItemFlag").asBoolean()) {
                JsonNode opts = widget.get("options");
                if (opts != null && opts.has("name")) keys.add(opts.get("name").asText());
            }
            if (widget.has("widgetList")) collectKeys(widget.get("widgetList"), keys);
            if (widget.has("cols")) {
                for (JsonNode col : widget.get("cols")) {
                    collectKeys(col.get("widgetList"), keys);
                }
            }
        }
    }

    private String getLabelForKey(String key, String formTemplateJson) {
        try {
            JsonNode root = new ObjectMapper().readTree(formTemplateJson);
            String found = findLabelInWidgetList(root.get("widgetList"), key);
            return found != null ? found + " (" + key + ")" : key;
        } catch (Exception e) {
            return key;
        }
    }

    private void extractInputKeyLabelPairs(JsonNode widgetList, List<Map<String, String>> result) {
        if (widgetList == null || !widgetList.isArray()) return;

        for (JsonNode widget : widgetList) {
            if (widget.has("formItemFlag") && widget.get("formItemFlag").asBoolean()) {
                JsonNode options = widget.get("options");
                if (options != null && options.has("name") && options.has("label")) {
                    String key = options.get("name").asText();
                    String label = options.get("label").asText();
                    String widgetType = widget.has("type") ? widget.get("type").asText() : "input";
                    String fieldType = inferFieldType(widgetType, options);

                    Map<String, String> entry = new HashMap<>();
                    entry.put("key", key);
                    entry.put("label", label);
                    entry.put("type", fieldType);
                    result.add(entry);
                }
            }

            if (widget.has("widgetList")) {
                extractInputKeyLabelPairs(widget.get("widgetList"), result);
            }

            if (widget.has("cols")) {
                for (JsonNode col : widget.get("cols")) {
                    if (col.has("widgetList")) {
                        extractInputKeyLabelPairs(col.get("widgetList"), result);
                    }
                }
            }
        }
    }

    /**
     * Infer field type category from widget type
     */
    private String inferFieldType(String widgetType, JsonNode options) {
        switch (widgetType) {
            case "number":
                return "number";
            case "select":
                return "select";
            case "radio":
                return "radio";
            case "checkbox":
                return "checkbox";
            case "input":
                if (options != null && options.has("type")) {
                    String inputType = options.get("type").asText();
                    if ("number".equals(inputType)) {
                        return "number";
                    }
                }
                return "other";
            default:
                return "other";
        }
    }

    private String findLabelInWidgetList(JsonNode widgetList, String fieldKey) {
        if (widgetList == null || !widgetList.isArray()) return null;

        for (JsonNode widget : widgetList) {
            String type = widget.has("type") ? widget.get("type").asText() : "";

            // Recurse into grid columns
            if ("grid".equals(type) && widget.has("cols")) {
                for (JsonNode col : widget.get("cols")) {
                    String label = findLabelInWidgetList(col.get("widgetList"), fieldKey);
                    if (label != null) return label;
                }
            }

            // Recurse into nested widgetList (tabs, sub-forms, etc.)
            if (widget.has("widgetList")) {
                String label = findLabelInWidgetList(widget.get("widgetList"), fieldKey);
                if (label != null) return label;
            }

            // Match any field widget by options.name
            JsonNode options = widget.get("options");
            if (options != null && options.has("name") && options.has("label")) {
                if (fieldKey.equals(options.get("name").asText())) {
                    return options.get("label").asText();
                }
            }
        }

        return null;
    }
}
