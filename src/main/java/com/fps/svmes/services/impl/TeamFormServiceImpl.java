package com.fps.svmes.services.impl;

import com.fps.shared.entity.primary.team.Team;
import com.fps.shared.entity.primary.team.TeamQCForm;
import com.fps.svmes.models.nosql.FormNode;
import com.fps.svmes.repositories.jpaRepo.user.TeamFormRepository;
import com.fps.svmes.repositories.jpaRepo.user.TeamRepository;
import com.fps.svmes.repositories.mongoRepo.FormNodeRepository;
import com.fps.svmes.services.TeamFormService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TeamFormServiceImpl implements TeamFormService {

    private final TeamFormRepository teamFormRepository;
    private final TeamRepository teamRepository;
    private final FormNodeRepository formNodeRepository;

    @Transactional
    @Override
    public void assignFormsToTeam(Integer teamId, List<String> formIds) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new EntityNotFoundException("Team not found"));

        Set<String> existingFormIds = teamFormRepository.findFormIdsByTeamId(teamId);

        List<TeamQCForm> newAssignments = formIds.stream()
                .filter(formId -> !existingFormIds.contains(formId))
                .map(formId -> new TeamQCForm(new TeamQCForm.TeamFormId(teamId, formId), team))
                .toList();

        if (!newAssignments.isEmpty()) {
            teamFormRepository.saveAll(newAssignments);
        }
    }

    @Transactional
    @Override
    public void removeFormsFromTeam(Integer teamId, List<String> formIds) {
        if (formIds == null || formIds.isEmpty()) {
            return;
        }

        teamFormRepository.deleteByIdTeamIdAndIdFormIdIn(teamId, formIds);
    }

    @Override
    public List<String> getFormIdsByTeam(Integer teamId) {
        return teamFormRepository.findByTeamId(teamId)
                .stream()
                .map(tf -> tf.getId().getFormId())
                .toList();
    }

    @Transactional
    @Override
    public void removeAllFormsFromTeam(Integer teamId) {
        teamFormRepository.deleteByTeamId(teamId);
    }

    @Override
    public List<FormNode> getFormTreeByTeamId(Integer teamId) {
        List<String> formIds = getFormIdsByTeam(teamId);
        List<FormNode> fullTree = formNodeRepository.findAll();

        List<FormNode> filteredTree = new ArrayList<>();
        for (FormNode root : fullTree) {
            FormNode filtered = filterTreeByFormIds(root, formIds);
            if (filtered != null) {
                filteredTree.add(filtered);
            }
        }

        return filteredTree;
    }

    private FormNode filterTreeByFormIds(FormNode node, List<String> allowedIds) {
        if ("document".equalsIgnoreCase(node.getNodeType())) {
            return allowedIds.contains(node.getId()) ? node : null;
        }

        List<FormNode> filteredChildren = new ArrayList<>();
        for (FormNode childNode : node.getChildren()) {
            FormNode filteredChild = filterTreeByFormIds(childNode, allowedIds);
            if (filteredChild != null) {
                filteredChildren.add(filteredChild);
            }
        }

        if (!filteredChildren.isEmpty()) {
            FormNode newNode = new FormNode();
            newNode.setId(node.getId());
            newNode.setLabel(node.getLabel());
            newNode.setNodeType(node.getNodeType());
            newNode.setQcFormTemplateId(node.getQcFormTemplateId());
            newNode.setChildren(filteredChildren);
            return newNode;
        }

        return null;
    }
}
