package com.fps.svmes.dto.dtos.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class UserRoleDTO {
    @JsonProperty("role")
    private RoleDTO role;

    @JsonProperty("is_primary")
    private Boolean isPrimary;
}

