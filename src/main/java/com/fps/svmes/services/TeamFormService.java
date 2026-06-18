package com.fps.svmes.services;

import com.fps.svmes.dto.requests.TeamFormAssociationRequest;
import com.fps.svmes.dto.responses.TeamFormAssociationResponse;
import com.fps.svmes.models.nosql.FormNode;
import java.util.List;

public interface TeamFormService {

    void assignFormsToTeam(Integer teamId, List<String> formIds);

    void removeFormsFromTeam(Integer teamId, List<String> formIds);

    List<String> getFormIdsByTeam(Integer teamId);

    void removeAllFormsFromTeam(Integer teamId);

    TeamFormAssociationResponse getTeamFormAssociation(Integer teamId);

    void replaceTeamFormAssociation(Integer teamId, TeamFormAssociationRequest request);

    void clearTeamFormAssociation(Integer teamId);

    List<FormNode> getFormTreeByTeamId(Integer teamId);
}
