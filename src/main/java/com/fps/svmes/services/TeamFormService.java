package com.fps.svmes.services;

import com.fps.svmes.models.nosql.FormNode;
import java.util.List;

public interface TeamFormService {

    void assignFormsToTeam(Integer teamId, List<String> formIds);

    void removeFormsFromTeam(Integer teamId, List<String> formIds);

    List<String> getFormIdsByTeam(Integer teamId);

    void removeAllFormsFromTeam(Integer teamId);

    List<FormNode> getFormTreeByTeamId(Integer teamId);
}
