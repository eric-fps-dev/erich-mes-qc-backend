package com.fps.svmes.services.impl;

import com.fps.shared.entity.primary.team.Team;
import com.fps.shared.entity.primary.team.TeamFormAccessConfig;
import com.fps.shared.entity.primary.team.TeamQCForm;
import com.fps.svmes.dto.requests.TeamFormAssociationRequest;
import com.fps.svmes.dto.responses.TeamFormAssociationResponse;
import com.fps.svmes.models.nosql.FormNode;
import com.fps.shared.enums.TeamFormAssociationMode;
import com.fps.svmes.repositories.jpaRepo.user.TeamFormAccessConfigRepository;
import com.fps.svmes.repositories.jpaRepo.user.TeamFormRepository;
import com.fps.svmes.repositories.jpaRepo.user.TeamRepository;
import com.fps.svmes.repositories.mongoRepo.FormNodeRepository;
import com.fps.svmes.services.TeamFormService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TeamFormServiceImpl implements TeamFormService {

    private final TeamFormRepository teamFormRepository;
    private final TeamFormAccessConfigRepository teamFormAccessConfigRepository;
    private final TeamRepository teamRepository;
    private final FormNodeRepository formNodeRepository;

    @Transactional
    @Override
    public void assignFormsToTeam(Integer teamId, List<String> formIds) {
        if (formIds == null || formIds.isEmpty()) {
            return;
        }

        TeamFormAssociationResponse currentAssociation = getTeamFormAssociation(teamId);
        LinkedHashSet<String> mergedNodeIds = new LinkedHashSet<>();
        if (currentAssociation.getAssociationMode() == TeamFormAssociationMode.FORM_NODE) {
            mergedNodeIds.addAll(currentAssociation.getNodeIds());
        }
        mergedNodeIds.addAll(normalizeNodeIds(formIds));

        TeamFormAssociationRequest request = new TeamFormAssociationRequest();
        request.setAssociationMode(TeamFormAssociationMode.FORM_NODE);
        request.setNodeIds(new ArrayList<>(mergedNodeIds));
        replaceTeamFormAssociation(teamId, request);
    }

    @Transactional
    @Override
    public void removeFormsFromTeam(Integer teamId, List<String> formIds) {
        if (formIds == null || formIds.isEmpty()) {
            return;
        }

        TeamFormAssociationResponse currentAssociation = getTeamFormAssociation(teamId);
        if (currentAssociation.getAssociationMode() != TeamFormAssociationMode.FORM_NODE) {
            return;
        }

        Set<String> formIdsToRemove = new LinkedHashSet<>(formIds);
        List<String> remainingNodeIds = currentAssociation.getNodeIds().stream()
                .filter(nodeId -> !formIdsToRemove.contains(nodeId))
                .toList();

        if (remainingNodeIds.isEmpty()) {
            clearTeamFormAssociation(teamId);
            return;
        }

        TeamFormAssociationRequest request = new TeamFormAssociationRequest();
        request.setAssociationMode(TeamFormAssociationMode.FORM_NODE);
        request.setNodeIds(remainingNodeIds);
        replaceTeamFormAssociation(teamId, request);
    }

    @Override
    public List<String> getFormIdsByTeam(Integer teamId) {
        ensureTeamExists(teamId);
        return new ArrayList<>(teamFormRepository.findFormIdsByTeamId(teamId));
    }

    @Transactional
    @Override
    public void removeAllFormsFromTeam(Integer teamId) {
        clearTeamFormAssociation(teamId);
    }

    @Override
    public TeamFormAssociationResponse getTeamFormAssociation(Integer teamId) {
        ensureTeamExists(teamId);
        TeamFormAssociationMode mode = getAssociationModeOrLegacy(teamId);
        List<String> nodeIds = mode == TeamFormAssociationMode.FORM_NODE || mode == TeamFormAssociationMode.FOLDER_NODE
                ? new ArrayList<>(teamFormRepository.findFormIdsByTeamId(teamId))
                : List.of();
        return new TeamFormAssociationResponse(teamId, mode, nodeIds);
    }

    @Transactional
    @Override
    public void replaceTeamFormAssociation(Integer teamId, TeamFormAssociationRequest request) {
        if (request == null || request.getAssociationMode() == null) {
            throw new IllegalArgumentException("associationMode is required");
        }

        Team team = ensureTeamExists(teamId);
        TeamFormAssociationMode mode = request.getAssociationMode();
        List<String> nodeIds = normalizeNodeIds(request.getNodeIds());

        validateAssociationRequest(mode, nodeIds);
        teamFormRepository.deleteByTeamId(teamId);

        saveAssociationMode(teamId, team, mode);

        if (mode == TeamFormAssociationMode.ALL_FORMS) {
            return;
        }

        List<TeamQCForm> assignments = nodeIds.stream()
                .map(nodeId -> new TeamQCForm(new TeamQCForm.TeamFormId(teamId, nodeId), team))
                .toList();

        teamFormRepository.saveAll(assignments);
    }

    @Transactional
    @Override
    public void clearTeamFormAssociation(Integer teamId) {
        ensureTeamExists(teamId);
        teamFormAccessConfigRepository.deleteById(teamId);
        teamFormRepository.deleteByTeamId(teamId);
    }

    @Override
    public List<FormNode> getFormTreeByTeamId(Integer teamId) {
        ensureTeamExists(teamId);
        TeamFormAssociationMode mode = getAssociationModeOrLegacy(teamId);
        List<FormNode> fullTree = formNodeRepository.findAll();

        if (mode == null) {
            return List.of();
        }

        if (mode == TeamFormAssociationMode.ALL_FORMS) {
            return cloneForest(fullTree);
        }

        Set<String> associatedNodeIds = teamFormRepository.findFormIdsByTeamId(teamId);
        List<FormNode> filteredTree = new ArrayList<>();
        for (FormNode root : fullTree) {
            FormNode filtered = mode == TeamFormAssociationMode.FOLDER_NODE
                    ? filterTreeByFolderNodeIds(root, associatedNodeIds)
                    : filterTreeByFormNodeIds(root, associatedNodeIds);
            if (filtered != null) {
                filteredTree.add(filtered);
            }
        }

        return filteredTree;
    }

    private void validateAssociationRequest(TeamFormAssociationMode mode, List<String> nodeIds) {
        if (mode == null) {
            throw new IllegalArgumentException("associationMode is required");
        }

        if (mode == TeamFormAssociationMode.ALL_FORMS && !nodeIds.isEmpty()) {
            throw new IllegalArgumentException("nodeIds must be empty for mode " + mode);
        }

        if (mode == TeamFormAssociationMode.ALL_FORMS) {
            return;
        }

        Map<String, FormNode> nodeIndex = buildNodeIndex(formNodeRepository.findAll());
        for (String nodeId : nodeIds) {
            FormNode node = nodeIndex.get(nodeId);
            if (node == null) {
                throw new IllegalArgumentException("Node not found: " + nodeId);
            }

            if (mode == TeamFormAssociationMode.FORM_NODE && !isDocumentNode(node)) {
                throw new IllegalArgumentException("FORM_NODE mode only accepts document nodes: " + nodeId);
            }

            if (mode == TeamFormAssociationMode.FOLDER_NODE && !isFolderLikeNode(node)) {
                throw new IllegalArgumentException("FOLDER_NODE mode only accepts folder nodes: " + nodeId);
            }
        }
    }

    private TeamFormAssociationMode getAssociationModeOrLegacy(Integer teamId) {
        Optional<TeamFormAccessConfig> config = teamFormAccessConfigRepository.findById(teamId);
        if (config.isPresent()) {
            return toApiMode(config.get().getAssociationMode());
        }

        return teamFormRepository.findFormIdsByTeamId(teamId).isEmpty()
                ? null
                : TeamFormAssociationMode.FORM_NODE;
    }

    private void saveAssociationMode(Integer teamId, Team team, TeamFormAssociationMode mode) {
        com.fps.shared.enums.TeamFormAssociationMode persistedMode = toPersistedMode(mode);

        Optional<TeamFormAccessConfig> existingConfig = teamFormAccessConfigRepository.findById(teamId);
        if (existingConfig.isPresent()) {
            TeamFormAccessConfig config = existingConfig.get();
            config.setTeam(team);
            config.setAssociationMode(persistedMode);
            teamFormAccessConfigRepository.save(config);
            return;
        }

        TeamFormAccessConfig config = new TeamFormAccessConfig();
        config.setTeam(team);
        config.setAssociationMode(persistedMode);
        teamFormAccessConfigRepository.save(config);
    }

    private TeamFormAssociationMode toApiMode(com.fps.shared.enums.TeamFormAssociationMode mode) {
        return switch (mode) {
            case FORM_NODE -> TeamFormAssociationMode.FORM_NODE;
            case FOLDER_NODE -> TeamFormAssociationMode.FOLDER_NODE;
            case ALL_FORMS -> TeamFormAssociationMode.ALL_FORMS;
        };
    }

    private com.fps.shared.enums.TeamFormAssociationMode toPersistedMode(TeamFormAssociationMode mode) {
        return switch (mode) {
            case FORM_NODE -> com.fps.shared.enums.TeamFormAssociationMode.FORM_NODE;
            case FOLDER_NODE -> com.fps.shared.enums.TeamFormAssociationMode.FOLDER_NODE;
            case ALL_FORMS -> com.fps.shared.enums.TeamFormAssociationMode.ALL_FORMS;
        };
    }

    private Team ensureTeamExists(Integer teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new EntityNotFoundException("Team not found"));
    }

    private List<String> normalizeNodeIds(List<String> nodeIds) {
        if (nodeIds == null || nodeIds.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<String> distinctNodeIds = new LinkedHashSet<>();
        for (String nodeId : nodeIds) {
            if (Objects.nonNull(nodeId)) {
                distinctNodeIds.add(nodeId);
            }
        }
        return new ArrayList<>(distinctNodeIds);
    }

    private Map<String, FormNode> buildNodeIndex(List<FormNode> roots) {
        Map<String, FormNode> nodeIndex = new HashMap<>();
        for (FormNode root : roots) {
            indexNode(root, nodeIndex);
        }
        return nodeIndex;
    }

    private void indexNode(FormNode node, Map<String, FormNode> nodeIndex) {
        if (node == null) {
            return;
        }

        nodeIndex.put(node.getId(), node);
        for (FormNode child : safeChildren(node)) {
            indexNode(child, nodeIndex);
        }
    }

    private FormNode filterTreeByFormNodeIds(FormNode node, Set<String> allowedFormNodeIds) {
        if (node == null) {
            return null;
        }

        if (isDocumentNode(node)) {
            return allowedFormNodeIds.contains(node.getId()) ? cloneSubtree(node) : null;
        }

        List<FormNode> filteredChildren = new ArrayList<>();
        for (FormNode childNode : safeChildren(node)) {
            FormNode filteredChild = filterTreeByFormNodeIds(childNode, allowedFormNodeIds);
            if (filteredChild != null) {
                filteredChildren.add(filteredChild);
            }
        }

        if (filteredChildren.isEmpty()) {
            return null;
        }

        FormNode cloned = cloneNodeWithoutChildren(node);
        cloned.setChildren(filteredChildren);
        return cloned;
    }

    private FormNode filterTreeByFolderNodeIds(FormNode node, Set<String> allowedFolderNodeIds) {
        if (node == null) {
            return null;
        }

        if (isFolderLikeNode(node) && allowedFolderNodeIds.contains(node.getId())) {
            return cloneSubtree(node);
        }

        List<FormNode> filteredChildren = new ArrayList<>();
        for (FormNode childNode : safeChildren(node)) {
            FormNode filteredChild = filterTreeByFolderNodeIds(childNode, allowedFolderNodeIds);
            if (filteredChild != null) {
                filteredChildren.add(filteredChild);
            }
        }

        if (filteredChildren.isEmpty()) {
            return null;
        }

        FormNode cloned = cloneNodeWithoutChildren(node);
        cloned.setChildren(filteredChildren);
        return cloned;
    }

    private boolean isDocumentNode(FormNode node) {
        return node != null && "document".equalsIgnoreCase(node.getNodeType());
    }

    private boolean isFolderLikeNode(FormNode node) {
        return node != null && !isDocumentNode(node);
    }

    private List<FormNode> cloneForest(List<FormNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return List.of();
        }

        return nodes.stream()
                .map(this::cloneSubtree)
                .toList();
    }

    private FormNode cloneSubtree(FormNode node) {
        FormNode cloned = cloneNodeWithoutChildren(node);
        List<FormNode> clonedChildren = new ArrayList<>();
        for (FormNode child : safeChildren(node)) {
            clonedChildren.add(cloneSubtree(child));
        }
        cloned.setChildren(clonedChildren);
        return cloned;
    }

    private FormNode cloneNodeWithoutChildren(FormNode node) {
        FormNode cloned = new FormNode();
        cloned.setId(node.getId());
        cloned.setLabel(node.getLabel());
        cloned.setNodeType(node.getNodeType());
        cloned.setQcFormTemplateId(node.getQcFormTemplateId());
        cloned.setChildren(new ArrayList<>());
        return cloned;
    }

    private List<FormNode> safeChildren(FormNode node) {
        if (node == null || node.getChildren() == null) {
            return Collections.emptyList();
        }
        return node.getChildren();
    }
}
