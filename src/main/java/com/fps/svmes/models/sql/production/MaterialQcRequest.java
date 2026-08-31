package com.fps.svmes.models.sql.production;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fps.svmes.models.sql.Common;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * A request to QC-check a material lot against a form template. Lifecycle:
 * draft -&gt; pending (on confirm, resolving suggested product/batch) -&gt; passed | failed.
 */
@Entity
@Table(name = "material_qc_request", schema = "quality_management")
@Data
@EqualsAndHashCode(callSuper = true)
public class MaterialQcRequest extends Common {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @JsonProperty("id")
    private Long id;

    @Column(name = "material_id", nullable = false)
    @JsonProperty("materialId")
    private Long materialId;

    @Column(name = "material_name")
    @JsonProperty("materialName")
    private String materialName;

    @Column(name = "material_lot_number", nullable = false)
    @JsonProperty("materialLotNumber")
    private String materialLotNumber;

    // Optional at creation/edit — a request can be drafted before a template is picked.
    // Required only to confirm (see MaterialQcRequestServiceImpl.confirm).
    @Column(name = "qc_form_template_id")
    @JsonProperty("formTemplateId")
    private Long formTemplateId;

    @Column(name = "qc_form_template_name")
    @JsonProperty("formTemplateName")
    private String formTemplateName;

    /** MaterialQcRequestState.dbValue() */
    @Column(name = "state", nullable = false)
    @JsonProperty("state")
    private String state;

    @Column(name = "resolved_suggested_product_id")
    @JsonProperty("resolvedSuggestedProductId")
    private Long resolvedSuggestedProductId;

    @Column(name = "resolved_suggested_batch_id")
    @JsonProperty("resolvedSuggestedBatchId")
    private Long resolvedSuggestedBatchId;

    // Guards against lost updates when two requests race a state transition
    // (e.g. confirm + setResult, or two concurrent setResult calls) on the same row.
    @Version
    @Column(name = "version", nullable = false)
    @JsonProperty("version")
    private Long version;
}
