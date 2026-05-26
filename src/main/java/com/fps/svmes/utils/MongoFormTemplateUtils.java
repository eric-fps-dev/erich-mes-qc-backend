package com.fps.svmes.utils;

import com.fps.svmes.repositories.jpaRepo.qcForm.QcFormTemplateRepository;

import com.fps.svmes.repositories.jpaRepo.user.UserRepository;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class MongoFormTemplateUtils {

    @Autowired
    private QcFormTemplateRepository qcFormTemplateRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    public HashMap<String, String> getFormTemplateKeyValueMapping(Long formId) {
        String formTemplateJson = qcFormTemplateRepository.findFormTemplateJsonById(formId);
        if (formTemplateJson == null || formTemplateJson.isEmpty()) {
            throw new RuntimeException("Form template JSON not found for formId: " + formId);
        }

        HashMap<String, String> keyValueMap = new HashMap<>();
        Document formTemplate = Document.parse(formTemplateJson);
        List<Document> widgetList = (List<Document>) formTemplate.get("widgetList");
        if (widgetList != null) {
            extractKeyValuePairs(widgetList, keyValueMap);
        }

        // Supplement with historical field mappings for deleted fields
        try {
            Query pairsQuery = new Query();
            pairsQuery.addCriteria(Criteria.where("qc_form_template_id").is(formId));
            Document pairsDoc = mongoTemplate.findOne(pairsQuery, Document.class, "form_template_key_label_pairs");
            if (pairsDoc != null && pairsDoc.containsKey("fields")) {
                List<Document> fields = pairsDoc.getList("fields", Document.class);
                for (Document field : fields) {
                    String key = field.getString("key");
                    String label = field.getString("label");
                    if (key != null && label != null && !keyValueMap.containsKey(key)) {
                        keyValueMap.put(key, label);
                    }
                }
            }
        } catch (Exception e) {
            // Non-fatal: proceed without historical mappings
        }

        return keyValueMap;
    }

    public HashMap<String, Object> getOptionItemsKeyValueMapping(Long formId) {
        String formTemplateJson = qcFormTemplateRepository.findFormTemplateJsonById(formId);
        if (formTemplateJson == null || formTemplateJson.isEmpty()) {
            throw new RuntimeException("Form template JSON not found for formId: " + formId);
        }

        HashMap<String, Object> optionItemsKeyValueMap = new HashMap<>();
        Document formTemplate = Document.parse(formTemplateJson);
        List<Document> widgetList = (List<Document>) formTemplate.get("widgetList");
        if (widgetList != null) {
            extractOptionItems(widgetList, optionItemsKeyValueMap);
        }

        // Supplement with historical option items for deleted fields
        try {
            Query pairsQuery = new Query();
            pairsQuery.addCriteria(Criteria.where("qc_form_template_id").is(formId));
            Document pairsDoc = mongoTemplate.findOne(pairsQuery, Document.class, "form_template_key_label_pairs");
            if (pairsDoc != null && pairsDoc.containsKey("option_items")) {
                Document storedOptionItems = (Document) pairsDoc.get("option_items");
                for (String fieldKey : storedOptionItems.keySet()) {
                    if (!optionItemsKeyValueMap.containsKey(fieldKey)) {
                        Document mapping = (Document) storedOptionItems.get(fieldKey);
                        HashMap<String, String> valueToLabel = new HashMap<>();
                        for (String v : mapping.keySet()) {
                            valueToLabel.put(v, mapping.getString(v));
                        }
                        optionItemsKeyValueMap.put(fieldKey, valueToLabel);
                    }
                }
            }
        } catch (Exception e) {
            // Non-fatal: proceed without historical option items
        }

        return optionItemsKeyValueMap;
    }

    private void extractKeyValuePairs(List<Document> widgetList, HashMap<String, String> keyValueMap) {
        for (Document widget : widgetList) {
            Document options = (Document) widget.get("options");
            if (options != null) {
                String name = options.getString("name");
                String label = options.getString("label");
                if (name != null && label != null) {
                    keyValueMap.put(name, label);
                }
            }

            List<Document> nestedWidgetList = (List<Document>) widget.get("widgetList");
            if (nestedWidgetList != null) {
                extractKeyValuePairs(nestedWidgetList, keyValueMap);
            }

            List<Document> cols = (List<Document>) widget.get("cols");
            if (cols != null) {
                for (Document col : cols) {
                    List<Document> colWidgetList = (List<Document>) col.get("widgetList");
                    if (colWidgetList != null) {
                        extractKeyValuePairs(colWidgetList, keyValueMap);
                    }
                }
            }
        }
    }

    private void extractOptionItems(List<Document> widgetList, HashMap<String, Object> optionItemsKeyValueMap) {
        for (Document widget : widgetList) {
            Document options = (Document) widget.get("options");
            if (options != null) {
                String name = options.getString("name");
                List<Document> optionItems = (List<Document>) options.get("optionItems");

                if (name != null && optionItems != null) {
                    HashMap<String, String> valueToLabelMap = new HashMap<>();
                    for (Document option : optionItems) {
                        Object value = option.get("value");
                        String label = option.getString("label");
                        if (value != null && label != null) {
                            valueToLabelMap.put(value.toString(), label);
                        }
                    }
                    optionItemsKeyValueMap.put(name, valueToLabelMap);
                }
            }

            List<Document> nestedWidgetList = (List<Document>) widget.get("widgetList");
            if (nestedWidgetList != null) {
                extractOptionItems(nestedWidgetList, optionItemsKeyValueMap);
            }

            List<Document> cols = (List<Document>) widget.get("cols");
            if (cols != null) {
                for (Document col : cols) {
                    List<Document> colWidgetList = (List<Document>) col.get("widgetList");
                    if (colWidgetList != null) {
                        extractOptionItems(colWidgetList, optionItemsKeyValueMap);
                    }
                }
            }
        }
    }

    public Document formatRecord(Document document, HashMap<String, Object> optionItemsKeyValueMap, HashMap<String, String> keyValueMap) {
        Document formatted = new Document();

        for (String key : document.keySet()) {
            Object value = document.get(key);

            if ("_id".equals(key) && value instanceof ObjectId) {
                formatted.put("_id", value.toString());
                continue;
            }

            String displayKey = keyValueMap.getOrDefault(key, key);

            if (optionItemsKeyValueMap.containsKey(key) && value instanceof List) {
                List<?> valueList = (List<?>) value;
                HashMap<String, String> labelMap = (HashMap<String, String>) optionItemsKeyValueMap.get(key);
                List<String> resolvedLabels = valueList.stream()
                        .map(val -> val != null ? labelMap.getOrDefault(val.toString(), val.toString()) : null)
                        .collect(Collectors.toList());
                formatted.put(displayKey, resolvedLabels);
            } else if (optionItemsKeyValueMap.containsKey(key)) {
                HashMap<String, String> labelMap = (HashMap<String, String>) optionItemsKeyValueMap.get(key);
                if (value != null) {
                    formatted.put(displayKey, labelMap.getOrDefault(value.toString(), value.toString()));
                } else {
                    formatted.put(displayKey, null);
                }
            } else {
                formatted.put(displayKey, value);
            }

            if ("created_by".equals(key) && value instanceof Long) {
                try {
                    String creatorName = userRepository.findNameById(Math.toIntExact((Long) value));

                    formatted.put("提交人", (creatorName != null ? creatorName : "未知用户"));
                } catch (Exception e) {
                    formatted.put("提交人", "未知用户");
                }
            }
        }

        if (document.containsKey("exceeded_info")) {
            Document original = (Document) document.get("exceeded_info");
            Document labeled = new Document();
            for (String raw : original.keySet()) {
                String labeledKey = keyValueMap.getOrDefault(raw, raw);
                labeled.put(labeledKey, original.get(raw));
            }
            formatted.put("exceeded_info", labeled);
        }

        return formatted;
    }

    public Object formatSubmissionSnapshotForResponse(Object snapshot, Long formId) {
        if (formId == null) {
            return snapshot;
        }

        Document document = toDocument(snapshot);
        if (document == null) {
            return snapshot;
        }
        return formatRecordWithDividers(document, formId);
    }

    public Document formatRecordWithDividers(Document document, Long formId) {
        HashMap<String, String> keyValueMap = getFormTemplateKeyValueMapping(formId);
        HashMap<String, Object> optionItemsKeyValueMap = getOptionItemsKeyValueMapping(formId);
        HashMap<String, String> fieldToDividerMap = new HashMap<>();
        List<Document> widgetList = getWidgetListFromTemplate(formId);
        // Build the same field -> divider ownership map used by QC task submission log formatting.
        populateFieldToDividerMap(widgetList, "uncategorized", fieldToDividerMap);

        Document formattedDocument = new Document();
        Document groupedData = new Document();

        for (String key : document.keySet()) {
            Object value = document.get(key);
            String formattedKey = keyValueMap.getOrDefault(key, key);
            String dividerLabel = fieldToDividerMap.getOrDefault(key, "uncategorized");

            if ("_id".equals(key) && value instanceof ObjectId) {
                formattedDocument.put("_id", value.toString());
                continue;
            }

            if (optionItemsKeyValueMap.containsKey(key) && value instanceof List<?> valueList) {
                HashMap<String, String> valueToLabelMap = (HashMap<String, String>) optionItemsKeyValueMap.get(key);
                value = valueList.stream()
                        .map(val -> val != null ? valueToLabelMap.getOrDefault(val.toString(), val.toString()) : null)
                        .collect(Collectors.toList());
            } else if (optionItemsKeyValueMap.containsKey(key) && value != null) {
                HashMap<String, String> valueToLabelMap = (HashMap<String, String>) optionItemsKeyValueMap.get(key);
                value = valueToLabelMap.getOrDefault(value.toString(), value.toString());
            }

            if (List.of("_id", "created_at", "created_by").contains(key)) {
                formattedDocument.put(formattedKey, value);
            } else {
                groupedData.computeIfAbsent(dividerLabel, ignored -> new Document());
                ((Document) groupedData.get(dividerLabel)).put(formattedKey, value);
            }

            if ("created_by".equals(key) && value instanceof Long longValue) {
                try {
                    String creatorName = userRepository.findNameById(Math.toIntExact(longValue));
                    formattedDocument.put("提交人", creatorName != null ? creatorName : "未知用户");
                } catch (Exception e) {
                    formattedDocument.put("提交人", "未知用户");
                }
            }
        }

        if (document.containsKey("exceeded_info")) {
            Document original = (Document) document.get("exceeded_info");
            Document labeled = new Document();
            for (String raw : original.keySet()) {
                String labeledKey = keyValueMap.getOrDefault(raw, raw);
                labeled.put(labeledKey, original.get(raw));
            }
            formattedDocument.put("exceeded_info", labeled);
        }

        formattedDocument.putAll(groupedData);
        return formattedDocument;
    }

    private void populateFieldToDividerMap(List<Document> widgetList,
                                           String currentDivider,
                                           HashMap<String, String> fieldToDividerMap) {
        if (widgetList == null) {
            return;
        }

        String activeDivider = currentDivider;
        for (Document widget : widgetList) {
            String type = widget.getString("type");
            Document options = (Document) widget.get("options");

            if ("divider".equals(type) && options != null) {
                activeDivider = options.getString("label");
                continue;
            }

            if (options != null && options.containsKey("name")) {
                fieldToDividerMap.put(options.getString("name"), activeDivider);
            }

            List<Document> nestedWidgetList = (List<Document>) widget.get("widgetList");
            if (nestedWidgetList != null) {
                // Nested widget containers inherit the nearest divider label.
                populateFieldToDividerMap(nestedWidgetList, activeDivider, fieldToDividerMap);
            }

            List<Document> cols = (List<Document>) widget.get("cols");
            if (cols != null) {
                for (Document col : cols) {
                    List<Document> colWidgetList = (List<Document>) col.get("widgetList");
                    if (colWidgetList != null) {
                        // Grid column fields also belong to the current divider section.
                        populateFieldToDividerMap(colWidgetList, activeDivider, fieldToDividerMap);
                    }
                }
            }
        }
    }

    private List<Document> getWidgetListFromTemplate(Long formId) {
        String formTemplateJson = qcFormTemplateRepository.findFormTemplateJsonById(formId);
        if (formTemplateJson == null || formTemplateJson.isEmpty()) {
            throw new RuntimeException("Form template JSON not found for formId: " + formId);
        }
        Document formTemplate = Document.parse(formTemplateJson);
        return (List<Document>) formTemplate.get("widgetList");
    }

    private Document toDocument(Object value) {
        if (value instanceof Document document) {
            return copyDocument(document);
        }
        if (value instanceof Map<?, ?> map) {
            Document document = new Document();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() != null) {
                    document.put(entry.getKey().toString(), normalizeValue(entry.getValue()));
                }
            }
            return document;
        }
        return null;
    }

    private Document copyDocument(Document source) {
        Document copy = new Document();
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            copy.put(entry.getKey(), normalizeValue(entry.getValue()));
        }
        return copy;
    }

    private Object normalizeValue(Object value) {
        if (value instanceof Document document) {
            return copyDocument(document);
        }
        if (value instanceof Map<?, ?> map) {
            return toDocument(map);
        }
        if (value instanceof List<?> list) {
            return list.stream()
                    .map(this::normalizeValue)
                    .toList();
        }
        return value;
    }
}
