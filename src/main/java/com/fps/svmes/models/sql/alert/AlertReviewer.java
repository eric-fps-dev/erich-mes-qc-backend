package com.fps.svmes.models.sql.alert;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "qc_alert_reviewer", schema = "quality_management")
@Data
public class AlertReviewer {

    @EmbeddedId
    private AlertReviewerId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("alertId")
    private AlertRecord alert;

    @Column(name = "reviewer_id", insertable = false, updatable = false)
    private Long reviewerId;
}
