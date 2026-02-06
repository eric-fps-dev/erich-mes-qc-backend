package com.fps.svmes.controllers;

import com.fps.svmes.dto.dtos.alert.AlertRecordDTO;
import com.fps.svmes.dto.dtos.alert.AlertSummaryDTO;
import com.fps.svmes.dto.dtos.alert.DetailedAlertRecordDTO;
import com.fps.svmes.dto.requests.alert.AlertRecordFilterRequest;
import com.fps.svmes.dto.requests.alert.UpdateAlertRecordRequest;
import com.fps.svmes.models.sql.alert.AlertRecord;
import com.fps.svmes.models.sql.alert.AlertStatus;
import com.fps.svmes.models.sql.alert.RiskLevel;
import com.fps.svmes.services.AlertRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.fps.svmes.repositories.jpaRepo.alert.RiskLevelRepository;
import com.fps.svmes.repositories.jpaRepo.alert.AlertStatusRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/alert-records")
@Slf4j
@RequiredArgsConstructor
public class AlertRecordController {

    private final AlertRecordService alertRecordService;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private RiskLevelRepository riskLevelRepository;

    @Autowired
    private AlertStatusRepository alertStatusRepository;


    /**
     * Updates the RPN value of an alert record (and automatically updates the status based on defined rules).
     * Also records a change log entry for modified fields (e.g., rpn, status).
     *
     * @param request UpdateAlertRecordRequest containing id, rpn, and updatedBy
     * @return the updated AlertRecordDTO
     */
    @PostMapping("/update-record")
    public ResponseEntity<?> updateAlertRecord(@RequestBody UpdateAlertRecordRequest request) {
        try {
            AlertRecordDTO updated = alertRecordService.updateRecord(request.getId(), request.getRpn(), Long.valueOf(request.getUpdatedBy()));
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("Update failed: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("update failed：" + e.getMessage());
        }
    }

    /**
     * Deletes an alert record.
     *
     * @param id      The ID of the alert record to delete
     * @param userId  The ID of the user performing the deletion
     * @return the deleted AlertRecordDTO
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAlertRecord(
            @PathVariable Long id,
            @RequestParam("userId") Long userId
    ) {
        try {
            AlertRecordDTO deleted = alertRecordService.deleteRecord(id, userId);
            return ResponseEntity.ok(deleted);
        } catch (Exception e) {
            log.error("Delete failed: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("delete failed: " + e.getMessage());
        }
    }

    /**
     * Retrieves a paginated list of detailed alert records.
     *
     * @param page The page number (zero-based)
     * @param size The page size
     * @return A Page of DetailedAlertRecordDTO
     */
    @GetMapping
    public ResponseEntity<?> getAllAlertRecords(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<DetailedAlertRecordDTO> result = alertRecordService.getDetailedList(page, size);
        return ResponseEntity.ok(result);
    }

    /**
     * Retrieves a summary of alert records.
     *
     * @return An AlertSummaryDTO containing summary information
     */
    @GetMapping("/summary")
    public ResponseEntity<?> getAlertSummary() {
        try {
            AlertSummaryDTO summary = alertRecordService.getAlertSummary();
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            log.error("Failed to get alert summary", e);
            return ResponseEntity.status(500).body("Error fetching summary: " + e.getMessage());
        }
    }

    /**
     * Filters alert records based on various criteria.
     *
     * @param request AlertRecordFilterRequest containing filter criteria
     * @return A Map containing the filtered alert records and total count
     */
    @PostMapping("/filter")
    public ResponseEntity<Map<String, Object>> filterAlertRecords(@RequestBody AlertRecordFilterRequest request) {
        Page<DetailedAlertRecordDTO> page = alertRecordService.filterAlertRecords(request);
        List<DetailedAlertRecordDTO> dtos = page.getContent().stream()
                .map(alert -> modelMapper.map(alert, DetailedAlertRecordDTO.class))
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("content", dtos);
        response.put("totalElements", page.getTotalElements());
        return ResponseEntity.ok(response);
    }

    /**
     * Gets all risk levels (for frontend dropdown).
     */
    @GetMapping("/risk-levels")
    public ResponseEntity<List<RiskLevel>> getRiskLevels() {
        List<RiskLevel> levels = riskLevelRepository.findAll();
        return ResponseEntity.ok(levels);
    }

    /**
     * Gets all alert statuses (for frontend dropdown).
     */
    @GetMapping("/alert-statuses")
    public ResponseEntity<List<AlertStatus>> getAlertStatuses() {
        List<AlertStatus> statuses = alertStatusRepository.findAll();
        return ResponseEntity.ok(statuses);
    }


}
