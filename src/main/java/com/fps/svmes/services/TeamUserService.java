//package com.fps.svmes.services;
//
//import com.fps.svmes.dto.dtos.user.TeamUserDTO;
//import com.fps.svmes.dto.dtos.user.UserDTO;
//import com.fps.svmes.dto.dtos.user.UserForTeamTableDTO;
//
//import java.util.List;
//
//public interface TeamUserService {
//    void assignUserToTeams(Integer userId, List<Integer> teamIds); // Changed Integer to Long
//    void assignUsersToTeam(Integer teamId, List<Integer> userIds); // Changed Integer to Long
//    void removeUserFromTeam(Integer userId, Integer teamId);
//    void removeUserFromAllTeams(Integer userId);
//    void removeTeamFromAllUsers(Integer teamId);
//    void removeUsersFromTeam(Integer teamId, List<Integer> userIds);
//    List<TeamUserDTO> getTeamsForUser(Integer userId);
//    List<UserDTO> getUsersForTeam(Integer teamId);
//    List<TeamUserDTO> getAllTeamUsers();
////    void setTeamLeader(Long teamId, Long leaderId);
////    List<TeamUserDTO> getLedTeamsForUser(Long userId);
//}
