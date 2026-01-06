//package com.fps.svmes.controllers;
//
//import com.fps.svmes.dto.responses.ResponseResult;
//import com.fps.svmes.models.nosql.FormNode;
//import com.fps.svmes.services.TeamFormService;
//import io.swagger.v3.oas.annotations.Operation;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.web.bind.annotation.*;
//import io.swagger.v3.oas.annotations.tags.Tag;
//
//import java.util.List;
//
//@RestController
//@Slf4j
//@RequestMapping("/team-forms")
//@RequiredArgsConstructor
//@Tag(name = "Team-Form API", description = "API for managing team-form relationships")
//public class TeamFormController {
//    private final TeamFormService teamFormService;
//    public static final Logger logger = LoggerFactory.getLogger(TeamFormController.class);
//
//    @PostMapping("/teams/{teamId}/forms")
//    @Operation(summary = "Assign multiple forms to a team", description = "Assign multiple forms to a single team")
//    public ResponseResult<String> assignFormsToTeam(@PathVariable Integer teamId, @RequestBody List<String> formIds) {
//        try {
//            for (String formId: formIds) {
//                teamFormService.assignFormToTeam(teamId, formId);
//            }
//            logger.info("Forms {} assigned to team {}", formIds, teamId);
//            return ResponseResult.success(null);
//        } catch (Exception e) {
//            logger.error("Error assigning forms {} to team {}", formIds, teamId, e);
//            return ResponseResult.fail("Error assigning forms to team", e);
//        }
//    }
//
//    @DeleteMapping("/teams/{teamId}/forms/")
//    @Operation(summary = "Remove a form from a team", description = "Unassign a specific form from a specific team")
//    public ResponseResult<String> removeFormFromTeam(@PathVariable Integer teamId, @RequestBody List<String> formIds) {
//        try {
//            for (String formId: formIds) {
//                teamFormService.removeFormFromTeam(teamId, formId);
//                logger.info("Form {} removed from team {}", formId, teamId);
//            }
//            return ResponseResult.success(null);
//        } catch (Exception e) {
//            logger.error("Error removing forms {} to team {}", formIds, teamId, e);
//            return ResponseResult.fail("Error removing form from team", e);
//        }
//    }
//
//    @DeleteMapping("/teams/{teamId}/forms")
//    @Operation(summary = "Remove all forms from a team", description = "Unassign all forms from a specific team")
//    public ResponseResult<Void> removeAllFormsFromTeam(@PathVariable Integer teamId) {
//        try {
//            teamFormService.removeAllFormsFromTeam(teamId);
//            logger.info("All forms removed from team {}", teamId);
//            return ResponseResult.success(null);
//        } catch (Exception e) {
//            logger.error("Error removing all forms from team {}", teamId, e);
//            return ResponseResult.fail("Error removing all forms from team", e);
//        }
//    }
//
//    @GetMapping("/teams/{teamId}/forms")
//    @Operation(summary = "Get forms assigned to team", description = "Retrieve all form IDs assigned to a specific team")
//    public ResponseResult<List<String>> getFormIdsForTeam(@PathVariable Integer teamId) {
//        try {
//            List<String> formIds = teamFormService.getFormIdsByTeam(teamId);
//            logger.info("Forms for team {} retrieved: {}", teamId, formIds);
//            return ResponseResult.success(formIds);
//        } catch (Exception e) {
//            logger.error("Error retrieving forms for team {}", teamId, e);
//            return ResponseResult.fail("Error retrieving forms for team", e);
//        }
//    }
//
//    @GetMapping("/teams/{teamId}/form-tree")
//    @Operation(summary = "Get filtered form tree by team", description = "Returns only the part of the form tree associated with the given team")
//    public ResponseResult<List<FormNode>> getFormTreeByTeam(@PathVariable Integer teamId) {
//        try {
//            List<FormNode> tree = teamFormService.getFormTreeByTeamId(teamId);
//            logger.info("Filtered form tree for team {} retrieved successfully", teamId);
//            return ResponseResult.success(tree);
//        } catch (Exception e) {
//            logger.error("Error retrieving form tree for team {}", teamId, e);
//            return ResponseResult.fail("Error retrieving filtered form tree", e);
//        }
//    }
//}