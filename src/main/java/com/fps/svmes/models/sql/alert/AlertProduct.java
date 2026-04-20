package com.fps.svmes.models.sql.alert;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "qc_alert_product", schema = "quality_management")
@Data
public class AlertProduct {

    @EmbeddedId
    private AlertProductId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("alertId")
    private AlertRecord alert;

    @Column(name = "product_id", insertable = false, updatable = false)
    private Long productId;
}


