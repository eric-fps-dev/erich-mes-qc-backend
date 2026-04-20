package com.fps.svmes.dto.dtos.recipe;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ControlLimitSettingDTO {

    @JsonProperty("qc_form_template_id")
    private Long qcFormTemplateId;

    @JsonProperty("control_limits")
    private LinkedHashMap<String, ControlLimitEntry> controlLimits;

    @Data
    public static class ControlLimitEntry {
        @JsonProperty("upper_control_limit")
        private Double upperControlLimit;

        @JsonProperty("lower_control_limit")
        private Double lowerControlLimit;

        @JsonProperty("label")
        private String label;

        @JsonProperty("valid_keys")
        private java.util.List<String> validKeys;

        @JsonProperty("optionItems")
        private java.util.List<OptionItem> optionItems;

        @Data
        public static class OptionItem {
            private String label;
            private String value;
        }
    }

}