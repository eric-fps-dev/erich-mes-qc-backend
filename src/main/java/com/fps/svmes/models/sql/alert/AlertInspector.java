package com.fps.svmes.models.sql.alert;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "qc_alert_inspector", schema = "quality_management")
@Data
public class AlertInspector {

    @EmbeddedId
    private AlertInspectorId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("alertId")
    private AlertRecord alert;

    @Column(name = "inspector_id", insertable = false, updatable = false)
    private Long inspectorId;
}
