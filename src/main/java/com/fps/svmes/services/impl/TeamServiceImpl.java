//package com.fps.svmes.services.impl;
//
//import com.fps.svmes.dto.dtos.user.LeaderDTO;
//import com.fps.svmes.dto.dtos.user.TeamDTO;
//import com.fps.svmes.dto.dtos.user.TeamUserDTO;
//import com.fps.svmes.dto.requests.TeamRequest;
//import com.fps.svmes.models.sql.user.Team;
//import com.fps.svmes.models.sql.user.TeamUserId;
//import com.fps.svmes.repositories.jpaRepo.user.TeamFormRepository;
//import com.fps.svmes.repositories.jpaRepo.user.TeamRepository;
//import com.fps.svmes.repositories.jpaRepo.user.TeamUserRepository;
//import com.fps.svmes.services.TeamService;
//import com.fps.svmes.services.TeamUserService;
//import com.fps.svmes.repositories.jpaRepo.user.UserRepository;
//import com.fps.svmes.models.sql.user.User;
//
//import jakarta.persistence.EntityNotFoundException;
//import jakarta.validation.Valid;
//
//import lombok.RequiredArgsConstructor;
//
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import org.modelmapper.ModelMapper;
//
//import java.util.*;
//
//@Service
//@RequiredArgsConstructor
//public class TeamServiceImpl implements TeamService {
//
//    private final TeamRepository teamRepository;
//
//    private final UserRepository userRepository;
//
//    private final TeamUserRepository teamUserRepository;
//
//    private final TeamFormRepository teamFormRepository;
//
//    private final TeamUserService teamUserService;
//
//    private final ModelMapper modelMapper;
//
//    @Override
//    @Transactional
//    public TeamDTO createTeam(TeamRequest teamRequest) {
//        Team team = modelMapper.map(teamRequest, Team.class);
//
//        if (teamRequest.getCreatedBy() != null) {
//            team.setCreationDetails(teamRequest.getCreatedBy(), teamRequest.getStatus());
//        }
//
//        if (teamRequest.getParentId() != null) {
//            Team parentTeam = teamRepository.findById(teamRequest.getParentId())
//                    .orElseThrow(() -> new IllegalArgumentException("Parent team not found: " + teamRequest.getParentId()));
//            team.setParent(parentTeam);
//        }
//
//        if (teamRequest.getLeaderId() != null) {
//            User leader = userRepository.findById(teamRequest.getLeaderId())
//                    .orElseThrow(() -> new IllegalArgumentException("Leader not found with ID: " + teamRequest.getLeaderId()));
//
//            // If leader is already assigned to another team, change that team's leader to null
//            Optional<Team> optionalOtherTeam = teamRepository.findByLeaderId(teamRequest.getLeaderId());
//
//            if (optionalOtherTeam.isPresent()) {
//                Team otherTeam = optionalOtherTeam.get();
//                otherTeam.setLeader(null);
//
//                // Remove the leader member since that leader is no longer within the team
//                teamUserService.removeUserFromTeam(leader.getId(), otherTeam.getId());
//
//                otherTeam.setUpdateDetails(teamRequest.getCreatedBy(), otherTeam.getStatus());
//            }
//
//            team.setLeader(leader);
//        }
//
//        team = teamRepository.save(team);
//        return modelMapper.map(team, TeamDTO.class);
//    }
//
//    @Override
//    @Transactional
//    public TeamDTO updateTeam(Integer id, @Valid TeamRequest teamRequest) {
//        Team team = teamRepository.findById(id).orElseThrow(() -> new RuntimeException("Team not found"));
//
//        // Map only non-null properties from request to team entity
//        if (teamRequest.getName() != null) team.setName(teamRequest.getName());
//        if (teamRequest.getType() != null) team.setType(teamRequest.getType());
//        if (teamRequest.getStartTime() != null) team.setStartTime(teamRequest.getStartTime());
//        if (teamRequest.getEndTime() != null) team.setEndTime(teamRequest.getEndTime());
//        if (teamRequest.getDescription() != null) team.setDescription(teamRequest.getDescription());
//        if (teamRequest.getUpdatedBy() != null) team.setUpdateDetails(teamRequest.getUpdatedBy(),teamRequest.getStatus());
//
//        if (teamRequest.getParentId() != null) {
//            Team parentTeam = teamRepository.findById(teamRequest.getParentId())
//                    .orElseThrow(() -> new IllegalArgumentException("Parent team not found: " + teamRequest.getParentId()));
//            team.setParent(parentTeam);
//        } else {
//            team.setParent(null);
//        }
//
//        if (teamRequest.getLeaderId() != null) {
//            User newLeader = userRepository.findById(teamRequest.getLeaderId())
//                    .orElseThrow(() -> new RuntimeException("Leader not found"));
//
//            // Current (old) leader of this team – may be null
//            User oldLeader = team.getLeader();
//
//            // Other team that currently has this new leader - may be null
//            Optional<Team> optionalOtherTeam = teamRepository.findByLeaderId(newLeader.getId());
//
//            if (Objects.equals(oldLeader, newLeader)) {
//                // Skip since same leader already assigned
//            } else if (optionalOtherTeam.isEmpty() || optionalOtherTeam.get().getId().equals(team.getId())) {
//                team.setLeader(newLeader);
//            } else {
//                Team otherTeam = optionalOtherTeam.get();
//                otherTeam.setLeader(oldLeader);
//
//                // Remove previous leader from the member list
//                teamUserService.removeUserFromTeam(newLeader.getId(), otherTeam.getId());
//
//                if (oldLeader != null)
//                {
//                    // Add leader as a member to other team
//                    teamUserService.assignUserToTeams(oldLeader.getId(), List.of(otherTeam.getId()));
//                }
//
//                if (teamRequest.getUpdatedBy() != null) {
//                    otherTeam.setUpdateDetails(teamRequest.getUpdatedBy(), otherTeam.getStatus());
//                }
//
//                team.setLeader(newLeader);
//            }
//        } else {
//            team.setLeader(null);
//        }
//
//        team = teamRepository.save(team);
//        return modelMapper.map(team, TeamDTO.class);
//    }
//
//    @Override
//    @Transactional
//    public void setTeamLeader(Integer teamId, Integer leaderId) {
//        Team team = teamRepository.findById(teamId)
//                .orElseThrow(() -> new EntityNotFoundException("Team " + teamId + " not found"));
//
//        User leader = userRepository.findById(leaderId)
//                .orElseThrow(() -> new EntityNotFoundException("User " + leaderId + " not found"));
//
//        // No changes needed
//        if (leader.equals(team.getLeader())) {
//            return;
//        }
//
//        if (team.getLeader() != null) {
//            Integer oldLeaderId = team.getLeader().getId();
//            teamUserService.removeUserFromTeam(oldLeaderId, teamId);
//            team.setLeader(null);
//        }
//
//        Optional<Team> optionalOldTeam = teamRepository.findByLeaderId(leaderId);
//
//        if (optionalOldTeam.isPresent() && !optionalOldTeam.get().getId().equals(teamId)) {
//            Team oldTeam = optionalOldTeam.get();
//
//            // Clear user's leadership on the previous team (if different)
//            oldTeam.setLeader(null);
//
//            // remove that user's membership from the previous team
//            teamUserService.removeUserFromTeam(leaderId, oldTeam.getId());
//            teamRepository.save(oldTeam);
//        }
//
//        team.setLeader(leader);
//        teamUserService.assignUserToTeams(leaderId, List.of(team.getId()));
//        teamRepository.save(team);
//    }
//
//    @Transactional
//    public void clearTeamLeader(Integer teamId) {
//        Team team = teamRepository.findById(teamId).orElseThrow(()-> new EntityNotFoundException("Team " + teamId));
//
//        if (team.getLeader() != null) {
//            teamUserService.removeUserFromTeam(team.getLeader().getId(), teamId);
//            team.setLeader(null);
//            teamRepository.save(team);
//        }
//    }
//
//    @Override
//    @Transactional
//    public TeamDTO getTeamById(Integer id) {
//        Optional<Team> optionalTeam = teamRepository.findById(id);
//        if (optionalTeam.isPresent() && optionalTeam.get().getStatus().equals(1)){
//            Team team = optionalTeam.get();
//            List<Integer> ids = teamRepository.findSelfAndAncestorIds(id);
//
//            return mapWithChildren(team, ids.size());
//        } else {
//            throw new RuntimeException("Team with id " + id + "not found");
//        }
//    }
//
//    @Override
//    @Transactional(readOnly=true)
//    public List<TeamDTO> getFullTeamTree() {
//        List<Team> roots = teamRepository.findByParentIsNull();
//        return roots.stream()
//                .filter(team -> team.getStatus().equals(1))
//                .sorted(Comparator.comparing(Team::getId))
//                .map(t -> mapWithChildren(t,1))
//                .toList();
//    }
//
//    @Override
//    @Transactional
//    public void softDeleteTeam(Integer id, Integer userId) {
//        Team team = teamRepository.findById(id).orElseThrow(() -> new RuntimeException("Team not found"));
//        softDeleteRecursively(team, userId);
//        teamRepository.save(team);
//    }
//
//    // Helper to soft-delete a team's leader & member association & form associations, repeat for all descendant teams
//    private void softDeleteRecursively(Team team, Integer userId) {
//        team.setUpdateDetails(userId, 0);
//        team.setLeader(null);
//
//        // Clean up team user association for this team
//        teamUserRepository.deleteByIdTeamId(team.getId());
//
//        // Clean up team form association for this team
//        teamFormRepository.deleteByTeamId(team.getId());
//
//        if (team.getChildren() != null && !team.getChildren().isEmpty()) {
//            team.getChildren().forEach(child -> softDeleteRecursively(child, userId));
//        }
//    }
//
//    @Override
//    public void hardDeleteTeam(Integer id) {
//        if (!teamRepository.existsById(id)) {
//            throw new RuntimeException("Team not found");
//        }
//
//        teamRepository.deleteById(id); // Permanently deletes the record
//
//        // Just in case. Not necessary as team user table and team form table should already have cascade on delete
//        // foreign key constraint to automatically handle for this
//        teamUserRepository.deleteByIdTeamId(id);
//        teamFormRepository.deleteByTeamId(id);
//    }
//
//    @Override
//    @Transactional
//    public TeamDTO getTeamDTOByTeamLeadId(Integer id) {
//        Optional<Team> optionalTeam = teamRepository.findByLeaderId(id);
//
//        if (optionalTeam.isEmpty()) {
//            throw new EntityNotFoundException("No team found for leader ID: " + id);
//        }
//
//        return modelMapper.map(optionalTeam.get(), TeamDTO.class);
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public List<LeaderDTO> getCurrentLeaders() {
//        return teamRepository.findAll()
//                .stream()
//                .filter(team -> team.getStatus().equals(1) && team.getLeader() != null)      // Keep only teams that have a leader
//                .map(team ->  new LeaderDTO(team.getLeader().getId(),
//                        team.getLeader().getName(),
//                        team.getId(),
//                        team.getName()))
//                .toList();
//    }
//
//    /**
//     * Returns the depth of the target team specified by team id in the hierarchy.
//     * root           → 1
//     * child of root  → 2
//     * grand‑child    → 3 …
//     */
//    @Override
//    @Transactional(readOnly = true)
//    public int getDepth(Integer teamId) {
//        Team team = teamRepository.findById(teamId)
//                .orElseThrow(() -> new RuntimeException("Team not found: " + teamId));
//
//        int depth = 1;
//        Team current  = team;
//
//        while (current.getParent() != null) {
//            depth++;
//            current = current.getParent();
//        }
//
//        return depth;
//    }
//
//    @Override
//    @Transactional
//    public void syncSelfAndDescendantTeamMembers(Integer teamId, List<Integer> parentAllowedUserIds) {
//        Set<Integer> allowedSet = new HashSet<>(parentAllowedUserIds);
//
//        List<Integer> childTeamIds = descendantTeamIds(teamId);
//        if (childTeamIds.isEmpty()) return;
//
//        for (Integer childTeamId : childTeamIds) {
//            List<Integer> childTeamUserIds = teamUserRepository.findByIdTeamId(childTeamId)
//                    .stream()
//                    .map(teamUser -> teamUser.getUser().getId())
//                    .toList();
//
//            List<Integer> targetRemovalUserIds = childTeamUserIds.stream()
//                    .filter(id -> !allowedSet.contains(id))
//                    .toList();
//
//            if (!targetRemovalUserIds.isEmpty()) {
////                teamUserRepository.deleteByTeamIdAndUserIdIn(childTeamId, targetRemovalUserIds);
//                for (int userId: targetRemovalUserIds){
//                    // For any non-allowed user, remove its team user association and leadership
//                    teamUserRepository.deleteById(new TeamUserId(childTeamId, userId));
//
//                    Optional<Team> leadingTeam = teamRepository.findByLeaderId(userId);
//                    leadingTeam.ifPresent(team -> team.setLeader(null));
//                }
//            }
//        }
//    }
//
//    @Override
//    @Transactional
//    public void syncSelfAndDescendantTeamForms(Integer teamId, List<String> parentAllowedFormIds) {
//        Set<String> allowedSet = new HashSet<>(parentAllowedFormIds);
//
//        List<Integer> allChildTeamIds = descendantTeamIds(teamId);
//        if (allChildTeamIds.isEmpty()) return;
//
//        for (Integer childTeamId : allChildTeamIds) {
//            List<String> childForms =
//                    teamFormRepository.findByTeamId(childTeamId)
//                            .stream()
//                            .map(teamForm -> teamForm.getId().getFormId())
//                            .toList();
//
//            List<String> targetRemovalFormIds = childForms.stream()
//                    .filter(id -> !allowedSet.contains(id))
//                    .toList();
//
//            if (!targetRemovalFormIds.isEmpty()) {
//                teamFormRepository.deleteByTeamIdAndFormIdIn(childTeamId, targetRemovalFormIds);
//            }
//        }
//    }
//
//    /**
//     * Set target team's leader to null if role is not allowed, determine by team's depth/level
//     * depth 1: allow supervisor only
//     * depth 2 and below: allow team lead only
//     */
//    @Transactional
//    public void verifyAndUpdateLeader(Integer teamId, Short roleId) {
//        int teamDepth = getDepth(teamId);
//        Team team = teamRepository.findById(teamId)
//                .orElseThrow(() -> new EntityNotFoundException("Team " + teamId + " not found"));
//
//        if (roleId == 1) {
//            // Supervisor role only allowed on team with depth 1
//            if (teamDepth != 1) {
//                team.setLeader(null);
//            }
//        } else if (roleId == 2) {
//            // Worker role is not allowed to be leader
//            team.setLeader(null);
//        } else if (roleId == 3) {
//            // Team lead role only allowed on depth other than 1
//            if (teamDepth == 1) {
//             team.setLeader(null);
//            }
//        } else if (roleId == 4) {
//            // Manager role only allowed on team with depth 1
//            if (teamDepth != 1) {
//                team.setLeader(null);
//            }
//        }
//
//        teamRepository.save(team);
//    }
//
//    // Cleanup orphan leadership association: Remove this user's leadership association if exist and ascendant team's membership is missing
//    @Override
//    @Transactional
//    public void removeOrphanLeadership(Integer userId) {
//        userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("User does not exist, skipping removeOrphanLeadership method"));
//
//        Optional<Team> optionalUserLeadingTeam = teamRepository.findByLeaderId(userId);
//
//        if (optionalUserLeadingTeam.isPresent()) {
//            Team userLeadingTeam = optionalUserLeadingTeam.get();
//
//            // all membership association team ids for this user
//            List<Integer> membershipTeamIds = teamUserService.getTeamsForUser(userId).stream().map(TeamUserDTO::getTeamId).toList();
//            List<Integer> leadingTeamAncestorIds = teamRepository.findSelfAndAncestorIds(userLeadingTeam.getId());
//
//            // if membershipTeamIds does not include leading team's ancestor id,
//            // then the userLeadingTeam association is invalid and should be removed
//            leadingTeamAncestorIds.forEach(ancestor_id -> {
//                if (!membershipTeamIds.contains(ancestor_id)) {
//                    userLeadingTeam.setLeader(null);
//                }
//            });
//
//            teamRepository.save(userLeadingTeam);
//        }
//    }
//
//    /**
//     * ids of *all* descendants (NOT including the team itself)
//     */
//    private List<Integer> descendantTeamIds(Integer teamId) {
//        List<Integer> all = teamRepository.findSelfAndDescendantIds(teamId);
//        all.remove(teamId);
//        return all;
//    }
//
//    private TeamDTO mapWithChildren(Team entity, int level) {
//        TeamDTO dto = modelMapper.map(entity, TeamDTO.class);
//        dto.setLevel(level);
//
//        dto.setAssociatedFormCount(teamFormRepository.findByTeamId(entity.getId()).size());
//        dto.setMemberCount(teamUserRepository.findByIdTeamId(entity.getId()).size());
//
//        if (entity.getParent() != null){
//            dto.setParentId(entity.getParent().getId());
//        }
//
//        if (entity.getChildren() != null && !entity.getChildren().isEmpty()) {
//            dto.setChildren(entity.getChildren().stream()
//                    .filter(child -> child.getStatus().equals(1))
//                    .sorted(Comparator.comparing(Team::getId))
//                    .map(c-> mapWithChildren(c, level + 1))
//                    .toList());
//        }
//
//        return dto;
//    }
//}
//
