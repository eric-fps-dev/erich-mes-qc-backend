package com.fps.svmes.models.sql.alert;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "qc_alert_record_log", schema = "quality_management")
@Data
public class AlertRecordLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long alertRecordId;

    private String operation; // "update", "auto-status", etc.

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, List<String>> diff;

    private String remarks;

    private Integer status = 1;

    private Integer createdBy;
    private OffsetDateTime createdAt = OffsetDateTime.now();

    private Integer updatedBy;
    private OffsetDateTime updatedAt = OffsetDateTime.now();
}

