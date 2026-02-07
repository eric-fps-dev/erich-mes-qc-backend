package com.fps.svmes.models.sql.user;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class TeamFormId implements Serializable {

    private Integer teamId;
    private String formId;
}
