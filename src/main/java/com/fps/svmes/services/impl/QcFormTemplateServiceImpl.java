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

import com.fps.svmes.models.nosql.FormNode;
import org.bson.Document;
import java.time.LocalDate;
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
                mergeControlLimitSettings(updatedTemplate, id);
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

    private void mergeControlLimitSettings(QcFormTemplateDTO updatedTemplate, Long templateId) {
        try {
            ObjectMapper mapper = new ObjectMapper();

            // Load existing control limits from MongoDB
            Document existingDoc = mongoService.findOne("control_limit_setting",
                    new Document("qc_form_template_id", templateId));
            Document oldControlLimits = (existingDoc != null && existingDoc.containsKey("control_limits"))
                    ? (Document) existingDoc.get("control_limits")
                    : new Document();

            // Build merged result
            JsonNode root = mapper.readTree(updatedTemplate.getFormTemplateJson());
            JsonNode widgetList = root.get("widgetList");

            ObjectNode controlLimitDoc = mapper.createObjectNode();
            controlLimitDoc.put("qc_form_template_id", updatedTemplate.getId());
            ObjectNode mergedLimits = mapper.createObjectNode();

            mergeControlLimits(widgetList, mergedLimits, oldControlLimits, mapper);
            controlLimitDoc.set("control_limits", mergedLimits);

            Document mongoDoc = Document.parse(mapper.writeValueAsString(controlLimitDoc));
            mongoService.replaceOne("control_limit_setting",
                    new Document("qc_form_template_id", templateId), mongoDoc);

        } catch (Exception e) {
            throw new RuntimeException("Failed to merge control limit settings", e);
        }
    }

    private void mergeControlLimits(JsonNode widgetList, ObjectNode mergedLimits,
                                     Document oldControlLimits, ObjectMapper mapper) {
        if (widgetList == null || !widgetList.isArray()) return;

        for (JsonNode widget : widgetList) {
            String type = widget.has("type") ? widget.get("type").asText() : "";

            if ("grid".equals(type)) {
                JsonNode cols = widget.get("cols");
                if (cols != null) {
                    for (JsonNode col : cols) {
                        mergeControlLimits(col.get("widgetList"), mergedLimits, oldControlLimits, mapper);
                    }
                }
            } else if ("number".equals(type)) {
                JsonNode options = widget.get("options");
                if (options != null && options.has("name") && options.has("label")) {
                    String name = options.get("name").asText();
                    String label = options.get("label").asText();

                    ObjectNode limit = mapper.createObjectNode();
                    limit.put("label", label);

                    Document oldEntry = oldControlLimits.containsKey(name)
                            ? (Document) oldControlLimits.get(name) : null;

                    if (oldEntry != null && oldEntry.containsKey("upper_control_limit")
                            && oldEntry.containsKey("lower_control_limit")) {
                        // Unchanged field: preserve custom limits
                        limit.put("upper_control_limit", ((Number) oldEntry.get("upper_control_limit")).doubleValue());
                        limit.put("lower_control_limit", ((Number) oldEntry.get("lower_control_limit")).doubleValue());
                    } else {
                        // New field or type changed: apply defaults
                        limit.put("upper_control_limit", 99999.00);
                        limit.put("lower_control_limit", 0.00);
                    }
                    mergedLimits.set(name, limit);
                }
            } else if ("select".equals(type) || "radio".equals(type) || "checkbox".equals(type)) {
                JsonNode options = widget.get("options");
                if (options != null && options.has("name") && options.has("label") && options.has("optionItems")) {
                    String name = options.get("name").asText();
                    String label = options.get("label").asText();
                    JsonNode optionItems = options.get("optionItems");

                    List<String> newOptionValues = new ArrayList<>();
                    List<Map<String, String>> optionList = new ArrayList<>();
                    for (JsonNode item : optionItems) {
                        String value = item.path("value").asText();
                        newOptionValues.add(value);
                        Map<String, String> optMap = new HashMap<>();
                        optMap.put("value", value);
                        optMap.put("label", item.path("label").asText());
                        optionList.add(optMap);
                    }

                    ObjectNode limit = mapper.createObjectNode();
                    limit.put("label", label);
                    limit.putPOJO("optionItems", optionList);

                    Document oldEntry = oldControlLimits.containsKey(name)
                            ? (Document) oldControlLimits.get(name) : null;

                    if (oldEntry != null && oldEntry.containsKey("valid_keys")) {
                        // Detect whether the option set changed
                        Set<String> oldOptionValues = new LinkedHashSet<>();
                        List<?> oldItems = (List<?>) oldEntry.get("optionItems");
                        if (oldItems != null) {
                            for (Object item : oldItems) {
                                if (item instanceof Document) {
                                    Object val = ((Document) item).get("value");
                                    if (val != null) oldOptionValues.add(val.toString());
                                }
                            }
                        }

                        if (oldOptionValues.equals(new LinkedHashSet<>(newOptionValues))) {
                            // Options unchanged: preserve valid_keys (strip any that no longer exist)
                            List<String> keptKeys = new ArrayList<>();
                            List<?> oldValidKeys = (List<?>) oldEntry.get("valid_keys");
                            if (oldValidKeys != null) {
                                Set<String> newSet = new LinkedHashSet<>(newOptionValues);
                                for (Object vk : oldValidKeys) {
                                    if (newSet.contains(vk.toString())) keptKeys.add(vk.toString());
                                }
                            }
                            limit.putPOJO("valid_keys", keptKeys);
                        } else {
                            // Options changed: reset to all valid (default)
                            limit.putPOJO("valid_keys", newOptionValues);
                        }
                    } else {
                        // New field or was numeric type: default all valid
                        limit.putPOJO("valid_keys", newOptionValues);
                    }
                    mergedLimits.set(name, limit);
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

            // --- OPTION ITEMS ---
            Map<String, Map<String, String>> optionItemsMap = new LinkedHashMap<>();
            extractOptionItemPairs(widgetList, optionItemsMap);

            // Preserve previously stored option items for deleted fields
            if (existing != null && existing.containsKey("option_items")) {
                Document existingOptionItems = (Document) existing.get("option_items");
                for (String fieldKey : existingOptionItems.keySet()) {
                    if (!optionItemsMap.containsKey(fieldKey)) {
                        Document oldMapping = (Document) existingOptionItems.get(fieldKey);
                        Map<String, String> preserved = new LinkedHashMap<>();
                        for (String v : oldMapping.keySet()) {
                            preserved.put(v, oldMapping.getString(v));
                        }
                        optionItemsMap.put(fieldKey, preserved);
                    }
                }
            }
            // --- END OPTION ITEMS ---

            Document mongoDoc = new Document();
            mongoDoc.put("qc_form_template_id", template.getId());
            mongoDoc.put("fields", fieldList);
            mongoDoc.put("option_items", optionItemsMap);

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
    public List<java.util.Map<String, Object>> getTemplateFields(Long templateId) {
        try {
            Document doc = mongoService.findOne("form_template_key_label_pairs",
                new Document("qc_form_template_id", templateId));
            if (doc == null || !doc.containsKey("fields")) return java.util.Collections.emptyList();

            // option_items is stored at the top level as {fieldKey: {optionValue: optionLabel}}
            Document optionItemsDoc = doc.containsKey("option_items")
                ? (Document) doc.get("option_items")
                : new Document();

            List<Document> fields = doc.getList("fields", Document.class);
            return fields.stream().map(f -> {
                java.util.Map<String, Object> entry = new java.util.LinkedHashMap<>();
                entry.put("key", f.getString("key"));
                entry.put("label", f.getString("label"));
                String deleted = f.getString("deleted");
                if (deleted != null) entry.put("deleted", deleted);
                String type = f.getString("type");
                if (type != null) entry.put("type", type);

                // Only attach optionItems for deleted fields whose type has discrete options
                // (i.e. not "number" or "other") so the frontend can map raw stored values to labels
                boolean isDeleted = "true".equals(deleted);
                boolean isOptionType = type != null && !type.equals("number") && !type.equals("other");
                if (isDeleted && isOptionType) {
                    String fieldKey = f.getString("key");
                    if (optionItemsDoc.containsKey(fieldKey)) {
                        Document valueToLabel = (Document) optionItemsDoc.get(fieldKey);
                        List<java.util.Map<String, String>> optionList = new java.util.ArrayList<>();
                        for (Map.Entry<String, Object> e : valueToLabel.entrySet()) {
                            java.util.Map<String, String> opt = new java.util.LinkedHashMap<>();
                            opt.put("value", e.getKey());
                            opt.put("label", e.getValue() != null ? e.getValue().toString() : "");
                            optionList.add(opt);
                        }
                        if (!optionList.isEmpty()) entry.put("optionItems", optionList);
                    }
                }
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

    private void extractOptionItemPairs(JsonNode widgetList, Map<String, Map<String, String>> result) {
        if (widgetList == null || !widgetList.isArray()) return;
        for (JsonNode widget : widgetList) {
            if (widget.has("formItemFlag") && widget.get("formItemFlag").asBoolean()) {
                JsonNode options = widget.get("options");
                if (options != null && options.has("name") && options.has("optionItems") && options.get("optionItems").isArray()) {
                    String key = options.get("name").asText();
                    Map<String, String> valueToLabel = new LinkedHashMap<>();
                    for (JsonNode item : options.get("optionItems")) {
                        if (item.has("value") && item.has("label")) {
                            valueToLabel.put(item.get("value").asText(), item.get("label").asText());
                        }
                    }
                    if (!valueToLabel.isEmpty()) {
                        result.put(key, valueToLabel);
                    }
                }
            }
            if (widget.has("widgetList")) {
                extractOptionItemPairs(widget.get("widgetList"), result);
            }
            if (widget.has("cols")) {
                for (JsonNode col : widget.get("cols")) {
                    if (col.has("widgetList")) {
                        extractOptionItemPairs(col.get("widgetList"), result);
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

    @Override
    public Map<String, Object> duplicateTemplate(Long templateId, String sourceNodeId, Integer requestedBy) {
        QcFormTemplate original = qcFormTemplateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found: " + templateId));

        QcFormTemplateDTO dto = new QcFormTemplateDTO();
        dto.setName("Copy of " + original.getName());
        dto.setFormTemplateJson(original.getFormTemplateJson());
        dto.setApprovalType(original.getApprovalType());
        dto.setCreatedBy(requestedBy);
        QcFormTemplateDTO newTemplate = createTemplate(dto);

        FormNode newNode = new FormNode();
        newNode.setLabel(newTemplate.getName());
        newNode.setNodeType("document");
        newNode.setQcFormTemplateId(newTemplate.getId());

        String parentId = formNodeService.findParentNodeId(sourceNodeId).orElse(null);
        if (parentId == null || "root".equals(parentId)) {
            formNodeService.saveNode(newNode);
        } else {
            formNodeService.addChildNode(parentId, newNode);
        }

        LocalDate now = LocalDate.now();
        String yearMonth = now.getYear() + String.format("%02d", now.getMonthValue());
        String collectionName = "form_template_" + newTemplate.getId() + "_" + yearMonth;
        mongoService.createCollection(collectionName);

        extractAndStoreKeyLabelPairs(newTemplate);
        createControlLimitSetting(newTemplate);

        QcFormTemplateEditLog log = new QcFormTemplateEditLog();
        log.setTemplateId(newTemplate.getId());
        log.setEditedBy(requestedBy != null ? requestedBy.longValue() : 0L);
        log.setEditedAt(OffsetDateTime.now());
        log.setChangeSummary("Duplicated from template " + templateId);
        editLogRepository.save(log);

        return Map.of("id", newTemplate.getId());
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
