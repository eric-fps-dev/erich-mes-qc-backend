package com.fps.svmes.services;

import com.fps.svmes.dto.dtos.alert.ExceededFieldInfoDTO;

import java.util.Map;

public interface ControlLimitEvaluationService {
    void evaluateAndTriggerAlerts(Long templateId, Long userId, Map<String, Object> formData, String submissionId);
    Map<String, ExceededFieldInfoDTO> evaluateExceededInfo(Long templateId, Map<String, Object> formData);
}