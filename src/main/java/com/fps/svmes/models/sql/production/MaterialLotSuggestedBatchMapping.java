package com.fps.svmes.models.sql.production;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fps.svmes.models.sql.Common;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Loose mapping between an inventory-service material lot (identified by its
 * lot number string, no cross-service FK) and a local SuggestedBatch.
 */
@Entity
@Table(name = "material_lot_suggested_batch_mapping", schema = "quality_management")
@Data
@EqualsAndHashCode(callSuper = true)
public class MaterialLotSuggestedBatchMapping extends Common {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @JsonProperty("id")
    private Long id;

    // Not DB-unique: uniqueness is enforced by a partial index on status = 1 (see
    // docs/sql/material_qc_schema.sql) so a soft-deleted mapping doesn't block a new one.
    @Column(name = "material_lot_number", nullable = false)
    @JsonProperty("materialLotNumber")
    private String materialLotNumber;

    @Column(name = "qc_suggested_batch_id", nullable = false)
    @JsonProperty("suggestedBatchId")
    private Long suggestedBatchId;
}
