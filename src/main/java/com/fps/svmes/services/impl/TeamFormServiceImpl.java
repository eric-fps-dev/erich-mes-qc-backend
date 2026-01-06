//package com.fps.svmes.services.impl;
//
//import com.fps.svmes.models.nosql.FormNode;
//import com.fps.svmes.models.sql.user.Team;
//import com.fps.svmes.models.sql.user.TeamForm;
//import com.fps.svmes.models.sql.user.TeamFormId;
//import com.fps.svmes.repositories.jpaRepo.user.TeamFormRepository;
//import com.fps.svmes.repositories.jpaRepo.user.TeamRepository;
//import com.fps.svmes.repositories.mongoRepo.FormNodeRepository;
//import com.fps.svmes.services.TeamFormService;
//import jakarta.persistence.EntityNotFoundException;
//import jakarta.transaction.Transactional;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//
//import java.util.ArrayList;
//import java.util.List;
//
//@Service
//@RequiredArgsConstructor
//public class TeamFormServiceImpl implements TeamFormService {
//    private final TeamFormRepository teamFormRepository;
//    private final TeamRepository teamRepository;
//    private final FormNodeRepository formNodeRepository;
//
//    @Transactional
//    @Override
//    public void assignFormToTeam(Integer teamId, String formId) {
//        Team team = teamRepository.findById(teamId)
//                .orElseThrow(() -> new EntityNotFoundException("Team not found"));
//
//        TeamFormId id = new TeamFormId(teamId, formId);
//
//        if (!teamFormRepository.existsById(id)) {
//            TeamForm teamForm = new TeamForm(id, team);
//            teamFormRepository.save(teamForm);
//        }
//    }
//
//    @Transactional
//    @Override
//    public void removeFormFromTeam(Integer teamId, String formId) {
//        teamFormRepository.deleteById(new TeamFormId(teamId, formId));
//    }
//
//    @Override
//    public List<String> getFormIdsByTeam(Integer teamId) {
//        return teamFormRepository.findByTeamId(teamId)
//                .stream()
//                .map(sf -> sf.getId().getFormId())
//                .toList();
//    }
//
//    @Transactional
//    @Override
//    public void removeAllFormsFromTeam(Integer teamId) {
//        teamFormRepository.deleteByTeamId(teamId);
//    }
//
//    @Override
//    public List<FormNode> getFormTreeByTeamId(Integer teamId) {
//        List<String> formIds = getFormIdsByTeam(teamId);
//        List<FormNode> fullTree = formNodeRepository.findAll();
//
//        List<FormNode> filteredTree = new ArrayList<>();
//        for (FormNode root: fullTree) {
//            FormNode filtered = filterTreeByFormIds(root, formIds);
//            if (filtered != null) {
//               filteredTree.add(filtered);
//            }
//        }
//
//        return filteredTree;
//    }
//
//    private FormNode filterTreeByFormIds(FormNode node, List<String> allowedId) {
//        if ("document".equalsIgnoreCase(node.getNodeType())) {
//            return allowedId.contains(node.getId()) ? node : null;
//        }
//
//        List<FormNode> filteredChildren = new ArrayList<>();
//        for (FormNode childNode: node.getChildren()) {
//            FormNode filteredChild = filterTreeByFormIds(childNode, allowedId);
//            if (filteredChild != null) {
//                filteredChildren.add(filteredChild);
//            }
//        }
//
//        if (!filteredChildren.isEmpty()) {
//            FormNode newNode = new FormNode();
//            newNode.setId(node.getId());
//            newNode.setLabel(node.getLabel());
//            newNode.setNodeType(node.getNodeType());
//            newNode.setQcFormTemplateId(node.getQcFormTemplateId());
//            newNode.setChildren(filteredChildren);
//            return newNode;
//        }
//
//        return null;
//    }
//}
