//package com.fps.svmes.services;
//
//import com.fps.svmes.dto.dtos.user.LeaderDTO;
//import com.fps.svmes.dto.dtos.user.TeamDTO;
//import com.fps.svmes.dto.requests.TeamRequest;
//import jakarta.validation.Valid;
//import java.util.List;
//
//public interface TeamService {
//    TeamDTO createTeam(TeamRequest TeamRequest);
//    TeamDTO updateTeam(Integer id, @Valid TeamRequest teamDTO);
//    void setTeamLeader(Integer teamId, Integer leaderId);
//    void clearTeamLeader(Integer teamId);
//    TeamDTO getTeamById(Integer id);
//    List<TeamDTO> getFullTeamTree();
//    void softDeleteTeam(Integer id, Integer userId);
//    void hardDeleteTeam(Integer id);
//    TeamDTO getTeamDTOByTeamLeadId(Integer id);
//    List<LeaderDTO> getCurrentLeaders();
//    int getDepth(Integer teamId);
//    void syncSelfAndDescendantTeamMembers(Integer teamId, List<Integer> parentAllowedUserIds);
//    void syncSelfAndDescendantTeamForms(Integer teamId, List<String> parentAllowedFormIds);
//    void verifyAndUpdateLeader(Integer teamId, Short roleId);
//    void removeOrphanLeadership(Integer userId);
//}