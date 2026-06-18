package com.fps.svmes.controllers;

import com.fps.svmes.dto.requests.TeamFormAssociationRequest;
import com.fps.svmes.dto.responses.ResponseResult;
import com.fps.svmes.dto.responses.TeamFormAssociationResponse;
import com.fps.svmes.models.nosql.FormNode;
import com.fps.svmes.services.TeamFormService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/team-forms")
@RequiredArgsConstructor
@Tag(name = "Team-Form API", description = "API for managing team-form relationships")
public class TeamFormController {

    private final TeamFormService teamFormService;

    @Deprecated
    @PostMapping("/{teamId}/forms")
    @Operation(summary = "Assign multiple forms to a team", description = "Assign multiple forms to a single team")
    public ResponseResult<String> assignFormsToTeam(
            @PathVariable Integer teamId,
            @RequestBody List<String> formIds
    ) {
        try {
            teamFormService.assignFormsToTeam(teamId, formIds);
            log.info("Forms {} assigned to team {}", formIds, teamId);
            return ResponseResult.success(null);
        } catch (Exception e) {
            log.error("Error assigning forms {} to team {}", formIds, teamId, e);
            return ResponseResult.fail("Error assigning forms to team", e);
        }
    }

    @Deprecated
    @DeleteMapping("/{teamId}/forms")
    @Operation(summary = "Remove a form from a team", description = "Unassign a specific form from a specific team")
    public ResponseResult<String> removeFormFromTeam(
            @PathVariable Integer teamId,
            @RequestBody List<String> formIds
    ) {
        try {
            teamFormService.removeFormsFromTeam(teamId, formIds);
            log.info("Form {} removed from team {}", formIds, teamId);
            return ResponseResult.success(null);
        } catch (Exception e) {
            log.error("Error removing forms {} from team {}", formIds, teamId, e);
            return ResponseResult.fail("Error removing form from team", e);
        }
    }

    @Deprecated
    @DeleteMapping("/{teamId}/all-forms")
    @Operation(summary = "Remove all forms from a team", description = "Unassign all forms from a specific team")
    public ResponseResult<Void> removeAllFormsFromTeam(
            @PathVariable Integer teamId
    ) {
        try {
            teamFormService.removeAllFormsFromTeam(teamId);
            log.info("All forms removed from team {}", teamId);
            return ResponseResult.success(null);
        } catch (Exception e) {
            log.error("Error removing all forms from team {}", teamId, e);
            return ResponseResult.fail("Error removing all forms from team", e);
        }
    }

    @Deprecated
    @GetMapping("/{teamId}/forms")
    @Operation(summary = "Get forms assigned to team", description = "Retrieve all form IDs assigned to a specific team")
    public ResponseResult<List<String>> getFormIdsForTeam(@PathVariable Integer teamId) {
        try {
            List<String> formIds = teamFormService.getFormIdsByTeam(teamId);
            log.info("Forms for team {} retrieved: {}", teamId, formIds);
            return ResponseResult.success(formIds);
        } catch (Exception e) {
            log.error("Error retrieving forms for team {}", teamId, e);
            return ResponseResult.fail("Error retrieving forms for team", e);
        }
    }

    @GetMapping("/{teamId}/form-associations")
    @Operation(summary = "Get team QC form association", description = "Retrieve the saved QC form association mode and selected node IDs for a team")
    public ResponseResult<TeamFormAssociationResponse> getTeamFormAssociation(@PathVariable Integer teamId) {
        try {
            return ResponseResult.success(teamFormService.getTeamFormAssociation(teamId));
        } catch (EntityNotFoundException e) {
            log.error("Team {} not found when retrieving form association", teamId, e);
            return ResponseResult.failNotFound("Team not found", e);
        } catch (Exception e) {
            log.error("Error retrieving form association for team {}", teamId, e);
            return ResponseResult.fail("Error retrieving team form association", e);
        }
    }

    @PutMapping("/{teamId}/form-associations")
    @Operation(summary = "Replace team QC form association", description = "Replace the saved QC form association mode and selected node IDs for a team")
    public ResponseResult<Void> replaceTeamFormAssociation(
            @PathVariable Integer teamId,
            @RequestBody TeamFormAssociationRequest request
    ) {
        try {
            teamFormService.replaceTeamFormAssociation(teamId, request);
            log.info("Form association replaced for team {}", teamId);
            return ResponseResult.success(null);
        } catch (EntityNotFoundException e) {
            log.error("Team {} not found when replacing form association", teamId, e);
            return ResponseResult.failNotFound("Team not found", e);
        } catch (IllegalArgumentException e) {
            log.error("Invalid form association request for team {}", teamId, e);
            return ResponseResult.failBadRequest(e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error replacing form association for team {}", teamId, e);
            return ResponseResult.fail("Error replacing team form association", e);
        }
    }

    @DeleteMapping("/{teamId}/form-associations")
    @Operation(summary = "Clear team QC form association", description = "Clear the saved QC form association for a team")
    public ResponseResult<Void> clearTeamFormAssociation(@PathVariable Integer teamId) {
        try {
            teamFormService.clearTeamFormAssociation(teamId);
            log.info("Form association cleared for team {}", teamId);
            return ResponseResult.success(null);
        } catch (EntityNotFoundException e) {
            log.error("Team {} not found when clearing form association", teamId, e);
            return ResponseResult.failNotFound("Team not found", e);
        } catch (Exception e) {
            log.error("Error clearing form association for team {}", teamId, e);
            return ResponseResult.fail("Error clearing team form association", e);
        }
    }

    @GetMapping("/{teamId}/form-tree")
    @Operation(summary = "Get filtered form tree by team", description = "Returns only the part of the form tree associated with the given team")
    public ResponseResult<List<FormNode>> getFormTreeByTeam(@PathVariable Integer teamId) {
        try {
            List<FormNode> tree = teamFormService.getFormTreeByTeamId(teamId);
            log.info("Filtered form tree for team {} retrieved successfully", teamId);
            return ResponseResult.success(tree);
        } catch (EntityNotFoundException e) {
            log.error("Team {} not found when retrieving form tree", teamId, e);
            return ResponseResult.failNotFound("Team not found", e);
        } catch (Exception e) {
            log.error("Error retrieving form tree for team {}", teamId, e);
            return ResponseResult.fail("Error retrieving filtered form tree", e);
        }
    }
}
