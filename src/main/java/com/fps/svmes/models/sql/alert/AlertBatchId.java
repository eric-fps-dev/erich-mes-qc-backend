package com.fps.svmes.models.sql.alert;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertBatchId implements Serializable {

    @Column(name = "alert_id")
    private Long alertId;

    @Column(name = "batch_id")
    private Long batchId;
}
