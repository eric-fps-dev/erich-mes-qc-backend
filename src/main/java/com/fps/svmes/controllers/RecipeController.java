package com.fps.svmes.controllers;

import com.fps.svmes.dto.dtos.recipe.ControlLimitSettingDTO;
import com.fps.svmes.dto.responses.ResponseResult;
import com.fps.svmes.services.RecipeService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/recipes")
public class RecipeController {

    @Autowired
    private RecipeService recipeService;

    @GetMapping("/{templateId}")
    @Operation(summary = "Get control limits by QC form template ID")
    public ResponseResult<ControlLimitSettingDTO> getByTemplateId(@PathVariable Long templateId) {
        return ResponseResult.success(recipeService.getByQcFormTemplateId(templateId));
    }

    /**
     * Updates all control limits for the specified QC form template.
     *
     * ⚠️ This is a full overwrite operation. The `control_limits` object in the database
     * will be replaced with the one provided in the request.
     *
     * ✅ You must include all control limits—both updated and unchanged.
     * ❌ Omitting any control limit will result in it being deleted from the database.
     *
     * Example:
     * {
     *   "qc_form_template_id": 206,
     *   "control_limits": {
     *     "alert_test_0": { "upper_control_limit": 90.0, "lower_control_limit": 20.0, "label": "数字警戒0" },
     *     "alert_test_1": { "upper_control_limit": 80.0, "lower_control_limit": 25.0, "label": "数字警戒1" }
     *   }
     * }
     *
     * @param dto Full ControlLimitSettingDTO with all control limits to persist
     * @return Success response if update completes
     */

    @PutMapping("/update")
    @Operation(summary = "Update control limits")
    public ResponseResult<Void> update(@RequestBody ControlLimitSettingDTO dto) {
        recipeService.updateControlLimits(dto);
        return ResponseResult.success();
    }


}