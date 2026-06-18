package com.fps.svmes.controllers;

import com.fps.svmes.dto.PagedResultDTO;
import com.fps.svmes.dto.dtos.reporting.QcRecordApprovalRequirementDTO;
import com.fps.svmes.dto.requests.QcRecordApprovalRequirementsRequest;
import com.fps.svmes.dto.dtos.reporting.WidgetDataDTO;
import com.fps.svmes.services.ReportingService;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/reporting")
public class ReportingController {

    private final ReportingService reportingService;

    @Autowired
    public ReportingController(ReportingService reportingService) {
        this.reportingService = reportingService;
    }

    @PostMapping("/extract")
    public List<WidgetDataDTO> extractWidgetData(@RequestBody String jsonInput) {
        return reportingService.extractWidgetData(jsonInput);
    }

    @PostMapping("/extract-with-counts")
    public List<WidgetDataDTO> extractWidgetDataWithCounts(
            @RequestParam Long formTemplateId,
            @RequestParam(required = false) String startDateTime,
            @RequestParam(required = false) String endDateTime
    ) {
        return reportingService.extractWidgetDataWithCounts(formTemplateId, startDateTime, endDateTime);
    }

    /**
     * Fetch QC records within a given date range, with pagination.
     */
    @GetMapping("/qc-records")
    public PagedResultDTO<Document> getQcRecords(
            @RequestParam Long formTemplateId,
            @RequestParam String startDateTime,
            @RequestParam String endDateTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String search
    ) {
        return reportingService.fetchQcRecordsPaged(
                formTemplateId,
                startDateTime,
                endDateTime,
                page,
                size,
                sort,
                search
        );
    }

    @PostMapping("/qc-records/approval-requirements")
    public Map<String, QcRecordApprovalRequirementDTO> getQcRecordApprovalRequirements(
            @RequestBody QcRecordApprovalRequirementsRequest request
    ) {
        return reportingService.fetchQcRecordApprovalRequirements(request == null ? null : request.getRecordIds());
    }

    @GetMapping("/qc-records/export")
    public ResponseEntity<List<Document>> exportQcRecords(
            @RequestParam Long   formTemplateId,
            @RequestParam String startDateTime,
            @RequestParam String endDateTime,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sort) {

        List<Document> allRecords = reportingService.fetchAllRecordsWithoutPagination(
                formTemplateId, startDateTime, endDateTime, search, sort);

        return ResponseEntity.ok(allRecords);
    }


    /**
     * Fetch QC records filtered by created_by ID, with pagination.
     */
    @GetMapping("/qc-records/by-user")
    public List<Document> getQcRecordsByUser(
            @RequestParam Long formTemplateId,
            @RequestParam String startDateTime,
            @RequestParam String endDateTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam Integer createdBy) {

        return reportingService.fetchQcRecordsFilteredByCreator(formTemplateId, startDateTime, endDateTime, page, size, createdBy);
    }

    @GetMapping("/qc-records/versions")
    public List<Document> getAllVersionsByGroupId(
            @RequestParam Long formTemplateId,
            @RequestParam String versionGroupId
    ) {
        return reportingService.fetchAllVersionsByGroupId(formTemplateId, versionGroupId);
    }

    /**
     * Fetch drill-down records filtered by a specific field value.
     * Used for chart drill-down (pie chart slices, trend chart data points).
     *
     * @param formTemplateId the form template ID
     * @param fieldName the field name (widget name) to filter on
     * @param optionValue the option value to filter for (optional - null skips value filter)
     * @param startDateTime global start date time (yyyy-MM-dd HH:mm:ss)
     * @param endDateTime global end date time (yyyy-MM-dd HH:mm:ss)
     * @param bucketStart optional bucket start time for trend drill-down (yyyy-MM-dd HH:mm:ss)
     * @param bucketEnd optional bucket end time for trend drill-down (yyyy-MM-dd HH:mm:ss)
     * @param page page number (0-indexed)
     * @param size page size
     * @param sort sort field and direction (e.g., "created_at,desc")
     * @param search optional search keyword
     * @return paged result of filtered documents
     */
    /**
     * Debug endpoint: returns keyValueMap and raw document keys to diagnose deleted field data issues.
     */
    @GetMapping("/debug/template-data")
    public ResponseEntity<Map<String, Object>> debugTemplateData(
            @RequestParam Long formTemplateId,
            @RequestParam String startDateTime,
            @RequestParam String endDateTime) {
        return ResponseEntity.ok(reportingService.debugTemplateData(formTemplateId, startDateTime, endDateTime));
    }

    @GetMapping("/qc-records/drilldown")
    public PagedResultDTO<Document> getDrilldownRecords(
            @RequestParam Long formTemplateId,
            @RequestParam String fieldName,
            @RequestParam(required = false) Integer optionValue,
            @RequestParam String startDateTime,
            @RequestParam String endDateTime,
            @RequestParam(required = false) String bucketStart,
            @RequestParam(required = false) String bucketEnd,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String search
    ) {
        return reportingService.fetchDrilldownRecords(
                formTemplateId,
                fieldName,
                optionValue,
                startDateTime,
                endDateTime,
                bucketStart,
                bucketEnd,
                page,
                size,
                sort,
                search
        );
    }

}
