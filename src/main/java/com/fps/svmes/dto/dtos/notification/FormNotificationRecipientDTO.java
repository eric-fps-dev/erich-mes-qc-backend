package com.fps.svmes.dto.dtos.notification;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class FormNotificationRecipientDTO {

    @JsonProperty("user_id")
    private Integer userId;

    @JsonProperty("user_email")
    private String userEmail;

    @JsonProperty("user_name")
    private String userName;
}
