package com.fps.svmes.models.sql.alert;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "qc_alert_batch", schema = "quality_management")
@Data
public class AlertBatch {

    @EmbeddedId
    private AlertBatchId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("alertId")
    private AlertRecord alert;

    @Column(name = "batch_id", insertable = false, updatable = false)
    private Long batchId;
}
