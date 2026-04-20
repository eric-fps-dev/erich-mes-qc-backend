package com.fps.svmes.models.nosql;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Map;

@Data
@Document(collection = "control_limit_setting")
public class ControlLimitSetting {

    @Id
    private String id;

    @Field("qc_form_template_id")
    private Long qcFormTemplateId;

    @Field("control_limits")
    private Map<String, ControlLimitEntry> controlLimits;

    @Data
    public static class ControlLimitEntry {

        @Field("upper_control_limit")
        private Double upperControlLimit;

        @Field("lower_control_limit")
        private Double lowerControlLimit;

        @Field("label")
        private String label;

        // 允许选项值（用于 checkbox、radio、select）
        @Field("valid_keys")
        private java.util.List<String> validKeys;

        // 所有可选项（用于 UI 渲染）
        @Field("optionItems")
        private java.util.List<OptionItem> optionItems;

        @Data
        public static class OptionItem {
            private String label;
            private String value;
        }
    }
}
