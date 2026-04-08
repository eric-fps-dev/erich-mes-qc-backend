package com.fps.svmes.services;

import com.fps.svmes.dto.dtos.alert.AlertRecordDTO;
import com.fps.svmes.dto.dtos.alert.AlertSummaryDTO;
import com.fps.svmes.dto.dtos.alert.DetailedAlertRecordDTO;
import com.fps.svmes.dto.requests.alert.AlertRecordFilterRequest;
import com.fps.svmes.models.sql.alert.AlertRecord;
import org.springframework.data.domain.Page;

import java.util.List;

public interface AlertRecordService {
    AlertRecordDTO create(AlertRecordDTO dto);
    Page<DetailedAlertRecordDTO> getDetailedList(int page, int size);
    AlertRecordDTO updateRecord(Long alertId, Integer newRpn, Long userId);
    AlertRecordDTO deleteRecord(Long alertId, Long userId);
    AlertSummaryDTO getAlertSummary();
    AlertSummaryDTO getAlertSummary(AlertRecordFilterRequest request);
    Page<DetailedAlertRecordDTO> filterAlertRecords(AlertRecordFilterRequest request);
    void deleteBySubmissionIds(List<String> submissionIds);

}
