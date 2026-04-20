package com.fps.svmes.models.sql.alert;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fps.svmes.models.sql.Common;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "qc_alert_record", schema = "quality_management")
@Data
@EqualsAndHashCode(callSuper = true)
public class AlertRecord extends Common {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @JsonProperty("id")
    private Long id;

    @Column(name = "alert_code")
    @JsonProperty("alert_code")
    private String alertCode;

    @Column(name = "alert_time")
    @JsonProperty("alert_time")
    private OffsetDateTime alertTime;

    @Column(name = "qc_form_template_id")
    @JsonProperty("qc_form_template_id")
    private Long qcFormTemplateId;

    @Column(name = "inspection_item_key")
    @JsonProperty("inspection_item_key")
    private String inspectionItemKey;

    @Column(name = "inspection_item_label")
    @JsonProperty("inspection_item_label")
    private String inspectionItemLabel;

    @Column(name = "inspection_value")
    @JsonProperty("inspection_value")
    private BigDecimal inspectionValue;

    @Column(name = "upper_control_limit")
    @JsonProperty("upper_control_limit")
    private BigDecimal upperControlLimit;

    @Column(name = "lower_control_limit")
    @JsonProperty("lower_control_limit")
    private BigDecimal lowerControlLimit;

    @Column(name = "rpn")
    @JsonProperty("rpn")
    private Integer rpn;

    @Column(name = "risk_level_id")
    @JsonProperty("risk_level_id")
    private Integer riskLevelId;

    @Column(name = "alert_status")
    @JsonProperty("alert_status")
    private Integer alertStatus;

    @OneToMany(mappedBy = "alert", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AlertProduct> alertProducts = new ArrayList<>();

    @OneToMany(mappedBy = "alert", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AlertBatch> alertBatches = new ArrayList<>();

    @OneToMany(mappedBy = "alert", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AlertInspector> alertInspectors = new ArrayList<>();

    @OneToMany(mappedBy = "alert", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AlertReviewer> alertReviewers = new ArrayList<>();

    @JsonProperty("alert_type")
    private String alertType; // "number" or "options"

    @JsonProperty("option_items")
    private List<String> optionItems;

    @JsonProperty("option_labels")
    private List<String> optionLabels;

    @JsonProperty("invalid_option_items")
    private List<String> invalidOptionItems;

    @JsonProperty("invalid_option_labels")
    private List<String> invalidOptionLabels;

    @JsonProperty("input_option_items")
    private List<String> inputOptionItems;

    @JsonProperty("input_option_items_labels")
    private List<String> inputOptionItemsLabels;

    @JsonProperty("submission_id")
    private String submissionId;

}
