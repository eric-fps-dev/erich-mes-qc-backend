package com.fps.svmes.services;

import com.fps.svmes.dto.dtos.recipe.ControlLimitSettingDTO;

public interface RecipeService {
    ControlLimitSettingDTO getByQcFormTemplateId(Long templateId);
    void updateControlLimits(ControlLimitSettingDTO dto);
}