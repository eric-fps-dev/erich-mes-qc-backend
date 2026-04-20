package com.fps.svmes.repositories.jpaRepo.alert;

import com.fps.svmes.models.sql.alert.AlertRecordLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertRecordLogRepository extends JpaRepository<AlertRecordLog, Long> {

    // Optional: Find logs by alert record ID (e.g., for frontend display)
    List<AlertRecordLog> findByAlertRecordIdOrderByCreatedAtDesc(Long alertRecordId);
}
