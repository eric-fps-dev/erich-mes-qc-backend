package com.fps.svmes.dto.dtos.alert;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fps.svmes.dto.dtos.CommonDTO;
import com.fps.svmes.dto.dtos.production.SuggestedBatchDTO;
import com.fps.svmes.dto.dtos.production.SuggestedProductDTO;
import com.fps.svmes.dto.dtos.qcForm.QcFormTemplateDTO;
import com.fps.svmes.dto.dtos.user.UserDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DetailedAlertRecordDTO extends CommonDTO {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("alert_code")
    private String alertCode;

    @JsonProperty("alert_time")
    private OffsetDateTime alertTime;

    @JsonProperty("products")
    private List<SuggestedProductDTO> products;

    @JsonProperty("batches")
    private List<SuggestedBatchDTO> batches;

    @JsonProperty("qc_form_template")
    private QcFormTemplateDTO qcFormTemplate;

    @JsonProperty("inspection_item")
    private InspectionItemDTO inspectionItem;

    @JsonProperty("inspection_value")
    private BigDecimal inspectionValue;

    @JsonProperty("control_range")
    private String controlRange;

    @JsonProperty("upper_control_limit")
    private BigDecimal upperControlLimit;

    @JsonProperty("lower_control_limit")
    private BigDecimal lowerControlLimit;

    @JsonProperty("alert_status")
    private AlertStatusDTO alertStatus;

    @JsonProperty("risk_level")
    private RiskLevelDTO riskLevel;

    @JsonProperty("rpn")
    private Integer rpn;

    @JsonProperty("inspectors")
    private List<UserDTO> inspectors;

    @JsonProperty("reviewers")
    private List<UserDTO> reviewers;

    @JsonProperty("alert_type")
    private String alertType;

    @JsonProperty("option_items")
    private List<String> optionItems;

    @JsonProperty("option_labels")
    private List<String> optionLabels;

    @JsonProperty("invalid_option_items")
    private List<String> invalidOptionItems;

    @JsonProperty("invalid_option_items_labels")
    private List<String> invalidOptionItemsLabels;

    @JsonProperty("input_option_items")
    private List<String> inputOptionItems;

    @JsonProperty("input_option_items_labels")
    private List<String> inputOptionItemsLabels;

    @JsonProperty("submission_id")
    private String submissionId;

}
