package com.fps.svmes.dto.dtos.subscription;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fps.svmes.dto.dtos.CommonDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@EqualsAndHashCode(callSuper = true)
public class WeeklyReportSubscriptionDTO extends CommonDTO {

    @JsonProperty("id")
    private Integer id;

    @JsonProperty("user_id")
    private Integer userId;

    @JsonProperty("user_name")
    private String userName;

    @JsonProperty("email")
    private String email;

    @JsonProperty("is_active")
    private Boolean isActive;

    @JsonProperty("status")
    private Integer status;

    @JsonProperty("language")
    private String language;
}
