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

        // if control limits exist, get only those with LOWER and UPPER limits and set the desired fields
        if (controlLimits.isPresent()) {
            // TODO: refactor once the limit structures are updated
            List<String> validLimits = controlLimits.get().getControlLimits().entrySet().stream()
                    .filter(e ->
                            e.getValue().getLowerControlLimit() != null
                                    && e.getValue().getUpperControlLimit() != null
                                    && (e.getValue().getUpperControlLimit() != 99999 && e.getValue().getLowerControlLimit() != 0)
                    )
                    .map(Map.Entry::getKey)
                    .toList();
            if (request.getFields() != null && !request.getFields().isEmpty()) {
                if (validLimits.containsAll(request.getFields())) {
                    wantedLimits = request.getFields();
                } else {
                    throw new IllegalArgumentException("Invalid field(s) given. Accepted fields: " + validLimits);
                }
            } else {
                wantedLimits = validLimits;
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

        // build time series map using field as keys
        for (String collectionName : collectionNames) {
            MongoCollection<Document> collection = database.getCollection(collectionName);
            for (Document doc : collection.find(filter)) {
                Timestamp createdAt = Timestamp.from(doc.getDate("created_at").toInstant());
                for (String wantedField : wantedLimits) {
                    if (doc.get(wantedField) != null) {
                        Object raw = doc.get(wantedField);
                        Double value;
                        if (raw instanceof Number) {
                            value = ((Number) raw).doubleValue();
                        } else {
                            try { value = Double.parseDouble(raw.toString()); }
                            catch (NumberFormatException e) { continue; }
                        }
                        TimeSeriesDTO timeSeriesDTO = new TimeSeriesDTO();
                        timeSeriesDTO.setTimestamp(createdAt);
                        timeSeriesDTO.setValue(value);

                        timeSeriesMap.get(wantedField).add(timeSeriesDTO);
                    }
                }
            }
        }

        // build SPCDTO to append to return list
        for (String fieldName : wantedLimits) {
            SPCDTO spcdto = new SPCDTO();
            spcdto.setFieldName(controlLimits.get().getControlLimits().get(fieldName).getLabel());
            spcdto.setFieldId(fieldName);

            LimitDTO limits = new LimitDTO(
                    controlLimits.get().getControlLimits().get(fieldName).getLowerControlLimit(),
                    controlLimits.get().getControlLimits().get(fieldName).getUpperControlLimit()
            );
            spcdto.setLimits(limits);

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
