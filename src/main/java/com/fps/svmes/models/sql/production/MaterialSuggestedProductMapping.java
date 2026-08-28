package com.fps.svmes.models.sql.production;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fps.svmes.models.sql.Common;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Loose mapping between an inventory-service material (warehouse_management.material,
 * referenced here only by its numeric id/name, no cross-service FK) and a local
 * SuggestedProduct.
 */
@Entity
@Table(name = "material_suggested_product_mapping", schema = "quality_management")
@Data
@EqualsAndHashCode(callSuper = true)
public class MaterialSuggestedProductMapping extends Common {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @JsonProperty("id")
    private Long id;

    // Not DB-unique: uniqueness is enforced by a partial index on status = 1 (see
    // docs/sql/material_qc_schema.sql) so a soft-deleted mapping doesn't block a new one.
    @Column(name = "material_id", nullable = false)
    @JsonProperty("materialId")
    private Long materialId;

    @Column(name = "material_name")
    @JsonProperty("materialName")
    private String materialName;

    @Column(name = "qc_suggested_product_id", nullable = false)
    @JsonProperty("suggestedProductId")
    private Long suggestedProductId;
}
