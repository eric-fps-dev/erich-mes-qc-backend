package com.fps.svmes.models.sql.production;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fps.svmes.models.sql.Common;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Links an inventory-service material (referenced only by id/name, no cross-service FK)
 * to the QC form template used to inspect it. One active link per (materialId, scope).
 * Field names/JSON shape intentionally mirror the existing frontend prototype's payload
 * ({@code materialId, materialName, defaultQcTemplateId, defaultQcTemplateName, active}).
 */
// "One active link per (materialId, scope)" is enforced by a partial index on status = 1
// (see docs/sql/material_qc_schema.sql), not a plain unique constraint, so a soft-deleted
// link doesn't block re-linking the same material/scope.
@Entity
@Table(name = "material_form_template_link", schema = "quality_management")
@Data
@EqualsAndHashCode(callSuper = true)
public class MaterialFormTemplateLink extends Common {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @JsonProperty("id")
    private Long id;

    /** "raw" | "finishedGoods" — matches the frontend's existing scope values. */
    @Column(name = "scope", nullable = false)
    @JsonProperty("scope")
    private String scope;

    @Column(name = "material_id", nullable = false)
    @JsonProperty("materialId")
    private Long materialId;

    @Column(name = "material_name")
    @JsonProperty("materialName")
    private String materialName;

    @Column(name = "qc_form_template_id", nullable = false)
    @JsonProperty("defaultQcTemplateId")
    private Long defaultQcTemplateId;

    @Column(name = "qc_form_template_name")
    @JsonProperty("defaultQcTemplateName")
    private String defaultQcTemplateName;

    @Column(name = "active")
    @JsonProperty("active")
    private Boolean active;
}
