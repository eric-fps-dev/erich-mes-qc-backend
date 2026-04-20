//package com.fps.svmes.controllers;
//
//import com.fps.svmes.dto.dtos.user.LeaderDTO;
//import com.fps.svmes.dto.dtos.user.TeamDTO;
//import com.fps.svmes.dto.requests.TeamRequest;
//import com.fps.svmes.dto.responses.ResponseResult;
//import com.fps.svmes.services.TeamFormService;
//import com.fps.svmes.services.TeamService;
//import com.fps.svmes.services.TeamUserService;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/teams")
//@Tag(name = "Team Management API", description = "API for managing teams")
//public class TeamController {
//
//    @Autowired
//    private TeamService teamService;
//
//    @Autowired
//    private TeamFormService teamFormService;
//
//    @Autowired
//    private TeamUserService teamUserService;
//
//    private static final Logger logger = LoggerFactory.getLogger(TeamController.class);
//
//    /**
//     * Create a new team with the provided details.
//     *
//     * @param teamRequest Team request object containing updated data
//     * @return TeamDTO
//     */
//    @PostMapping
//    @Operation(summary = "Create a new team", description = "Create a new team with the provided details")
//    public ResponseResult<TeamDTO> createTeam(@RequestBody TeamRequest teamRequest) {
//        try {
//            TeamDTO createdTeam = teamService.createTeam(teamRequest);
//
//            // Setup member associations
//            teamUserService.assignUsersToTeam(createdTeam.getId(), teamRequest.getMemberIds());
//
//            // Sync leader as a member for all ascendant teams
//            if (teamRequest.getLeaderId() != null) {
//                teamUserService.assignUserToTeams(teamRequest.getLeaderId(), List.of(createdTeam.getId()));
//            }
//
//            // Setup form associations
//            for (String formId: teamRequest.getFormIds()) {
//                teamFormService.assignFormToTeam(createdTeam.getId(), formId);
//            }
//
//            return ResponseResult.success(createdTeam);
//        } catch (Exception e) {
//            logger.error("Error creating team", e);
//            return ResponseResult.fail("Failed to create team", e);
//        }
//    }
//
//    /**
//     * Update an existing team and sync children teams forms/members by removing association that are not included.
//     *
//     * @param id ID of the team to update
//     * @param teamRequest Team request object containing updated data
//     * @return TeamDTO
//     */
//    @PutMapping("/{id}")
//    @Operation(summary = "Update an existing team", description = "Update an existing team and sync children teams " +
//            "forms/members by removing association that are not included")
//    public ResponseResult<TeamDTO> updateTeam(
//            @PathVariable Integer id,
//            @RequestBody TeamRequest teamRequest
//    ) {
//        try {
//            TeamDTO updatedTeam = teamService.updateTeam(id, teamRequest);
//
//            // Sync Member associations
//            teamUserService.removeTeamFromAllUsers(id);
//
//            List<Integer> memberIds = teamRequest.getMemberIds();
//            teamUserService.assignUsersToTeam(id, memberIds);
//            teamService.syncSelfAndDescendantTeamMembers(id, memberIds);
//
//            // Sync leader as a member for all ascendant teams
//            if (teamRequest.getLeaderId() != null) {
//                teamUserService.assignUserToTeams(teamRequest.getLeaderId(), List.of(updatedTeam.getId()));
//            }
//
//            // Sync Form associations
//            List<String> formIds = teamRequest.getFormIds();
//            teamFormService.removeAllFormsFromTeam(id);
//
//            for (String formId: formIds) {
//                teamFormService.assignFormToTeam(id, formId);
//            }
//
//            teamService.syncSelfAndDescendantTeamForms(id, formIds);
//
//            return ResponseResult.success(updatedTeam);
//        } catch (Exception e) {
//            logger.error("Error updating team", e);
//            return ResponseResult.fail("Failed to update team", e);
//        }
//    }
//
//    /**
//     * Change an existing team leader, if leader or team does not exist then the operation is skip.
//     *
//     * @param teamId ID of the team to update
//     * @param leaderId user ID to set as leader of the target team
//     *
//     */
//    @PutMapping("/leadership/{teamId}/{leaderId}")
//    @Operation(summary = "Change an existing team's leader", description = "Change an existing team's leader'")
//    public ResponseResult<Void> setTeamLeader(@PathVariable Integer teamId, @PathVariable Integer leaderId) {
//        try {
//            teamService.setTeamLeader(teamId, leaderId);
//            return ResponseResult.success();
//        } catch (Exception e) {
//            logger.error("Error setting leader for team", e);
//            return ResponseResult.fail("Failed to set leader for team");
//        }
//    }
//
//    @PutMapping("/leadership/{teamId}")
//    @Operation(summary = "Set leader as null for an existing team", description = "Set leader as null for an existing team")
//    public ResponseResult<Void> clearTeamLeader(@PathVariable Integer teamId) {
//        try {
//            teamService.clearTeamLeader(teamId);
//            return ResponseResult.success();
//        } catch (Exception e) {
//            logger.error("Error clearing leader for team", e);
//            return ResponseResult.fail("Failed to clear leader for team");
//        }
//    }
//
//    /**
//     * Get a specific team by ID.
//     *
//     * @param id Team ID
//     * @return TeamDTO
//     */
//    @GetMapping("/{id}")
//    @Operation(summary = "Get a specific team by ID", description = "Fetch a team by its ID")
//    public ResponseResult<TeamDTO> getTeamById(@PathVariable Integer id) {
//        try {
//            TeamDTO team = teamService.getTeamById(id);
//            return ResponseResult.success(team);
//        } catch (Exception e) {
//            logger.error("Error retrieving team by ID", e);
//            return ResponseResult.fail("Failed to retrieve team", e);
//        }
//    }
//
//    /**
//     * Get all teams.
//     *
//     * @return List of TeamDTO
//     */
//    @GetMapping
//    @Operation(summary = "Get all teams", description = "Fetch all teams")
//    public ResponseResult<List<TeamDTO>> getAllTeams() {
//        try {
//            List<TeamDTO> teams = teamService.getFullTeamTree();
//            return ResponseResult.success(teams);
//        } catch (Exception e) {
//            logger.error("Error retrieving all teams", e);
//            return ResponseResult.fail("Failed to retrieve teams", e);
//        }
//    }
//
//    /**
//     * Soft delete a team.
//     *
//     * @param id     ID of the team to soft delete
//     * @param updatedBy ID of the user performing the action
//     * @return Void
//     */
//    @PutMapping("/soft-delete/{id}")
//    @Operation(summary = "Soft delete a team", description = "Mark a team as inactive by performing a soft delete")
//    public ResponseResult<Void> softDeleteTeam(@PathVariable Integer id, @RequestParam Integer updatedBy) {
//        try {
//            teamService.softDeleteTeam(id, updatedBy);
//            return ResponseResult.success();
//        } catch (Exception e) {
//            logger.error("Error soft deleting team", e);
//            return ResponseResult.fail("Failed to soft delete team", e);
//        }
//    }
//
//    /**
//     * Check user's current team leadership association, remove the association if invalid
//     *
//     * @param userId ID of the user to check leadership
//     * @return Void
//     */
//    @PostMapping("/leadership/{userId}")
//    @Operation(summary = "Check user's leading team association and remove association if invalid (ascendant team membership missing)")
//    public ResponseResult<Void> removeOrphanLeadership(@PathVariable Integer userId) {
//        try {
//            teamService.removeOrphanLeadership(userId);
//            return ResponseResult.success();
//        } catch (Exception e) {
//            logger.error("Error removing orphan user's team leadership", e);
//            return ResponseResult.fail("Failed to removing orphan user's team leadership", e);
//        }
//    }
//
//    /**
//     * Hard delete a team.
//     *
//     * @param id ID of the team to hard delete
//     * @return Void
//     */
//    @DeleteMapping("/delete/{id}")
//    @Operation(summary = "Hard delete a team", description = "Permanently delete a team")
//    public ResponseResult<Void> hardDeleteTeam(@PathVariable Integer id) {
//        try {
//            teamService.hardDeleteTeam(id);
//            return ResponseResult.success();
//        } catch (Exception e) {
//            logger.error("Error hard deleting team", e);
//            return ResponseResult.fail("Failed to hard delete team", e);
//        }
//    }
//
//    // get the team id by the team lead id
//    @GetMapping("/lead/{id}")
//    @Operation(summary = "Get team by team lead id", description = "Fetch a team by its team lead id")
//    public ResponseResult<TeamDTO> getTeamByTeamLeadId(@PathVariable Integer id) {
//        try {
//            TeamDTO team = teamService.getTeamDTOByTeamLeadId(id);
//            return ResponseResult.success(team);
//        } catch (Exception e) {
//            logger.error("Error retrieving team by team lead id", e);
//            return ResponseResult.fail("Failed to retrieve team", e);
//        }
//    }
//
//    // get the list of current leader ids existing in the Team
//    @GetMapping("/leaders")
//    @Operation(summary = "Get current leaders", description = "Fetch a list of current leaders")
//    public ResponseResult<List<LeaderDTO>> getCurrentLeaders() {
//        try {
//            List<LeaderDTO> leaders = teamService.getCurrentLeaders();
//            return ResponseResult.success(leaders);
//        } catch (Exception e) {
//            logger.error("Error retrieving current leader ids", e);
//            return ResponseResult.fail("Failed to retrieve current leader ids", e);
//        }
//    }
//
//    /**
//     * Get the depth (level) of a team in the hierarchy.
//     * Root team → 1, child of root → 2, etc.
//     *
//     * @param id Team ID
//     * @return depth as Integer
//     */
//    @GetMapping("/depth/{id}")
//    @Operation(
//            summary     = "Get depth of a team",
//            description = "Return the hierarchical depth of the specified team (root level = 1)"
//    )
//    public ResponseResult<Integer> getTeamDepth(@PathVariable Integer id) {
//        try {
//            int depth = teamService.getDepth(id);
//            return ResponseResult.success(depth);
//        } catch (Exception e) {
//            logger.error("Error retrieving depth for team {}", id, e);
//            return ResponseResult.fail("Failed to get team depth", e);
//        }
//    }
//
//
//
//}
