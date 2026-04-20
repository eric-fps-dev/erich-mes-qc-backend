package com.fps.svmes.dto.dtos.alert;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExceededFieldInfoDTO {

    @JsonProperty("type")
    private String type; // "number" or "options"

    @JsonProperty("value")
    private Object value; // For both number and options

    @JsonProperty("lower_limit")
    private BigDecimal lowerLimit; // Only for type = "number"

    @JsonProperty("upper_limit")
    private BigDecimal upperLimit; // Only for type = "number"

    @JsonProperty("valid_options")
    private List<String> validOptions; // Only for type = "options"

    @JsonProperty("invalid_options")
    private List<String> invalidOptions; // Only for type = "options"

    @JsonProperty("valid_option_labels")
    private List<String> validOptionLabels;

    @JsonProperty("invalid_option_labels")
    private List<String> invalidOptionLabels;

    @JsonProperty("result")
    private String result; // "high", "low", "invalid", etc.
}
