package com.fps.svmes.dto.dtos.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class RoleDTO {

    @JsonProperty("id")
    private Short id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("code")
    private String code;

//    @JsonProperty("description")
//    private String description;
//
//    @JsonProperty("el_tag_type")
//    private String elTagType;
}
