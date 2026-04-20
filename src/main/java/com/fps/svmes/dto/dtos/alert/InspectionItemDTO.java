package com.fps.svmes.dto.dtos.alert;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InspectionItemDTO {
    @JsonProperty("key")
    private String key;

    @JsonProperty("label")
    private String label;
}
