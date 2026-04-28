package com.fps.svmes.dto.dtos.notification;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class FormNotificationConfigDTO {

    private Long id;

    @JsonProperty("form_template_id")
    private Long formTemplateId;

    @JsonProperty("trigger_type")
    private String triggerType = "always";

    @JsonProperty("delivery_method")
    private String deliveryMethod = "email";

    private List<FormNotificationRecipientDTO> recipients;
}
