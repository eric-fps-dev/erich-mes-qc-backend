//package com.fps.svmes.services.impl;
//
//import com.fps.svmes.dto.dtos.user.RoleDTO;
//import com.fps.svmes.dto.dtos.user.TeamUserDTO;
//import com.fps.svmes.dto.dtos.user.UserDTO;
//import com.fps.svmes.dto.dtos.user.UserForTeamTableDTO;
//import com.fps.svmes.models.sql.user.TeamUser;
//import com.fps.svmes.models.sql.user.TeamUserId;
//import com.fps.svmes.models.sql.user.User;
//import com.fps.svmes.repositories.jpaRepo.user.TeamRepository;
//import com.fps.svmes.repositories.jpaRepo.user.TeamUserRepository;
//import com.fps.svmes.repositories.jpaRepo.user.UserRepository;
//import com.fps.svmes.services.TeamUserService;
//import jakarta.transaction.Transactional;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.modelmapper.ModelMapper;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//import java.util.Set;
//import java.util.stream.Collectors;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class TeamUserServiceImpl implements TeamUserService {
//
//    private final TeamUserRepository teamUserRepository;
//
//    private final TeamRepository teamRepository;
//
//    private final UserRepository userRepository;
//
//    @Autowired
//    private ModelMapper modelMapper;
//
//    // Assign user to teams. Teams with ancestor team will also get assigned with the user.
//    // Teams that already contain the user will be ignored.
//    @Override
//    public void assignUserToTeams(Integer userId, List<Integer> teamIds) {
//        if (teamIds == null || teamIds.isEmpty()) {
//            log.warn("assignUserToTeams called with empty teamIds – nothing to do.");
//            return;
//        }
//
//        Set<Integer> targetTeamIds = teamIds.stream().flatMap(tid -> teamRepository.findSelfAndAncestorIds(tid)
//                .stream())
//                .collect(Collectors.toSet());
//
//        Set<Integer> alreadyAssigned = teamUserRepository.findByIdUserId(userId)
//                .stream()
//                .map(tu -> tu.getTeam().getId()).collect(Collectors.toSet());
//
//        targetTeamIds.removeAll(alreadyAssigned);
//        if (targetTeamIds.isEmpty()){
//            log.info("User {} is already assigned to all requested teams / ancestors.", userId);
//            return;
//        }
//
//        List<TeamUser> teamUsers = targetTeamIds.stream()
//                .map(teamId -> {
//                    TeamUser teamUser = new TeamUser(new TeamUserId(teamId, userId));
//                    teamUser.setTeam(teamRepository.findById(Math.toIntExact(teamId))
//                            .orElseThrow(() -> new IllegalArgumentException("Team not found: " + teamId)));
//                    teamUser.setUser(userRepository.findById(Math.toIntExact(userId))
//                            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId)));
//                    return teamUser;
//                })
//                .collect(Collectors.toList());
//
//        teamUserRepository.saveAll(teamUsers);
//        log.info("Assigned user {} to teams {}", userId, teamIds);
//    }
//
//    @Override
//    public void assignUsersToTeam(Integer teamId, List<Integer> userIds) {
//        List<TeamUser> teamUsers = userIds.stream()
//                .map(userId -> {
//                    TeamUser teamUser = new TeamUser(new TeamUserId(teamId, userId));
//                    teamUser.setTeam(teamRepository.findById(Math.toIntExact(teamId))
//                            .orElseThrow(() -> new IllegalArgumentException("Team not found: " + teamId)));
//                    teamUser.setUser(userRepository.findById(Math.toIntExact(userId))
//                            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId)));
//                    return teamUser;
//                })
//                .collect(Collectors.toList());
//        teamUserRepository.saveAll(teamUsers);
//        log.info("Assigned users {} to team {}", userIds, teamId);
//    }
//
//    @Override
//    @Transactional
//    public void removeUserFromTeam(Integer userId, Integer teamId) {
//        teamUserRepository.deleteById(new TeamUserId(teamId, userId));
//        log.info("Removed user {} from team {}", userId, teamId);
//    }
//
//    @Override
//    @Transactional
//    public void removeUserFromAllTeams(Integer userId) {
//        teamUserRepository.deleteByIdUserId(userId);
//        log.info("Removed user {} from all teams", userId);
//    }
//
//    @Override
//    @Transactional
//    public void removeTeamFromAllUsers(Integer teamId) {
//        teamUserRepository.deleteByIdTeamId(teamId);
//        log.info("Removed all users from team {}", teamId);
//    }
//
//    @Override
//    @Transactional
//    public void removeUsersFromTeam(Integer teamId, List<Integer> userIds) {
//        userIds.forEach(userId ->
//                teamUserRepository.deleteById(new TeamUserId(teamId, userId))
//        );
//        log.info("Removed users {} from team {}", userIds, teamId);
//    }
//
//    @Override
//    public List<TeamUserDTO> getTeamsForUser(Integer userId) {
//        List<TeamUser> teamUsers = teamUserRepository.findByIdUserId(userId);
//        log.info("Retrieved teams for user {}: {}", userId, teamUsers.size());
//        return teamUsers.stream()
//                .map(teamUser -> new TeamUserDTO(
//                        teamUser.getId().getUserId(),
//                        teamUser.getId().getTeamId()
//                ))
//                .collect(Collectors.toList());
//    }
//
//    @Override
//    @Transactional
//    public List<UserDTO> getUsersForTeam(Integer teamId) {
//        // 1) Find all TeamUser records for this teamId
//        List<TeamUser> teamUsers = teamUserRepository.findByIdTeamId(teamId);
//        log.info("Retrieved users for team {}: {}", teamId, teamUsers.size());
//
//        return teamUsers.stream()
//                .map(teamUser -> modelMapper.map(teamUser.getUser(), UserDTO.class))
//                .toList();
//    }
//
//    @Override
//    public List<TeamUserDTO> getAllTeamUsers() {
//        List<TeamUser> teamUsers = teamUserRepository.findAll();
//        log.info("Retrieved all team-user relationships: {}", teamUsers.size());
//        return teamUsers.stream()
//                .map(teamUser -> {
//                    TeamUserId id = teamUser.getId(); // Retrieve the embedded ID
//                    if (id != null) {
//                        return new TeamUserDTO(id.getUserId(), id.getTeamId());
//                    } else {
//                        log.warn("TeamUser entity with null ID found: {}", teamUser);
//                        return new TeamUserDTO(null, null);
//                    }
//                })
//                .collect(Collectors.toList());
//    }
//}