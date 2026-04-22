//package com.fps.svmes.controllers;
//
//import com.fps.svmes.dto.dtos.user.TeamUserDTO;
//import com.fps.svmes.dto.dtos.user.UserDTO;
//import com.fps.svmes.dto.dtos.user.UserForTeamTableDTO;
//import com.fps.svmes.dto.responses.ResponseResult;
//import com.fps.svmes.services.TeamUserService;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@RestController
//@Slf4j
//@RequestMapping("/team-users")
//@RequiredArgsConstructor
//@Tag(name = "Team-User API", description = "API for managing Team-User relationships")
//public class TeamUserController {
//
//    @Autowired
//    private final TeamUserService teamUserService;
//
//    public static Logger logger = LoggerFactory.getLogger(TeamUserController.class);
//
//    @PostMapping("/users/{userId}/teams")
//    @Operation(summary = "Assign user to multiple teams", description = "Assign a single user to multiple teams")
//    public ResponseResult<Void> assignUserToTeams(@PathVariable Integer userId, @RequestBody List<Integer> teamIds) {
//        try {
//            teamUserService.assignUserToTeams(userId, teamIds);
//            logger.info("User {} assigned to teams {}", userId, teamIds);
//            return ResponseResult.success(null);
//        } catch (Exception e) {
//            logger.error("Error assigning user {} to teams {}", userId, teamIds, e);
//            return ResponseResult.fail("Error assigning user to teams", e);
//        }
//    }
//
//    @PostMapping("/teams/{teamId}/users")
//    @Operation(summary = "Assign multiple users to a team", description = "Assign multiple users to a single team")
//    public ResponseResult<Void> assignUsersToTeam(@PathVariable Integer teamId, @RequestBody List<Integer> userIds) {
//        try {
//            teamUserService.assignUsersToTeam(teamId, userIds);
//            logger.info("Users {} assigned to team {}", userIds, teamId);
//            return ResponseResult.success(null);
//        } catch (Exception e) {
//            logger.error("Error assigning users {} to team {}", userIds, teamId, e);
//            return ResponseResult.fail("Error assigning users to team", e);
//        }
//    }
//
//    @DeleteMapping("/users/{userId}/teams/{teamId}")
//    @Operation(summary = "Remove a user from a team", description = "Unassign a specific user from a specific team")
//    public ResponseResult<Void> removeUserFromTeam(@PathVariable Integer userId, @PathVariable Integer teamId) {
//        try {
//            teamUserService.removeUserFromTeam(userId, teamId);
//            logger.info("User {} removed from team {}", userId, teamId);
//            return ResponseResult.success(null);
//        } catch (Exception e) {
//            logger.error("Error removing user {} from team {}", userId, teamId, e);
//            return ResponseResult.fail("Error removing user from team", e);
//        }
//    }
//
//    @DeleteMapping("/users/{userId}/teams")
//    @Operation(summary = "Remove user from all teams", description = "Unassign a specific user from all teams")
//    public ResponseResult<Void> removeUserFromAllTeams(@PathVariable Integer userId) {
//        try {
//            teamUserService.removeUserFromAllTeams(userId);
//            logger.info("User {} removed from all teams", userId);
//            return ResponseResult.success(null);
//        } catch (Exception e) {
//            logger.error("Error removing user {} from all teams", userId, e);
//            return ResponseResult.fail("Error removing user from all teams", e);
//        }
//    }
//
//    @DeleteMapping("/teams/{teamId}/users/all")
//    @Operation(summary = "Remove all users from a team", description = "Unassign all users from a specific team")
//    public ResponseResult<Void> removeTeamFromAllUsers(@PathVariable Integer teamId) {
//        try {
//            teamUserService.removeTeamFromAllUsers(teamId);
//            logger.info("All users removed from team {}", teamId);
//            return ResponseResult.success(null);
//        } catch (Exception e) {
//            logger.error("Error removing all users from team {}", teamId, e);
//            return ResponseResult.fail("Error removing all users from team", e);
//        }
//    }
//
//    @DeleteMapping("/teams/{teamId}/users")
//    @Operation(summary = "Remove multiple users from a team", description = "Unassign multiple users from a specific team")
//    public ResponseResult<Void> removeUsersFromTeam(@PathVariable Integer teamId, @RequestBody List<Integer> userIds) {
//        try {
//            teamUserService.removeUsersFromTeam(teamId, userIds);
//            logger.info("Users {} removed from team {}", userIds, teamId);
//            return ResponseResult.success(null);
//        } catch (Exception e) {
//            logger.error("Error removing users {} from team {}", userIds, teamId, e);
//            return ResponseResult.fail("Error removing users from team", e);
//        }
//    }
//
//    @GetMapping("/users/{userId}/teams")
//    @Operation(summary = "Get teams for user", description = "Retrieve all teams assigned to a specific user")
//    public ResponseResult<List<TeamUserDTO>> getTeamsForUser(@PathVariable Integer userId) {
//        try {
//            List<TeamUserDTO> teams = teamUserService.getTeamsForUser(userId);
//            logger.info("Teams for user {} retrieved: {}", userId, teams);
//            return ResponseResult.success(teams);
//        } catch (Exception e) {
//            logger.error("Error retrieving teams for user {}", userId, e);
//            return ResponseResult.fail("Error retrieving teams for user", e);
//        }
//    }
//
//    @GetMapping("/teams/{teamId}/users")
//    @Operation(summary = "Get users for team", description = "Retrieve all users assigned to a specific team")
//    public ResponseResult<List<UserDTO>> getUsersForTeam(@PathVariable Integer teamId) {
//        try {
//            // Now returns a list of UserForTeamTableDTO
//            List<UserDTO> users = teamUserService.getUsersForTeam(teamId);
//            logger.info("Users for team {} retrieved: {}", teamId, users.size());
//            return ResponseResult.success(users);
//        } catch (Exception e) {
//            logger.error("Error retrieving users for team {}", teamId, e);
//            return ResponseResult.fail("Error retrieving users for team", e);
//        }
//    }
//
//    @GetMapping("")
//    @Operation(summary = "Get all team-user relationships", description = "Retrieve all team-user assignments")
//    public ResponseResult<List<TeamUserDTO>> getAllTeamUsers() {
//        try {
//            List<TeamUserDTO> allTeamUsers = teamUserService.getAllTeamUsers();
//            logger.info("All team-user relationships retrieved: {}", allTeamUsers);
//            return ResponseResult.success(allTeamUsers);
//        } catch (Exception e) {
//            logger.error("Error retrieving all team-user relationships", e);
//            return ResponseResult.fail("Error retrieving all team-user relationships", e);
//        }
//    }
//
////    @PutMapping("/teams/{teamId}/leader")
////    @Operation(summary = "Set or update team leader", description = "Assign or update the leader for a specific team")
////    public ResponseResult<Void> setTeamLeader(@PathVariable Long teamId, @RequestBody Long leaderId) {
////        try {
////            teamUserService.setTeamLeader(teamId, leaderId);
////            logger.info("Leader {} set for team {}", leaderId, teamId);
////            return ResponseResult.success(null);
////        } catch (Exception e) {
////            logger.error("Error setting leader {} for team {}", leaderId, teamId, e);
////            return ResponseResult.fail("Error setting team leader", e);
////        }
////    }
//
////    @GetMapping("/users/{userId}/led-teams")
////    @Operation(summary = "Get teams led by user", description = "Retrieve all teams where a specific user is the leader")
////    public ResponseResult<List<TeamUserDTO>> getLedTeamsForUser(@PathVariable Long userId) {
////        try {
////            List<TeamUserDTO> ledTeams = teamUserServi ce.getLedTeamsForUser(userId);
////            logger.info("Teams led by user {} retrieved: {}", userId, ledTeams);
////            return ResponseResult.success(ledTeams);
////        } catch (Exception e) {
////            logger.error("Error retrieving teams led by user {}", userId, e);
////            return ResponseResult.fail("Error retrieving teams led by user", e);
////        }
////    }
//}