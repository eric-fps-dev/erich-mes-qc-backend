package com.fps.svmes.services;

import com.fps.svmes.dto.PagedResultDTO;
import com.fps.svmes.dto.dtos.reporting.QcRecordApprovalRequirementDTO;
import com.fps.svmes.dto.dtos.reporting.WidgetDataDTO;
import org.bson.Document;

import java.util.List;
import java.util.Map;

public interface ReportingService {
    List<WidgetDataDTO> extractWidgetData(String jsonInput);

    // Now receives jsonInput instead of a list of WidgetDataDTO
    List<WidgetDataDTO> extractWidgetDataWithCounts(Long formTemplateId, String startDateTime, String endDateTime);

    // Old fetchQcRecords method without pagination
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

    /**
    * Fetch drill-down records filtered by a specific field value.
    * Used for chart drill-down (pie chart slices, trend chart data points).
    *
    * @param formTemplateId the form template ID
    * @param fieldName the field name (widget name) to filter on
    * @param optionValue the option value to filter for (null to skip value filter)
    * @param startDateTime global start date time
    * @param endDateTime global end date time
    * @param bucketStart optional bucket start time (for trend drill-down)
    * @param bucketEnd optional bucket end time (for trend drill-down)
    * @param page page number (0-indexed)
    * @param size page size
    * @param sort sort field and direction (e.g., "created_at,desc")
    * @param search optional search keyword
    * @return paged result of filtered documents
    */
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

