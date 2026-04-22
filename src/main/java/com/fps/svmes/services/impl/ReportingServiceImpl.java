package com.fps.svmes.services.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fps.svmes.dto.PagedResultDTO;
import com.fps.svmes.dto.dtos.reporting.OptionItemDTO;
import com.fps.svmes.dto.dtos.reporting.TimeBucketedOptionDTO;
import com.fps.svmes.dto.dtos.reporting.WidgetDataDTO;
import com.fps.svmes.repositories.jpaRepo.qcForm.QcFormTemplateRepository;
import com.fps.svmes.repositories.jpaRepo.user.UserRepository;
import com.fps.svmes.services.ReportingService;
import com.fps.shared.entity.primary.user.User;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Sorts.ascending;
import static com.mongodb.client.model.Sorts.descending;

import java.util.List;
import java.util.Collections;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

@Service
public class ReportingServiceImpl implements ReportingService {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MongoClient mongoClient;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter MONGO_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS");

    @Autowired
    QcFormTemplateRepository qcFormTemplateRepository;

    @Autowired
    UserRepository userRepository;

    @Value("${spring.data.mongodb.database}")
    private String mongoDatabaseName;

    @Autowired
    public ReportingServiceImpl(MongoClient mongoClient) {
        this.mongoClient = mongoClient;
    }

    @Override
    public List<WidgetDataDTO> extractWidgetData(String jsonInput) {
        List<WidgetDataDTO> extractedData = new ArrayList<>();
        parseJsonToWidgetList(jsonInput, extractedData);
        return extractedData;
    }

    @Override
    public List<WidgetDataDTO> extractWidgetDataWithCounts(Long formTemplateId, String startDateTime, String endDateTime) {
        // Set default start and end timestamps (Parse as UTC)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        Timestamp defaultStart = Timestamp.from(LocalDateTime.parse(startDateTime, formatter).atZone(ZoneOffset.UTC).toInstant());
        Timestamp defaultEnd = Timestamp.from(LocalDateTime.parse(endDateTime, formatter).atZone(ZoneOffset.UTC).toInstant());

        String jsonInput = qcFormTemplateRepository.findFormTemplateJsonById(formTemplateId);
        List<WidgetDataDTO> widgetDataList = extractWidgetData(jsonInput);
        MongoDatabase database = mongoClient.getDatabase(mongoDatabaseName);

        appendDeletedFields(formTemplateId, widgetDataList, database);

        // Use updated generateCollectionNames with default timestamps
        List<String> collectionNames = generateCollectionNames(formTemplateId, defaultStart, defaultEnd);

        // Apply updates step by step
        for (String collectionName : collectionNames) {
            List<WidgetDataDTO> newData = processCollection(database, collectionName, widgetDataList, defaultStart, defaultEnd);
            mergeWidgetDataLists(widgetDataList, newData); // Merge instead of overwriting
        }

        return widgetDataList;
    }

    private void appendDeletedFields(Long formTemplateId, List<WidgetDataDTO> widgetDataList, MongoDatabase database) {
        try {
            MongoCollection<Document> pairsCollection = database.getCollection("form_template_key_label_pairs");
            Document pairsDoc = pairsCollection.find(eq("qc_form_template_id", formTemplateId)).first();
            if (pairsDoc == null) return;

            List<Document> fields = pairsDoc.getList("fields", Document.class);
            if (fields == null) return;

            Document optionItemsDoc = pairsDoc.containsKey("option_items")
                    ? (Document) pairsDoc.get("option_items")
                    : new Document();

            Set<String> existingNames = widgetDataList.stream()
                    .map(WidgetDataDTO::getName)
                    .collect(Collectors.toSet());

            for (Document field : fields) {
                if (!"true".equals(field.getString("deleted"))) continue;
                String key = field.getString("key");
                if (key == null || existingNames.contains(key)) continue;

                String label = field.getString("label");
                String type = field.getOrDefault("type", "other").toString();

                if ("number".equals(type)) {
                    WidgetDataDTO dto = new WidgetDataDTO(key, label, "number", new ArrayList<>(), null, null);
                    dto.setDeleted(true);
                    widgetDataList.add(dto);
                } else if ("select".equals(type) || "radio".equals(type) || "checkbox".equals(type)) {
                    List<OptionItemDTO> optionList = new ArrayList<>();
                    if (optionItemsDoc.containsKey(key)) {
                        Document valueToLabel = (Document) optionItemsDoc.get(key);
                        for (String v : valueToLabel.keySet()) {
                            try {
                                int intValue = Integer.parseInt(v);
                                optionList.add(new OptionItemDTO(valueToLabel.getString(v), intValue, 0));
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                    if (!optionList.isEmpty()) {
                        WidgetDataDTO dto = new WidgetDataDTO(key, label, type, optionList, null, null);
                        dto.setDeleted(true);
                        widgetDataList.add(dto);
                    }
                }
            }
        } catch (Exception e) {
            // Non-fatal: deleted field append failure should not break chart loading
        }
    }

    private void mergeWidgetDataLists(List<WidgetDataDTO> originalList, List<WidgetDataDTO> newList) {
        Map<String, WidgetDataDTO> widgetMap = originalList.stream()
                .collect(Collectors.toMap(WidgetDataDTO::getName, w -> w, (w1, w2) -> w1));

        for (WidgetDataDTO newWidget : newList) {
            WidgetDataDTO existingWidget = widgetMap.get(newWidget.getName());

            if (existingWidget != null) {
                if (newWidget.getType().equals("number")) {
                    if (existingWidget.getChartData() == null) {
                        existingWidget.setChartData(new ArrayList<>());
                    }
                    if (existingWidget.getXaxisData() == null) {
                        existingWidget.setXaxisData(new ArrayList<>());
                    }

                    existingWidget.getChartData().addAll(newWidget.getChartData());
                    existingWidget.getXaxisData().addAll(newWidget.getXaxisData());

                    List<Map.Entry<String, Double>> sortedEntries = new ArrayList<>();
                    int size = Math.min(existingWidget.getXaxisData().size(), existingWidget.getChartData().size());
                    for (int i = 0; i < size; i++) {
                        sortedEntries.add(Map.entry(existingWidget.getXaxisData().get(i), existingWidget.getChartData().get(i)));
                    }
                    sortedEntries.sort(Map.Entry.comparingByKey());

                    existingWidget.setXaxisData(sortedEntries.stream().map(Map.Entry::getKey).collect(Collectors.toList()));
                    existingWidget.setChartData(sortedEntries.stream().map(Map.Entry::getValue).collect(Collectors.toList()));

                } else {
                    if (existingWidget.getOptionItems() == null) {
                        existingWidget.setOptionItems(new ArrayList<>());
                    }
                    if (newWidget.getOptionItems() == null) {
                        newWidget.setOptionItems(new ArrayList<>());
                    }

                    // ✅ Corrected: Sum up counts across multiple collections properly
                    Map<Integer, Integer> countMap = new HashMap<>();
                    Map<Integer, String> valueToLabelMap = new HashMap<>();

                    for (OptionItemDTO item : existingWidget.getOptionItems()) {
                        countMap.put(item.getValue(), item.getCount());
                        valueToLabelMap.put(item.getValue(), item.getLabel());
                    }

                    for (OptionItemDTO item : newWidget.getOptionItems()) {
                        countMap.put(item.getValue(), countMap.getOrDefault(item.getValue(), 0) + item.getCount());
                        valueToLabelMap.putIfAbsent(item.getValue(), item.getLabel());
                    }

                    // ✅ Ensuring counts are correctly accumulated across months
                    List<OptionItemDTO> updatedOptions = countMap.entrySet().stream()
                            .map(e -> new OptionItemDTO(valueToLabelMap.get(e.getKey()), e.getKey(), e.getValue()))
                            .collect(Collectors.toList());

                    existingWidget.setOptionItems(updatedOptions);

                    // ✅ MERGE Time-Bucketed Data
                    if (existingWidget.getTimeBucketedData() == null || existingWidget.getTimeBucketedData().isEmpty()) {
                        existingWidget.setTimeBucketedData(newWidget.getTimeBucketedData());
                        existingWidget.setBucketLabels(newWidget.getBucketLabels());
                        existingWidget.setBucketType(newWidget.getBucketType());
                    } else if (newWidget.getTimeBucketedData() != null && !newWidget.getTimeBucketedData().isEmpty()) {
                        // Merge counts for each option
                        Map<Integer, TimeBucketedOptionDTO> existingMap = existingWidget.getTimeBucketedData().stream()
                                .collect(Collectors.toMap(TimeBucketedOptionDTO::getValue, d -> d));

                        for (TimeBucketedOptionDTO newData : newWidget.getTimeBucketedData()) {
                            TimeBucketedOptionDTO existingData = existingMap.get(newData.getValue());
                            if (existingData != null) {
                                // Sum up the counts element-wise
                                List<Integer> existingCounts = existingData.getCounts();
                                List<Integer> newCounts = newData.getCounts();
                                int len = Math.min(existingCounts.size(), newCounts.size());
                                for (int i = 0; i < len; i++) {
                                    existingCounts.set(i, existingCounts.get(i) + newCounts.get(i));
                                }
                            } else {
                                existingWidget.getTimeBucketedData().add(newData);
                            }
                        }
                    }
                }
            } else {
                originalList.add(newWidget);
            }
        }
    }

    private void parseJsonToWidgetList(String jsonInput, List<WidgetDataDTO> extractedData) {
        try {
            JsonNode rootNode = objectMapper.readTree(jsonInput);
            JsonNode widgetList = rootNode.path("widgetList");

            if (widgetList.isArray()) {
                for (JsonNode widgetNode : widgetList) {
                    extractWidgetRecursive(widgetNode, extractedData);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void extractWidgetRecursive(JsonNode widgetNode, List<WidgetDataDTO> extractedData) {
        String type = widgetNode.path("type").asText();

        JsonNode options = widgetNode.path("options");
        if (!options.isMissingNode()) {
            String name = options.path("name").asText();
            String label = options.path("label").asText();
            JsonNode optionItems = options.path("optionItems");

            List<OptionItemDTO> optionList = new ArrayList<>();
            if (optionItems.isArray()) {
                for (JsonNode item : optionItems) {
                    optionList.add(new OptionItemDTO(
                            item.path("label").asText(),
                            item.path("value").asInt(),
                            0
                    ));
                }
            }

            extractedData.add(new WidgetDataDTO(name, label, type, optionList, null, null));
        }

        JsonNode cols = widgetNode.path("cols");
        if (cols.isArray()) {
            for (JsonNode col : cols) {
                JsonNode widgetList = col.path("widgetList");
                if (widgetList.isArray()) {
                    for (JsonNode nestedWidget : widgetList) {
                        extractWidgetRecursive(nestedWidget, extractedData);
                    }
                }
            }
        }
    }

    private List<String> generateCollectionNames(Long formTemplateId, Timestamp utcStartDateTime, Timestamp utcEndDateTime) {
        List<String> collectionNames = new ArrayList<>();
        MongoDatabase database = mongoClient.getDatabase(mongoDatabaseName);

        // Convert timestamps to YYYYMM format
        SimpleDateFormat yearMonthFormat = new SimpleDateFormat("yyyyMM");
        int startYearMonth = Integer.parseInt(yearMonthFormat.format(utcStartDateTime));
        int endYearMonth = Integer.parseInt(yearMonthFormat.format(utcEndDateTime));

        for (String collectionName : database.listCollectionNames()) {
            // Extract YYYYMM from collection name
            String pattern = "form_template_" + formTemplateId + "_(\\d{6})";
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(pattern).matcher(collectionName);

            if (matcher.matches()) {
                int collectionYearMonth = Integer.parseInt(matcher.group(1));

                // Check if collection is within the time range
                if (collectionYearMonth >= startYearMonth && collectionYearMonth <= endYearMonth) {
                    collectionNames.add(collectionName);
                }
            }
        }

        return collectionNames;
    }

    private List<WidgetDataDTO> processCollection(
            MongoDatabase database,
            String collectionName,
            List<WidgetDataDTO> widgetDataList,
            Timestamp startDateTime,
            Timestamp endDateTime
    ) {
        MongoCollection<Document> collection = database.getCollection(collectionName);
        List<WidgetDataDTO> updatedWidgets = new ArrayList<>();

        // Fetch all documents in range and filter for latest versions
        List<Document> allDocs = collection.find().into(new ArrayList<>());
        Map<String, Document> latestVersionMap = new HashMap<>();

        for (Document doc : allDocs) {
             Date rawDate = doc.getDate("created_at");
             Instant createdAt = rawDate != null ? rawDate.toInstant() : null;

             if (createdAt == null ||
                 createdAt.isBefore(startDateTime.toInstant()) ||
                 createdAt.isAfter(endDateTime.toInstant())) {
                 continue;
             }

             String groupId = doc.getString("version_group_id");
             Integer version = doc.getInteger("version", 0);

             if (groupId != null) {
                Document existing = latestVersionMap.get(groupId);
                if (existing == null || version > existing.getInteger("version", 0)) {
                    latestVersionMap.put(groupId, doc);
                }
             } else {
                latestVersionMap.put(doc.getObjectId("_id").toString(), doc);
             }
        }
        
        List<Document> validDocs = new ArrayList<>(latestVersionMap.values());
        validDocs.sort(Comparator.comparing(d -> d.getDate("created_at")));

        for (WidgetDataDTO widget : widgetDataList) {
            if (widget.getOptionItems().isEmpty() && !widget.getType().equals("number")) {
                updatedWidgets.add(widget);
                continue;
            }

            if (widget.getType().equals("number")) {
                List<Double> chartData = new ArrayList<>();
                List<String> xaxisData = new ArrayList<>();

                for (Document doc : validDocs) {
                    if (doc.containsKey(widget.getName())) {
                        Object value = doc.get(widget.getName());
                        if (value instanceof Integer) {
                            chartData.add(((Integer) value).doubleValue());
                        } else if (value instanceof Double) {
                            chartData.add((Double) value);
                        }
                        
                        Date rawDate = doc.getDate("created_at");
                        if (rawDate != null) {
                             String formattedDate = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                                     .withZone(ZoneId.of("UTC"))
                                     .format(rawDate.toInstant());
                             xaxisData.add(formattedDate);
                        }
                    }
                }
                updatedWidgets.add(new WidgetDataDTO(widget.getName(), widget.getLabel(), widget.getType(),
                        new ArrayList<>(), chartData, xaxisData));
            }

            if (!widget.getOptionItems().isEmpty()) {
                Map<Integer, Integer> optionCounts = countOptionOccurrences(validDocs, widget.getName(), widget.getOptionItems());

                String bucketType = determineBucketType(startDateTime, endDateTime);
                List<String> bucketLabels = generateBucketLabels(startDateTime, endDateTime, bucketType);

                Map<Integer, List<Integer>> timeBucketedCounts = countOptionOccurrencesByTimeBucket(
                        validDocs, widget.getName(), widget.getOptionItems(),
                        startDateTime, bucketType, bucketLabels.size()
                );

                List<TimeBucketedOptionDTO> timeBucketedData = widget.getOptionItems().stream()
                        .map(option -> new TimeBucketedOptionDTO(
                                option.getLabel(),
                                option.getValue(),
                                timeBucketedCounts.getOrDefault(option.getValue(), Collections.emptyList())
                        ))
                        .collect(Collectors.toList());

                List<OptionItemDTO> updatedOptions = widget.getOptionItems().stream()
                        .map(option -> new OptionItemDTO(
                                option.getLabel(),
                                option.getValue(),
                                optionCounts.getOrDefault(option.getValue(), 0))) 
                        .collect(Collectors.toList());

                WidgetDataDTO newWidget = new WidgetDataDTO(widget.getName(), widget.getLabel(), widget.getType(),
                        updatedOptions, null, null);
                newWidget.setTimeBucketedData(timeBucketedData);
                newWidget.setBucketLabels(bucketLabels);
                newWidget.setBucketType(bucketType);

                updatedWidgets.add(newWidget); 
            }
        }

        return updatedWidgets;
    }

    private Map<String, List<Object>> extractNumberFieldData(
            MongoCollection<Document> collection, String fieldName) {
        List<Object> values = new ArrayList<>();
        List<Object> timestamps = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

        for (Document doc : collection.find(exists(fieldName, true))) {
            if (doc.containsKey(fieldName) && doc.get(fieldName) instanceof Number) {
                values.add(doc.get(fieldName));

                if (doc.containsKey("created_at")) {
                    Object createdAt = doc.get("created_at");

                    if (createdAt instanceof String) {
                        try {
                            LocalDateTime parsedDate = Instant.parse((String) createdAt)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDateTime();
                            timestamps.add(parsedDate.toString().replace("T", " ").substring(0, 16));
                        } catch (Exception e) {
                            System.out.println("Invalid date format: " + createdAt);
                            timestamps.add(createdAt); // Store as-is if parsing fails
                        }
                    }
                }
            }
        }

        Map<String, List<Object>> result = new HashMap<>();
        result.put("values", values);
        result.put("timestamps", timestamps);
        return result;
    }

    private Map<Integer, Integer> countOptionOccurrences(
            List<Document> documents,
            String fieldName,
            List<OptionItemDTO> options
    ) {
        Map<Integer, Integer> countMap = new HashMap<>();

        for (Document doc : documents) {
            if (!doc.containsKey(fieldName)) continue;

            Object val = doc.get(fieldName);
            if (val instanceof List<?>) {
                for (Object item : (List<?>) val) {
                    try {
                        int intVal = Integer.parseInt(item.toString());
                        countMap.put(intVal, countMap.getOrDefault(intVal, 0) + 1);
                    } catch (NumberFormatException ignored) {}
                }
            } else if (val instanceof Integer) {
                int intVal = (Integer) val;
                countMap.put(intVal, countMap.getOrDefault(intVal, 0) + 1);
            }
        }

        return countMap;
    }

    /**
     * Fetch QC records for a given template ID within a date range, with pagination.
     */
    @Override
    public List<Document> fetchQcRecords(Long formTemplateId, String startDateTime, String endDateTime, Integer page, Integer size) {
        MongoDatabase database = mongoClient.getDatabase(mongoDatabaseName);

        // Get label mappings for formTemplateId
        HashMap<String, Object> optionItemsKeyValueMap = QcFormTemplateOptionItemsKeyValueMapping(formTemplateId);
        HashMap<String, String> keyValueMap = getFormTemplateKeyValueMapping(formTemplateId);

        // Get target collections based on the date range
        List<String> collectionNames = getRelevantCollections(database, formTemplateId, startDateTime, endDateTime);

        Map<String, Document> latestVersionMap = new HashMap<>();
        Set<Integer> userIds = new HashSet<>();

        for (String collectionName : collectionNames) {
            MongoCollection<Document> collection = database.getCollection(collectionName);
            List<Document> collectionRecords = queryRecords(collection, startDateTime, endDateTime, page, size);

            for (Document doc : collectionRecords) {
                String groupId = doc.getString("version_group_id");
                Integer version = doc.getInteger("version", 0);

                if (doc.containsKey("created_by") && doc.get("created_by") instanceof Number) {
                    userIds.add(((Number) doc.get("created_by")).intValue());
                }

                if (groupId != null) {
                    Document existing = latestVersionMap.get(groupId);
                    if (existing == null || version > existing.getInteger("version", 0)) {
                        latestVersionMap.put(groupId, doc);
                    }
                } else {
                    // No versioning info, treat as standalone record
                    latestVersionMap.put(doc.getObjectId("_id").toString(), doc);
                }
            }
        }

        // Bulk fetch user names
//        Map<Integer, String> userNameMap = userService.getUsersByIds(new ArrayList<>(userIds)).stream()
//                .collect(Collectors.toMap(u -> u.getId(), u -> u.getName()));

        // Bulk fetch user names from local DB
        Map<Long, String> userNameMap = userRepository.findAllByIdIn(userIds).stream()
                .collect(Collectors.toMap(
                        User::getId,
                        User::getFullName
                ));

        // Convert and return only the latest versions
        return latestVersionMap.values().stream()
                .map(doc -> formattedResult(doc, optionItemsKeyValueMap, keyValueMap, userNameMap))
                .collect(Collectors.toList());
    }

    // another function use above fetchQcRecords but filtered by createdBy integer
    @Override
    public List<Document> fetchQcRecordsFilteredByCreator(Long formTemplateId, String startDateTime, String endDateTime, Integer page, Integer size, Integer createdBy) {
        // 先获取全部记录（分页限制会在这里执行）
        List<Document> allRecords = fetchQcRecords(formTemplateId, startDateTime, endDateTime, page, size);

        // 然后再根据 created_by 进行过滤
        return allRecords.stream()
                .filter(doc -> {
                    Object creator = doc.get("created_by");
                    if (creator instanceof Integer) {
                        return creator.equals(createdBy);
                    } else if (creator instanceof Long) {
                        return ((Long) creator).intValue() == createdBy;
                    } else {
                        return false;
                    }
                })
                .collect(Collectors.toList());
    }

    @Override
    public PagedResultDTO<Document> fetchQcRecordsPaged(
            Long formTemplateId,
            String startDateTime,
            String endDateTime,
            Integer page,
            Integer size,
            String sort,
            String search
    ) {
        MongoDatabase database = mongoClient.getDatabase(mongoDatabaseName);

        // Get label mappings
        HashMap<String, Object> optionItemsKeyValueMap = QcFormTemplateOptionItemsKeyValueMapping(formTemplateId);
        HashMap<String, String> keyValueMap = getFormTemplateKeyValueMapping(formTemplateId);

        // Identify relevant collections
        List<String> collectionNames = getRelevantCollections(database, formTemplateId, startDateTime, endDateTime);

        Instant startInstant = convertStringToInstant(startDateTime);
        Instant endInstant = convertStringToInstant(endDateTime);

        Map<String, Document> latestVersionMap = new HashMap<>();
        Map<String, Document> originalVersionMap = new HashMap<>();
        Map<String, Document> standaloneRecordMap = new HashMap<>();
        Set<Integer> userIds = new HashSet<>();

        for (String collectionName : collectionNames) {
            MongoCollection<Document> collection = database.getCollection(collectionName);

            List<Document> allDocs = collection.find().into(new ArrayList<>());
            for (Document doc : allDocs) {
                String groupId = doc.getString("version_group_id");
                Integer version = doc.getInteger("version", 0);
                Date createdAt = doc.getDate("created_at");

                if (doc.containsKey("created_by") && doc.get("created_by") instanceof Number) {
                    userIds.add(((Number) doc.get("created_by")).intValue());
                }

                if (groupId != null) {
                    Document existingLatest = latestVersionMap.get(groupId);
                    if (existingLatest == null
                            || version > existingLatest.getInteger("version", 0)
                            || (Objects.equals(version, existingLatest.getInteger("version", 0))
                            && createdAt != null
                            && (existingLatest.getDate("created_at") == null || createdAt.after(existingLatest.getDate("created_at"))))) {
                        latestVersionMap.put(groupId, doc);
                    }

                    Document existingOriginal = originalVersionMap.get(groupId);
                    if (existingOriginal == null
                            || version < existingOriginal.getInteger("version", 0)
                            || (Objects.equals(version, existingOriginal.getInteger("version", 0))
                            && createdAt != null
                            && (existingOriginal.getDate("created_at") == null || createdAt.before(existingOriginal.getDate("created_at"))))) {
                        originalVersionMap.put(groupId, doc);
                    }
                } else {
                    if (createdAt != null
                            && !createdAt.toInstant().isBefore(startInstant)
                            && !createdAt.toInstant().isAfter(endInstant)) {
                        standaloneRecordMap.put(doc.getObjectId("_id").toString(), doc);
                    }
                }
            }
        }

        Map<String, Document> includedRecords = new HashMap<>(standaloneRecordMap);
        for (Map.Entry<String, Document> entry : latestVersionMap.entrySet()) {
            String groupId = entry.getKey();
            Document latestDoc = entry.getValue();
            Document originalDoc = originalVersionMap.get(groupId);
            Date originalCreatedAt = originalDoc != null ? originalDoc.getDate("created_at") : null;

            if (originalCreatedAt != null
                    && !originalCreatedAt.toInstant().isBefore(startInstant)
                    && !originalCreatedAt.toInstant().isAfter(endInstant)) {
                includedRecords.put(groupId, latestDoc);
            }
        }

        // Bulk fetch user names
//        Map<Integer, String> userNameMap = userService.getUsersByIds(new ArrayList<>(userIds)).stream()
//                .collect(Collectors.toMap(u -> u.getId(), u -> u.getName()));
        Map<Long, String> userNameMap =
                userRepository.findAllByIdIn(userIds).stream()
                        .collect(Collectors.toMap(
                                User::getId,
                                User::getFullName
                        ));

        // Stream Pipeline: Format -> Filter -> Sort
        Stream<Document> stream = includedRecords.values().parallelStream()
                .map(doc -> formattedResult(doc, optionItemsKeyValueMap, keyValueMap, userNameMap));

        // 2. Filter (search) on visible field values only (excluding metadata fields)
        if (search != null && !search.isEmpty()) {
            String lower = search.toLowerCase();
            stream = stream.filter(doc -> containsSearchInVisibleFields(doc, lower));
        }

        // 3. Sorting (default to created_at descending if no sort specified)
        String effectiveSort = (sort == null || sort.isEmpty()) ? "created_at,desc" : sort;
        if (effectiveSort.contains(",")) {
            String[] parts = effectiveSort.split(",", 2);
            String field = parts[0];
            boolean desc = "desc".equalsIgnoreCase(parts[1]);
            Comparator<Document> cmp;
            if ("created_at".equals(field)) {
                cmp = Comparator.comparing(d -> Optional.ofNullable(d.getDate("created_at")).orElse(new Date(0)));
            } else {
                cmp = Comparator.comparing(d -> Optional.ofNullable(d.get(field)).map(Object::toString).orElse(""));
            }
            if (desc) cmp = cmp.reversed();
            stream = stream.sorted(cmp);
        }

        // 4. Pagination
        List<Document> filtered = stream.collect(Collectors.toList());
        long total = filtered.size();
        int from = page * size;
        int to = Math.min(from + size, filtered.size());
        List<Document> pageContent = (from >= filtered.size())
                ? Collections.emptyList()
                : filtered.subList(from, to);

        int totalPages = (int) Math.ceil((double) total / size);
        return new PagedResultDTO<>(
                pageContent,
                total,
                totalPages,
                page,
                size
        );
    }


    /**
     * 将 "field,asc|desc" 转换为 MongoDB 排序 Bson
     */
    private Bson parseSortBson(String sort) {
        if (sort == null || !sort.contains(",")) {
            return ascending("_id");
        }
        String[] parts = sort.split(",", 2);
        return "desc".equalsIgnoreCase(parts[1])
                ? descending(parts[0])
                : ascending(parts[0]);
    }

    @Override
    public List<Document> fetchAllRecordsWithoutPagination(Long formTemplateId,
                                                           String startDateTime,
                                                           String endDateTime,
                                                           String search,
                                                           String sort) {

        MongoDatabase database = mongoClient.getDatabase(mongoDatabaseName);

        // label / optionItems 映射
        HashMap<String, Object> optionItemsKeyValueMap = QcFormTemplateOptionItemsKeyValueMapping(formTemplateId);
        HashMap<String, String> keyValueMap            = getFormTemplateKeyValueMapping(formTemplateId);

        // 找到时间范围内相关集合
        List<String> collectionNames = getRelevantCollections(database, formTemplateId,
                startDateTime, endDateTime);

        Instant startInstant = convertStringToInstant(startDateTime);
        Instant endInstant = convertStringToInstant(endDateTime);

        /** ---------- 1. 取最新版本 ---------- */
        Map<String, Document> latestVersionMap = new HashMap<>();
        Map<String, Document> originalVersionMap = new HashMap<>();
        Map<String, Document> standaloneRecordMap = new HashMap<>();
        Set<Integer> userIds = new HashSet<>();

        for (String colName : collectionNames) {
            MongoCollection<Document> col = database.getCollection(colName);

            List<Document> docs = col.find().into(new ArrayList<>());

            for (Document d : docs) {
                String gid      = d.getString("version_group_id");
                Integer version = d.getInteger("version", 0);
                Date createdAt = d.getDate("created_at");

                if (d.containsKey("created_by") && d.get("created_by") instanceof Number) {
                    userIds.add(((Number) d.get("created_by")).intValue());
                }

                if (gid != null) {
                    Document existingLatest = latestVersionMap.get(gid);
                    if (existingLatest == null
                            || version > existingLatest.getInteger("version", 0)
                            || (Objects.equals(version, existingLatest.getInteger("version", 0))
                            && createdAt != null
                            && (existingLatest.getDate("created_at") == null || createdAt.after(existingLatest.getDate("created_at"))))) {
                        latestVersionMap.put(gid, d);
                    }

                    Document existingOriginal = originalVersionMap.get(gid);
                    if (existingOriginal == null
                            || version < existingOriginal.getInteger("version", 0)
                            || (Objects.equals(version, existingOriginal.getInteger("version", 0))
                            && createdAt != null
                            && (existingOriginal.getDate("created_at") == null || createdAt.before(existingOriginal.getDate("created_at"))))) {
                        originalVersionMap.put(gid, d);
                    }
                } else {
                    if (createdAt != null
                            && !createdAt.toInstant().isBefore(startInstant)
                            && !createdAt.toInstant().isAfter(endInstant)) {
                        standaloneRecordMap.put(d.getObjectId("_id").toString(), d);
                    }
                }
            }
        }

        Map<String, Document> includedRecords = new HashMap<>(standaloneRecordMap);
        for (Map.Entry<String, Document> entry : latestVersionMap.entrySet()) {
            String groupId = entry.getKey();
            Document latestDoc = entry.getValue();
            Document originalDoc = originalVersionMap.get(groupId);
            Date originalCreatedAt = originalDoc != null ? originalDoc.getDate("created_at") : null;

            if (originalCreatedAt != null
                    && !originalCreatedAt.toInstant().isBefore(startInstant)
                    && !originalCreatedAt.toInstant().isAfter(endInstant)) {
                includedRecords.put(groupId, latestDoc);
            }
        }

        // Bulk fetch user names
//        Map<Integer, String> userNameMap = userService.getUsersByIds(new ArrayList<>(userIds)).stream()
//                .collect(Collectors.toMap(u -> u.getId(), u -> u.getName()));
        Map<Long, String> userNameMap =
                userRepository.findAllByIdIn(userIds).stream()
                        .collect(Collectors.toMap(
                                User::getId,
                                User::getFullName
                        ));

        /** ---------- 2. Pipeline: Format -> Filter -> Sort ---------- */
        Stream<Document> stream = includedRecords.values().parallelStream()
                .map(doc -> formattedResult(doc, optionItemsKeyValueMap, keyValueMap, userNameMap));

        if (search != null && !search.isEmpty()) {
            String kw = search.toLowerCase();
            stream = stream.filter(doc -> containsSearchInVisibleFields(doc, kw));
        }

        /** ---------- 3. 排序 (default to created_at descending if no sort specified) ---------- */
        String effectiveSort = (sort == null || sort.isEmpty()) ? "created_at,desc" : sort;
        if (effectiveSort.contains(",")) {
            String[] parts   = effectiveSort.split(",", 2);
            String field     = parts[0];
            boolean desc     = "desc".equalsIgnoreCase(parts[1]);

            Comparator<Document> cmp;
            if ("created_at".equals(field)) {
                cmp = Comparator.comparing(d ->
                        Optional.ofNullable(d.getDate("created_at")).orElse(new Date(0)));
            } else {
                cmp = Comparator.comparing(d ->
                        Optional.ofNullable(d.get(field)).map(Object::toString).orElse(""));
            }
            if (desc) cmp = cmp.reversed();
            stream = stream.sorted(cmp);
        }

        /** ---------- 4. 最终结果 ---------- */
        return stream.collect(Collectors.toList());
    }

    @Override
    public List<Document> fetchAllVersionsByGroupId(Long formTemplateId, String versionGroupId) {
        MongoDatabase database = mongoClient.getDatabase(mongoDatabaseName);

        HashMap<String, Object> optionItemsKeyValueMap = QcFormTemplateOptionItemsKeyValueMapping(formTemplateId);
        HashMap<String, String> keyValueMap = getFormTemplateKeyValueMapping(formTemplateId);

        List<Document> versionedDocs = new ArrayList<>();
        Set<Integer> userIds = new HashSet<>();

        // Loop over all relevant collections in here
        for (String collectionName : database.listCollectionNames()) {
            if (!collectionName.startsWith("form_template_" + formTemplateId + "_")) continue;

            MongoCollection<Document> collection = database.getCollection(collectionName);

            List<Document> matches = collection.find(eq("version_group_id", versionGroupId)).into(new ArrayList<>());
            for (Document d : matches) {
                if (d.containsKey("created_by") && d.get("created_by") instanceof Number) {
                    userIds.add(((Number) d.get("created_by")).intValue());
                }
            }
            versionedDocs.addAll(matches);
        }

        versionedDocs.sort((a, b) -> {
            Integer v1 = a.getInteger("version", 0);
            Integer v2 = b.getInteger("version", 0);
            return v2.compareTo(v1); // latest first
        });

        // Bulk fetch user names
//        Map<Integer, String> userNameMap = userService.getUsersByIds(new ArrayList<>(userIds)).stream()
//                .collect(Collectors.toMap(u -> u.getId(), u -> u.getName()));

        Map<Long, String> userNameMap =
                userRepository.findAllByIdIn(userIds).stream()
                        .collect(Collectors.toMap(
                                User::getId,
                                User::getFullName
                        ));

        return versionedDocs.stream()
                .map(doc -> formattedResult(doc, optionItemsKeyValueMap, keyValueMap, userNameMap))
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> debugTemplateData(Long formTemplateId, String startDateTime, String endDateTime) {
        MongoDatabase database = mongoClient.getDatabase(mongoDatabaseName);
        Map<String, Object> result = new LinkedHashMap<>();

        // 1. Key-value map (includes deleted field mappings after fix)
        HashMap<String, String> keyValueMap = getFormTemplateKeyValueMapping(formTemplateId);
        result.put("keyValueMap", keyValueMap);

        // 2. Relevant collections
        List<String> collections = getRelevantCollections(database, formTemplateId, startDateTime, endDateTime);
        result.put("relevantCollections", collections);

        // 3. Raw keys from first document in each collection
        Map<String, Object> rawKeysPerCollection = new LinkedHashMap<>();
        for (String colName : collections) {
            MongoCollection<Document> col = database.getCollection(colName);
            Document firstDoc = col.find().first();
            rawKeysPerCollection.put(colName, firstDoc != null ? new ArrayList<>(firstDoc.keySet()) : "empty");
        }
        result.put("rawDocumentKeys", rawKeysPerCollection);

        return result;
    }

    // TODO: use MongoFormTemplateUtils
    public HashMap<String, String> getFormTemplateKeyValueMapping(Long formId) {
        String formTemplateJson = qcFormTemplateRepository.findFormTemplateJsonById(formId);

        if (formTemplateJson == null || formTemplateJson.isEmpty()) {
            throw new RuntimeException("Form template JSON not found for formId: " + formId);
        }

        HashMap<String, String> keyValueMap = new HashMap<>();

        try {
            Document formTemplate = Document.parse(formTemplateJson);
            List<Document> widgetList = (List<Document>) formTemplate.get("widgetList");

            if (widgetList != null) {
                extractKeyValuePairs(widgetList, keyValueMap); // Extract key-label mappings
            }
        } catch (Exception e) {
            throw new RuntimeException("Error parsing form template JSON", e);
        }

        // Supplement with soft-deleted field mappings so historical records that still
        // contain deleted field data get their keys resolved to the correct labels.
        try {
            MongoDatabase database = mongoClient.getDatabase(mongoDatabaseName);
            MongoCollection<Document> pairsCollection = database.getCollection("form_template_key_label_pairs");
            Document pairsDoc = pairsCollection.find(new Document("qc_form_template_id", formId)).first();
            if (pairsDoc != null && pairsDoc.containsKey("fields")) {
                List<Document> fields = pairsDoc.getList("fields", Document.class);
                for (Document field : fields) {
                    String key = field.getString("key");
                    String label = field.getString("label");
                    // Only add if not already mapped by the active template (active fields take precedence)
                    if (key != null && label != null && !keyValueMap.containsKey(key)) {
                        keyValueMap.put(key, label);
                    }
                }
            }
        } catch (Exception e) {
            // Non-fatal: deleted field labels won't resolve, but active fields still work
        }

        return keyValueMap;
    }

    private void extractKeyValuePairs(List<Document> widgetList, HashMap<String, String> keyValueMap) {
        for (Document widget : widgetList) {
            // Extract options and add name-label pairs to the map
            Document options = (Document) widget.get("options");
            if (options != null) {
                String name = options.getString("name");
                String label = options.getString("label");
                if (name != null && label != null) {
                    keyValueMap.put(name, label);
                }
            }

            // Recursively process nested widgetList
            List<Document> nestedWidgetList = (List<Document>) widget.get("widgetList");
            if (nestedWidgetList != null) {
                extractKeyValuePairs(nestedWidgetList, keyValueMap);
            }

            // Check for widget lists inside grid columns
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

    // TODO: use MongoFormTemplateUtils
    private Document formattedResult(Document document, HashMap<String, Object> optionItemsKeyValueMap, HashMap<String, String> keyValueMap, Map<Long, String> userNameMap) {
        Document formattedDocument = new Document();

        for (String key : document.keySet()) {
            Object value = document.get(key);

            // 确保 `_id` 仍然是 ObjectId 而不是 timestamp + date 结构
            if ("_id".equals(key) && value instanceof ObjectId) {
                formattedDocument.put("_id", value.toString()); // 转换成字符串
                continue;
            }

            // 替换字段名
            String formattedKey = keyValueMap.getOrDefault(key, key);

            // 如果 value 是选项列表，则转换成对应的 label
            if (optionItemsKeyValueMap.containsKey(key) && value instanceof List) {
                List<?> valueList = (List<?>) value;
                HashMap<String, String> valueToLabelMap = (HashMap<String, String>) optionItemsKeyValueMap.get(key);

                List<String> resolvedLabels = valueList.stream()
                        .map(val -> val != null ? valueToLabelMap.getOrDefault(val.toString(), val.toString()) : null)
                        .collect(Collectors.toList());
                formattedDocument.put(formattedKey, resolvedLabels);
            }
            // 如果 value 有 label 映射，则转换（不限制类型）
            else if (optionItemsKeyValueMap.containsKey(key) && value != null) {
                HashMap<String, String> valueToLabelMap = (HashMap<String, String>) optionItemsKeyValueMap.get(key);
                formattedDocument.put(formattedKey, valueToLabelMap.getOrDefault(value.toString(), value.toString()));
            } else {
                formattedDocument.put(formattedKey, value);
            }
        }

        // 获取 `created_by` 并添加 `created_by_name` using bulk map
        if (document.containsKey("created_by") && document.get("created_by") instanceof Number) {
            Number createdByIdNum = (Number) document.get("created_by");

            // Match with long type as userNameMap key is long to avoid same number mismatch issue
            Long createdById = createdByIdNum.longValue();
            String creatorName = userNameMap.getOrDefault(createdById, "未知用户");
            formattedDocument.put("提交人", creatorName); // 添加 `created_by_name`
        }

        // 🔧 Remap exceeded_info keys from fieldName to fieldLabel
        if (document.containsKey("exceeded_info")) {
            Document originalExceededInfo = (Document) document.get("exceeded_info");
            Document labeledExceededInfo = new Document();

            for (String rawField : originalExceededInfo.keySet()) {
                String labeledField = keyValueMap.getOrDefault(rawField, rawField); // name → label
                labeledExceededInfo.put(labeledField, originalExceededInfo.get(rawField));
            }

            formattedDocument.put("exceeded_info", labeledExceededInfo);
        }

        return formattedDocument;
    }

    /**
     * Check if any visible field in the document contains the search keyword.
     * Excludes metadata fields like created_by, created_at, exceeded_info, approval_info, e-signature,
     * and fields ending with _id or _ids (like related_team_id, related_inspector_ids).
     * Note: _id (Submission ID) is searchable as it's displayed in the table.
     */
    private boolean containsSearchInVisibleFields(Document doc, String searchLower) {
        Set<String> excludedFields = Set.of(
                "created_by", "created_at", "exceeded_info", "approval_info", "e-signature",
                "version", "version_group_id", "approver_updated_at"
        );

        for (String key : doc.keySet()) {
            // Skip excluded fields
            if (excludedFields.contains(key)) {
                continue;
            }
            // Skip fields ending with _id or _ids (like related_team_id, related_inspector_ids)
            // But allow "_id" itself (Submission ID is searchable)
            if (!key.equals("_id") && (key.endsWith("_id") || key.endsWith("_ids"))) {
                continue;
            }

            Object value = doc.get(key);
            if (value == null) {
                continue;
            }

            // Convert value to string and check if it contains the search term
            String valueStr;
            if (value instanceof List) {
                valueStr = ((List<?>) value).stream()
                        .map(Object::toString)
                        .collect(Collectors.joining(" "));
            } else {
                valueStr = value.toString();
            }

            if (valueStr.toLowerCase().contains(searchLower)) {
                return true;
            }
        }
        return false;
    }

    // TODO: use MongoFormTemplateUtils
    private HashMap<String, Object> QcFormTemplateOptionItemsKeyValueMapping(Long formId) {
        String formTemplateJson = qcFormTemplateRepository.findFormTemplateJsonById(formId);

        if (formTemplateJson == null || formTemplateJson.isEmpty()) {
            throw new RuntimeException("Form template JSON not found for formId: " + formId);
        }

        HashMap<String, Object> optionItemsKeyValueMap = new HashMap<>();

        try {
            Document formTemplate = Document.parse(formTemplateJson);
            List<Document> widgetList = (List<Document>) formTemplate.get("widgetList");

            if (widgetList != null) {
                extractOptionItems(widgetList, optionItemsKeyValueMap);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error parsing form template JSON", e);
        }

        // Supplement with historical option items from form_template_key_label_pairs for deleted fields
        try {
            MongoDatabase database = mongoClient.getDatabase(mongoDatabaseName);
            MongoCollection<Document> pairsCollection = database.getCollection("form_template_key_label_pairs");
            Document pairsDoc = pairsCollection.find(eq("qc_form_template_id", formId)).first();
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
            // Non-critical: log and continue with whatever was resolved from current template
        }

        // Final fallback: control_limit_setting.control_limits[field].optionItems
        // This is always kept up-to-date (including deleted fields) by mergeControlLimitSettings.
        try {
            MongoDatabase database = mongoClient.getDatabase(mongoDatabaseName);
            MongoCollection<Document> clsCollection = database.getCollection("control_limit_setting");
            Document clsDoc = clsCollection.find(eq("qc_form_template_id", formId)).first();
            if (clsDoc != null && clsDoc.containsKey("control_limits")) {
                Document controlLimits = (Document) clsDoc.get("control_limits");
                for (String fieldKey : controlLimits.keySet()) {
                    if (!optionItemsKeyValueMap.containsKey(fieldKey)) {
                        Object entry = controlLimits.get(fieldKey);
                        if (entry instanceof Document) {
                            List<Document> optionItems = ((Document) entry).getList("optionItems", Document.class);
                            if (optionItems != null && !optionItems.isEmpty()) {
                                HashMap<String, String> valueToLabel = new HashMap<>();
                                for (Document item : optionItems) {
                                    Object val = item.get("value");
                                    String lbl = item.getString("label");
                                    if (val != null && lbl != null) {
                                        valueToLabel.put(val.toString(), lbl);
                                    }
                                }
                                if (!valueToLabel.isEmpty()) {
                                    optionItemsKeyValueMap.put(fieldKey, valueToLabel);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Non-critical
        }

        return optionItemsKeyValueMap;
    }

    private void extractOptionItems(List<Document> widgetList, HashMap<String, Object> optionItemsKeyValueMap) {
        for (Document widget : widgetList) {
            // Extract options with optionItems
            Document options = (Document) widget.get("options");
            if (options != null) {
                String name = options.getString("name");
                List<Document> optionItems = (List<Document>) options.get("optionItems");

                if (name != null && optionItems != null) {
                    HashMap<String, String> valueToLabelMap = new HashMap<>();
                    for (Document option : optionItems) {
                        Object value = option.get("value");
                        String optionLabel = option.getString("label");
                        if (value != null && optionLabel != null) {
                            valueToLabelMap.put(value.toString(), optionLabel);
                        }
                    }
                    optionItemsKeyValueMap.put(name, valueToLabelMap);
                }
            }

            // Recursively process nested widgetList
            List<Document> nestedWidgetList = (List<Document>) widget.get("widgetList");
            if (nestedWidgetList != null) {
                extractOptionItems(nestedWidgetList, optionItemsKeyValueMap);
            }

            // Check for widget lists inside grid columns
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

    /**
     * Identify the collections relevant to the given date range.
     */
    private List<String> getRelevantCollections(MongoDatabase database, Long formTemplateId, String startDateTime, String endDateTime) {
        List<String> targetCollections = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMM");

        LocalDateTime start = LocalDateTime.parse(startDateTime, DATE_FORMATTER);
        LocalDateTime end = LocalDateTime.parse(endDateTime, DATE_FORMATTER);

        while (!start.isAfter(end)) {
            String collectionName = "form_template_" + formTemplateId + "_" + sdf.format(java.sql.Timestamp.valueOf(start));
            if (database.listCollectionNames().into(new ArrayList<>()).contains(collectionName)) {
                targetCollections.add(collectionName);
            }
            start = start.plusMonths(1); // Move to next month
        }
        return targetCollections;
    }

    private String determineBucketType(Timestamp start, Timestamp end) {
        long diffDays = ChronoUnit.DAYS.between(
                start.toLocalDateTime().toLocalDate(),
                end.toLocalDateTime().toLocalDate()
        );

        if (diffDays < 7) {
            return "hourly";
        } else if (diffDays <= 30) {
            return "daily";
        } else {
            return "weekly";
        }
    }

    private List<String> generateBucketLabels(Timestamp start, Timestamp end, String bucketType) {
        List<String> labels = new ArrayList<>();
        ZonedDateTime current = start.toInstant().atZone(ZoneOffset.UTC);
        ZonedDateTime endTime = end.toInstant().atZone(ZoneOffset.UTC);

        while (!current.isAfter(endTime)) {
            labels.add(current.format(DateTimeFormatter.ISO_INSTANT));

            switch (bucketType) {
                case "hourly": current = current.plusHours(1); break;
                case "daily": current = current.plusDays(1); break;
                case "weekly": current = current.plusWeeks(1); break;
            }
        }

        return labels;
    }

    private Map<Integer, List<Integer>> countOptionOccurrencesByTimeBucket(
            List<Document> documents,
            String fieldName,
            List<OptionItemDTO> options,
            Timestamp startDateTime,
            String bucketType,
            int bucketCount
    ) {
        Map<Integer, List<Integer>> result = new HashMap<>();
        for (OptionItemDTO option : options) {
            List<Integer> counts = new ArrayList<>(Collections.nCopies(bucketCount, 0));
            result.put(option.getValue(), counts);
        }

        for (Document doc : documents) {
            if (!doc.containsKey(fieldName)) continue;

            Date rawDate = doc.getDate("created_at");
            if (rawDate == null) continue;

            Instant createdAt = rawDate.toInstant();
            // Start/End date check assumed done in caller, but bucket calculation needs startDateTime
            
            int bucketIndex = calculateBucketIndex(
                    createdAt, startDateTime.toInstant(), bucketType
            );
            if (bucketIndex < 0 || bucketIndex >= bucketCount) continue;

            Object value = doc.get(fieldName);
            if (value instanceof Integer) {
                List<Integer> counts = result.get((Integer) value);
                if (counts != null) {
                    counts.set(bucketIndex, counts.get(bucketIndex) + 1);
                }
            } else if (value instanceof List<?>) {
                for (Object item : (List<?>) value) {
                    try {
                        int intVal = Integer.parseInt(item.toString());
                        List<Integer> counts = result.get(intVal);
                        if (counts != null) {
                            counts.set(bucketIndex, counts.get(bucketIndex) + 1);
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
        }

        return result;
    }

    private int calculateBucketIndex(Instant timestamp, Instant start, String bucketType) {
        long diff;
        // Convert to ZonedDateTime for correct ChronoUnit calculation, especially for WEEKS
        ZonedDateTime startTime = start.atZone(ZoneId.systemDefault());
        ZonedDateTime targetTime = timestamp.atZone(ZoneId.systemDefault());

        switch (bucketType) {
            case "hourly":
                diff = ChronoUnit.HOURS.between(startTime, targetTime);
                break;
            case "daily":
                diff = ChronoUnit.DAYS.between(startTime, targetTime);
                break;
            case "weekly":
                diff = ChronoUnit.WEEKS.between(startTime, targetTime);
                break;
            default:
                diff = ChronoUnit.DAYS.between(startTime, targetTime);
        }
        return (int) diff;
    }

    private List<Document> queryRecords(MongoCollection<Document> collection, String startDateTime, String endDateTime, int page, int size) {
        Instant startInstant = convertStringToInstant(startDateTime);
        Instant endInstant = convertStringToInstant(endDateTime);

//
        System.out.println("Querying records with range: " + startInstant + " to " + endInstant);
        List<Document> allRecords = collection.find().into(new ArrayList<>());
        List<Document> filtered = new ArrayList<>();

        for (Document doc : allRecords) {
            Date rawDate = doc.getDate("created_at");
            Instant createdAt = rawDate != null ? rawDate.toInstant() : null;

            if (createdAt != null && !createdAt.isBefore(startInstant) && !createdAt.isAfter(endInstant)) {
                filtered.add(doc);
            }
        }

        return filtered.stream()
                .skip(page * size)
                .limit(size)
                .collect(Collectors.toList());
    }

    private Instant convertStringToInstant(String dateTime) {
        try {
            // Expected format: "yyyy-MM-dd HH:mm:ss"
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime localDateTime = LocalDateTime.parse(dateTime, formatter);
            return localDateTime.atZone(ZoneOffset.UTC).toInstant();
        } catch (DateTimeParseException e) {
            // Fallback if Swagger or frontend passes "2025-06-01T14:00:00Z"
            return Instant.parse(dateTime);
        }
    }

    private Instant convertMongoCreatedAtStringToInstant(String mongoDateTime) {
        try {
            if (mongoDateTime.length() >= 29) { // nanosecond precision
                return Instant.parse(mongoDateTime.substring(0, 26) + "Z"); // truncate to microsecond precision
            } else if (mongoDateTime.length() >= 26) {
                return Instant.parse(mongoDateTime.substring(0, 26) + "Z");
            } else {
                return Instant.parse(mongoDateTime + "Z");
            }
        } catch (Exception e) {
            System.out.println("Error parsing MongoDB created_at: " + mongoDateTime + " - " + e.getMessage());
            return null;
        }
    }


    /**
     * ✅ Converts "yyyy-MM-dd HH:mm:ss" to MongoDB's "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS" format.
     */
    private String convertToMongoDateTimeFormat(String dateTime) {
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS");

        LocalDateTime localDateTime = LocalDateTime.parse(dateTime, inputFormatter);
        return localDateTime.atZone(ZoneId.systemDefault()).format(outputFormatter);
    }

    private String convertToUtcString(String localDateTimeString) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        ZonedDateTime localDateTime = ZonedDateTime.of(LocalDateTime.parse(localDateTimeString, formatter), ZoneId.systemDefault());
        // ZonedDateTime utcDateTime = localDateTime.withZoneSameInstant(ZoneId.of("UTC"));
        // convert to shanghai datetime
        ZonedDateTime shangHaiDateTime = localDateTime.withZoneSameInstant(ZoneId.of("Asia/Shanghai"));
        return shangHaiDateTime.format(formatter);
    }

//    // with time conversion
//    private List<Document> queryRecords(MongoCollection<Document> collection, String startDateTime, String endDateTime, int page, int size) {
//        // ✅ Step 1: Convert "yyyy-MM-dd HH:mm:ss" to `Instant`
//        Instant startInstant = convertStringToInstant(startDateTime);
//        Instant endInstant = convertStringToInstant(endDateTime);
//
//        System.out.println("Querying records with range: " + startInstant + " to " + endInstant);
//
//        // ✅ Step 2: Retrieve all documents from the collection
//        List<Document> allRecords = collection.find().into(new ArrayList<>());
//
//        // ✅ Step 3: Convert `created_at` field from String to `Instant` and add a new field `created_at_time`
//        List<Document> enrichedRecords = new ArrayList<>();
//        for (Document doc : allRecords) {
//            if (doc.containsKey("created_at") && doc.get("created_at") instanceof String) {
//                try {
//                    Instant createdAtInstant = convertMongoCreatedAtStringToInstant(doc.getString("created_at"));
//                    doc.put("created_at_time", createdAtInstant); // Add a new Instant field
//                    enrichedRecords.add(doc);
//                } catch (Exception e) {
//                    System.out.println("Error converting created_at: " + doc.get("created_at") + " - " + e.getMessage());
//                }
//            }
//        }
//
//        // ✅ Step 4: Filter records based on `created_at_time`
//        return enrichedRecords.stream()
//                .filter(doc -> {
//                    Instant createdAt = doc.get("created_at_time", Instant.class);
//                    return createdAt != null && !createdAt.isBefore(startInstant) && !createdAt.isAfter(endInstant);
//                })
//                .skip(page * size) // Apply pagination
//                .limit(size)
//                .collect(Collectors.toList());
//    }
//
//    /**
//     * ✅ Converts "yyyy-MM-dd HH:mm:ss" to `Instant`
//     */
//    private Instant convertStringToInstant(String dateTime) {
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
//        LocalDateTime localDateTime = LocalDateTime.parse(dateTime, formatter);
//
//        // Convert to UTC Instant
//        return localDateTime.atZone(ZoneId.systemDefault()).toInstant();
//    }
//
//    /**
//     * ✅ Converts MongoDB's `created_at` format ("2025-01-22T19:34:02.966394038") to `Instant`
//     */
//    private Instant convertMongoCreatedAtStringToInstant(String mongoDateTime) {
//        try {
//            // Handle timestamps with 6 or 9 decimal places dynamically
//            DateTimeFormatter formatter;
//            if (mongoDateTime.length() == 29) { // 9 decimal places (e.g., 2025-01-22T19:34:02.966394038)
//                formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS");
//            } else if (mongoDateTime.length() == 26) { // 6 decimal places (e.g., 2025-02-28T13:49:31.535097)
//                formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");
//            } else {
//                throw new IllegalArgumentException("Unexpected created_at format: " + mongoDateTime);
//            }
//
//            return LocalDateTime.parse(mongoDateTime, formatter).atZone(ZoneId.systemDefault()).toInstant();
//        } catch (Exception e) {
//            System.out.println("Error parsing MongoDB created_at: " + mongoDateTime + " - " + e.getMessage());
//            return null;
//        }
//    }
//
//    private String convertToUtcString(String localDateTimeString) {
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
//        ZonedDateTime localDateTime = ZonedDateTime.of(LocalDateTime.parse(localDateTimeString, formatter), ZoneId.systemDefault());
//        ZonedDateTime utcDateTime = localDateTime.withZoneSameInstant(ZoneId.of("UTC"));
//        // convert to shanghai datetime
////        ZonedDateTime shangHaiDateTime = localDateTime.withZoneSameInstant(ZoneId.of("Asia/Shanghai"));
//        return utcDateTime.format(formatter);
//    }

    @Override
    public PagedResultDTO<Document> fetchDrilldownRecords(
            Long formTemplateId,
            String fieldName,
            Integer optionValue,
            String startDateTime,
            String endDateTime,
            String bucketStart,
            String bucketEnd,
            Integer page,
            Integer size,
            String sort,
            String search
    ) {
        MongoDatabase database = mongoClient.getDatabase(mongoDatabaseName);

        // Get label mappings
        HashMap<String, Object> optionItemsKeyValueMap = QcFormTemplateOptionItemsKeyValueMapping(formTemplateId);
        HashMap<String, String> keyValueMap = getFormTemplateKeyValueMapping(formTemplateId);

        // Determine the effective time range for filtering
        String effectiveStartDateTime = (bucketStart != null && !bucketStart.isEmpty()) ? bucketStart : startDateTime;
        String effectiveEndDateTime = (bucketEnd != null && !bucketEnd.isEmpty()) ? bucketEnd : endDateTime;

        // Identify relevant collections
        List<String> collectionNames = getRelevantCollections(database, formTemplateId, startDateTime, endDateTime);

        Map<String, Document> latestVersionMap = new HashMap<>();
        Set<Integer> userIds = new HashSet<>();

        Instant filterStart = convertStringToInstant(effectiveStartDateTime);
        Instant filterEnd = convertStringToInstant(effectiveEndDateTime);

        for (String collectionName : collectionNames) {
            MongoCollection<Document> collection = database.getCollection(collectionName);

            // Get all documents and filter by date range and field value
            List<Document> allDocs = collection.find().into(new ArrayList<>());
            for (Document doc : allDocs) {
                Date rawDate = doc.getDate("created_at");
                if (rawDate == null) continue;

                Instant createdAt = rawDate.toInstant();

                // Filter by effective time range
                if (createdAt.isBefore(filterStart) || createdAt.isAfter(filterEnd)) {
                    continue;
                }

                // Filter by field value if optionValue is provided
                if (optionValue != null && fieldName != null && !fieldName.isEmpty()) {
                    Object fieldValue = doc.get(fieldName);
                    boolean matches = false;

                    if (fieldValue instanceof Integer) {
                        matches = fieldValue.equals(optionValue);
                    } else if (fieldValue instanceof List<?>) {
                        for (Object item : (List<?>) fieldValue) {
                            if (item instanceof Integer && item.equals(optionValue)) {
                                matches = true;
                                break;
                            } else if (item instanceof String && Integer.parseInt((String) item) == optionValue) {
                                matches = true;
                                break;
                            }
                        }
                    }

                    if (!matches) {
                        continue;
                    }
                }

                // Collect user IDs for name lookup
                if (doc.containsKey("created_by") && doc.get("created_by") instanceof Number) {
                    userIds.add(((Number) doc.get("created_by")).intValue());
                }

                // Keep only latest version
                String groupId = doc.getString("version_group_id");
                Integer version = doc.getInteger("version", 0);

                if (groupId != null) {
                    Document existing = latestVersionMap.get(groupId);
                    if (existing == null || version > existing.getInteger("version", 0)) {
                        latestVersionMap.put(groupId, doc);
                    }
                } else {
                    latestVersionMap.put(doc.getObjectId("_id").toString(), doc);
                }
            }
        }

        // Bulk fetch user names
//        Map<Integer, String> userNameMap = userService.getUsersByIds(new ArrayList<>(userIds)).stream()
//                .collect(Collectors.toMap(u -> u.getId(), u -> u.getName()));

        Map<Long, String> userNameMap =
                userRepository.findAllByIdIn(userIds).stream()
                        .collect(Collectors.toMap(
                                User::getId,
                                User::getFullName
                        ));

        // Stream Pipeline: Format -> Filter -> Sort
        Stream<Document> stream = latestVersionMap.values().parallelStream()
                .map(doc -> formattedResult(doc, optionItemsKeyValueMap, keyValueMap, userNameMap));

        // Apply search filter on visible fields
        if (search != null && !search.isEmpty()) {
            String lower = search.toLowerCase();
            stream = stream.filter(doc -> containsSearchInVisibleFields(doc, lower));
        }

        // Sorting (default to created_at descending if no sort specified)
        String effectiveSort = (sort == null || sort.isEmpty()) ? "created_at,desc" : sort;
        if (effectiveSort.contains(",")) {
            String[] parts = effectiveSort.split(",", 2);
            String field = parts[0];
            boolean desc = "desc".equalsIgnoreCase(parts[1]);
            Comparator<Document> cmp;
            if ("created_at".equals(field)) {
                cmp = Comparator.comparing(d -> Optional.ofNullable(d.getDate("created_at")).orElse(new Date(0)));
            } else {
                cmp = Comparator.comparing(d -> Optional.ofNullable(d.get(field)).map(Object::toString).orElse(""));
            }
            if (desc) cmp = cmp.reversed();
            stream = stream.sorted(cmp);
        }

        // Pagination
        List<Document> filtered = stream.collect(Collectors.toList());
        long total = filtered.size();
        int from = page * size;
        int to = Math.min(from + size, filtered.size());
        List<Document> pageContent = (from >= filtered.size())
                ? Collections.emptyList()
                : filtered.subList(from, to);

        int totalPages = (int) Math.ceil((double) total / size);
        return new PagedResultDTO<>(
                pageContent,
                total,
                totalPages,
                page,
                size
        );
    }

}
