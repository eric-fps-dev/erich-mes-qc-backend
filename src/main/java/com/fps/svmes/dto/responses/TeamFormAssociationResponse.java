package com.fps.svmes.dto.responses;

import com.fps.shared.enums.TeamFormAssociationMode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamFormAssociationResponse {

    private Integer teamId;
    private TeamFormAssociationMode associationMode;
    private List<String> nodeIds;
}
