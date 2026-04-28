package com.fps.svmes.services;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fps.svmes.dto.dtos.qcForm.QcFormTemplateDTO;
import com.fps.svmes.dto.dtos.qcForm.QcFormTemplateEditLogDTO;

import java.util.List;
import java.util.Map;

public interface QcFormTemplateService {
    List<QcFormTemplateDTO> getAllActiveTemplates();
    QcFormTemplateDTO getTemplateById(Long id);
    QcFormTemplateDTO createTemplate(QcFormTemplateDTO dto);
    QcFormTemplateDTO updateTemplate(Long id, QcFormTemplateDTO dto);
    void deleteTemplate(Long id);
    void extractControlLimits(JsonNode widgetList, ObjectNode controlLimits);
    void createControlLimitSetting(QcFormTemplateDTO template);
    String getApprovalTypeByFormId(Long formTemplateId);
    String resolveLabelFromTemplateByKey(Long templateId, String fieldKey);
    void extractAndStoreKeyLabelPairs(QcFormTemplateDTO template);
    QcFormTemplateDTO updateTemplateWithNodeSync(Long id, QcFormTemplateDTO dto);
    List<QcFormTemplateEditLogDTO> getEditLog(Long templateId);
    List<java.util.Map<String, Object>> getTemplateFields(Long templateId);

    Map<String, Object> duplicateTemplate(Long templateId, String sourceNodeId, Integer requestedBy);
}