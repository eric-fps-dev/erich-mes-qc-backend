package com.fps.svmes.services.impl;

import com.fps.svmes.dto.dtos.spc.LimitDTO;
import com.fps.svmes.dto.dtos.spc.SPCDTO;
import com.fps.svmes.dto.dtos.spc.TimeSeriesDTO;
import com.fps.svmes.dto.requests.SPCRequest;
import com.fps.svmes.models.nosql.ControlLimitSetting;
import com.fps.svmes.repositories.mongoRepo.ControlLimitSettingRepository;
import com.fps.svmes.services.SPCService;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import org.apache.commons.math3.exception.NoDataException;
import org.apache.coyote.BadRequestException;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Time;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SPCServiceImpl implements SPCService {

    private final MongoClient mongoClient;

    private final ControlLimitSettingRepository controlLimitSettingRepository;

    @Value("${spring.data.mongodb.database}")
    private String mongoDatabaseName;

    @Autowired
    public SPCServiceImpl(MongoClient mongoClient, ControlLimitSettingRepository controlLimitSettingRepository) {
        this.mongoClient = mongoClient;
        this.controlLimitSettingRepository = controlLimitSettingRepository;
    }

    @Override
    @Transactional
    public List<SPCDTO> getSPCData(SPCRequest request) throws IllegalArgumentException {
        if (request.getStartDateTime().isAfter(request.getEndDateTime())) {
            throw new IllegalArgumentException("startDateTime: " + request.getStartDateTime() + " must not exceed endDateTime: " + request.getEndDateTime());
        }
        List<SPCDTO> spcList = new ArrayList<>();
        Timestamp start = Timestamp.from(request.getStartDateTime().toInstant());
        Timestamp end = Timestamp.from(request.getEndDateTime().toInstant());
        List<String> collectionNames = generateCollectionNames(
                request.getFormTemplateId(),
                start,
                end);
        MongoDatabase database = mongoClient.getDatabase(mongoDatabaseName);

        Optional<ControlLimitSetting> controlLimits = controlLimitSettingRepository.findByQcFormTemplateId(request.getFormTemplateId());
        List<String> wantedLimits = new ArrayList<>();

        // Query form_template_key_label_pairs for deleted fields
        Map<String, String> deletedKeyToLabel = new LinkedHashMap<>();
        {
            MongoCollection<Document> pairsCollection = database.getCollection("form_template_key_label_pairs");
            Document pairsDoc = pairsCollection.find(Filters.eq("qc_form_template_id", request.getFormTemplateId())).first();
            if (pairsDoc != null && pairsDoc.containsKey("fields")) {
                List<Document> fields = pairsDoc.getList("fields", Document.class);
                for (Document f : fields) {
                    if ("true".equals(f.getString("deleted"))) {
                        String key = f.getString("key");
                        String label = f.getString("label");
                        if (key != null && label != null) {
                            deletedKeyToLabel.put(key, label);
                        }
                    }
                }
            }
        }

        // if control limits exist, get only those with LOWER and UPPER limits and set the desired fields
        if (controlLimits.isPresent()) {
            // TODO: refactor once the limit structures are updated
            List<String> validLimits = controlLimits.get().getControlLimits().entrySet().stream()
                    .filter(e -> {
                        Double lower = e.getValue().getLowerControlLimit();
                        Double upper = e.getValue().getUpperControlLimit();

                        if (lower == null || upper == null) {
                            return false;
                        }

                        // Only exclude the untouched default pair (0, 99999).
                        // Allow valid one-sided business limits such as:
                        // - lower == 0 with a real upper limit
                        // - upper == 99999 with a real lower limit
                        return !(Double.compare(lower, 0.0) == 0 && Double.compare(upper, 99999.0) == 0);
                    })
                    .map(Map.Entry::getKey)
                    .toList();
            if (request.getFields() != null && !request.getFields().isEmpty()) {
                List<String> activeRequested = request.getFields().stream()
                        .filter(f -> !deletedKeyToLabel.containsKey(f))
                        .collect(Collectors.toList());
                List<String> deletedRequested = request.getFields().stream()
                        .filter(deletedKeyToLabel::containsKey)
                        .collect(Collectors.toList());
                if (!validLimits.containsAll(activeRequested)) {
                    throw new IllegalArgumentException("Invalid field(s) given. Accepted fields: " + validLimits);
                }
                wantedLimits = new ArrayList<>(activeRequested);
                wantedLimits.addAll(deletedRequested);
            } else {
                wantedLimits = new ArrayList<>(validLimits);
                wantedLimits.addAll(deletedKeyToLabel.keySet());
            }
        } else {
            if (request.getFields() != null && !request.getFields().isEmpty()) {
                wantedLimits = request.getFields().stream()
                        .filter(deletedKeyToLabel::containsKey)
                        .collect(Collectors.toList());
            } else {
                wantedLimits = new ArrayList<>(deletedKeyToLabel.keySet());
            }
        }

        // if no valid fields, return empty list
        if (wantedLimits.isEmpty()) {
            return spcList;
        }

        Map<String, List<TimeSeriesDTO>> timeSeriesMap = new HashMap<>();
        for (String fieldName : wantedLimits) {
            timeSeriesMap.put(fieldName, new ArrayList<>());
        }

        // Mongo filter
        Bson filter = Filters.and(
                Filters.gte("created_at", start),
                Filters.lte("created_at", end)
        );

        List<String> stateFilter = request.getSubmissionStates();
        boolean filterByState = stateFilter != null && !stateFilter.isEmpty();

        // Collect all matching documents across shards, deduplicate by version_group_id
        // keeping only the highest version per group (same logic as ReportingServiceImpl)
        Map<String, Document> latestVersionMap = new LinkedHashMap<>();
        for (String collectionName : collectionNames) {
            MongoCollection<Document> collection = database.getCollection(collectionName);
            for (Document doc : collection.find(filter)) {
                if (filterByState && !stateFilter.contains(doc.getString("state"))) continue;
                String groupId = doc.getString("version_group_id");
                if (groupId != null) {
                    int version = doc.getInteger("version", 0);
                    Document existing = latestVersionMap.get(groupId);
                    if (existing == null || version > existing.getInteger("version", 0)) {
                        latestVersionMap.put(groupId, doc);
                    }
                } else {
                    latestVersionMap.put(doc.getObjectId("_id").toString(), doc);
                }
            }
        }

        // build time series map using deduplicated documents
        for (Document doc : latestVersionMap.values()) {
            Date createdAtDate = doc.getDate("created_at");
            if (createdAtDate == null) continue;
            Timestamp createdAt = new Timestamp(createdAtDate.getTime());
            for (String wantedField : wantedLimits) {
                Object rawValue = doc.get(wantedField);
                if (rawValue instanceof Number) {
                    Double value = ((Number) rawValue).doubleValue();
                    TimeSeriesDTO timeSeriesDTO = new TimeSeriesDTO();
                    timeSeriesDTO.setTimestamp(createdAt);
                    timeSeriesDTO.setValue(value);
                    timeSeriesMap.get(wantedField).add(timeSeriesDTO);
                } else if (rawValue instanceof List<?> list && !list.isEmpty()) {
                    // If it's a list, check the first element
                    Object firstElement = list.get(0);
                    if (firstElement instanceof Number) {
                        Double value = ((Number) firstElement).doubleValue();
                        TimeSeriesDTO timeSeriesDTO = new TimeSeriesDTO();
                        timeSeriesDTO.setTimestamp(createdAt);
                        timeSeriesDTO.setValue(value);
                        timeSeriesMap.get(wantedField).add(timeSeriesDTO);
                    }
                }
            }
        }

        // build SPCDTO to append to return list
        Map<String, ControlLimitSetting.ControlLimitEntry> controlLimitMap = controlLimits.isPresent()
                ? controlLimits.get().getControlLimits()
                : Collections.emptyMap();
        for (String fieldName : wantedLimits) {
            SPCDTO spcdto = new SPCDTO();
            spcdto.setFieldId(fieldName);

            ControlLimitSetting.ControlLimitEntry entry = controlLimitMap.get(fieldName);
            if (entry != null) {
                spcdto.setFieldName(entry.getLabel());
                spcdto.setLimits(new LimitDTO(entry.getLowerControlLimit(), entry.getUpperControlLimit()));
            } else {
                // Deleted field without a control limit entry — use label from form_template_key_label_pairs
                spcdto.setFieldName(deletedKeyToLabel.getOrDefault(fieldName, fieldName));
                spcdto.setLimits(new LimitDTO(null, null));
            }

            List<TimeSeriesDTO> timeSeriesList = timeSeriesMap.get(fieldName);
            spcdto.setTimeSeries(timeSeriesList);
            spcdto.setTimeSeriesCount(timeSeriesList.size());

            spcList.add(spcdto);
        }
        return spcList;
    }

    private List<String> generateCollectionNames(Long formTemplateId, Timestamp startDateTime, Timestamp endDateTime) {
        List<String> collectionNames = new ArrayList<>();
        MongoDatabase database = mongoClient.getDatabase(mongoDatabaseName);

        // Convert timestamps to YYYYMM format
        SimpleDateFormat yearMonthFormat = new SimpleDateFormat("yyyyMM");
        int startYearMonth = Integer.parseInt(yearMonthFormat.format(startDateTime));
        int endYearMonth = Integer.parseInt(yearMonthFormat.format(endDateTime));

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
}
