package com.fps.svmes.services;

import com.fps.svmes.dto.PagedResultDTO;
import com.fps.svmes.dto.dtos.reporting.QcRecordApprovalRequirementDTO;
import com.fps.svmes.dto.dtos.reporting.WidgetDataDTO;
import org.bson.Document;

import java.security.Timestamp;
import java.util.List;
import java.util.Map;

public interface ReportingService {
    List<WidgetDataDTO> extractWidgetData(String jsonInput);

    List<WidgetDataDTO> extractWidgetDataWithCounts(Long formTemplateId, String startDateTime, String endDateTime);

    List<Document> fetchQcRecords(Long formTemplateId, String startDateTime, String endDateTime, Integer page, Integer size);

    List<Document> fetchQcRecordsFilteredByCreator(Long formTemplateId, String startDateTime, String endDateTime, Integer page, Integer size, Integer createdBy);

    List<Document> fetchAllVersionsByGroupId(Long formTemplateId, String versionGroupId);

    PagedResultDTO<Document> fetchQcRecordsPaged(
            Long formTemplateId,
            String startDateTime,
            String endDateTime,
            Integer page,
            Integer size,
            String sort,
            String search
    );

    List<Document> fetchAllRecordsWithoutPagination(Long formTemplateId,
                                                           String startDateTime,
                                                           String endDateTime,
                                                           String search,
                                                           String sort);

    Map<String, Object> debugTemplateData(Long formTemplateId, String startDateTime, String endDateTime);

    PagedResultDTO<Document> fetchDrilldownRecords(
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
    );

    Map<String, QcRecordApprovalRequirementDTO> fetchQcRecordApprovalRequirements(List<String> recordIds);
}

