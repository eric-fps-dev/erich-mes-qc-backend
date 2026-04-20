package com.fps.svmes.dto.dtos.alert;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fps.svmes.dto.dtos.CommonDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@EqualsAndHashCode(callSuper = true)
public class AlertRecordDTO extends CommonDTO {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("alert_code")
    private String alertCode;

    @JsonProperty("alert_time")
    private OffsetDateTime alertTime;

    @JsonProperty("qc_form_template_id")
    private Long qcFormTemplateId;

    @JsonProperty("inspection_item_key")
    private String inspectionItemKey;

    @JsonProperty("inspection_item_label")
    private String inspectionItemLabel;

    @JsonProperty("inspection_value")
    private BigDecimal inspectionValue;

    @JsonProperty("upper_control_limit")
    private BigDecimal upperControlLimit;

    @JsonProperty("lower_control_limit")
    private BigDecimal lowerControlLimit;

    @JsonProperty("rpn")
    private Integer rpn;

    @JsonProperty("risk_level_id")
    private Integer riskLevelId;

    @JsonProperty("product_ids")
    private List<Long> productIds;

    @JsonProperty("batch_ids")
    private List<Long> batchIds;

    @JsonProperty("inspector_ids")
    private List<Long> inspectorIds;

    @JsonProperty("reviewer_ids")
    private List<Long> reviewerIds;

    @JsonProperty("alert_status")
    private Integer alertStatus;

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
