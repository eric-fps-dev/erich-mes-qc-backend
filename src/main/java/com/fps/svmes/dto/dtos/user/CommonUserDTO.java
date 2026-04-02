package com.fps.svmes.dto.dtos.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CommonUserDTO {
    @JsonProperty("id")
    private Long id;

    @JsonProperty("first_name")
    private String firstName;

    @JsonProperty("last_name")
    private String lastName;

    @JsonProperty("full_name")
    private String fullName;

    @JsonProperty("username")
    private String username;

    @JsonProperty("employee_number")
    private String employeeNumber;

    @JsonProperty("image")
    private String image;
}

