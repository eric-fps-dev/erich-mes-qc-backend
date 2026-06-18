package com.fps.svmes.dto.requests;

import com.fps.shared.enums.TeamFormAssociationMode;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class TeamFormAssociationRequest {

    @NotNull
    private TeamFormAssociationMode associationMode;

    private List<String> nodeIds;
}
