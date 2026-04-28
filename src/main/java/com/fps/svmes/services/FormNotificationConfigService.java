package com.fps.svmes.services;

import com.fps.svmes.dto.dtos.alert.ExceededFieldInfoDTO;
import com.fps.svmes.dto.dtos.notification.FormNotificationConfigDTO;

import java.util.Map;

public interface FormNotificationConfigService {

    FormNotificationConfigDTO getByFormTemplateId(Long formTemplateId);

    FormNotificationConfigDTO save(FormNotificationConfigDTO dto);

    void delete(Long configId);

    void triggerSubmissionNotification(Long formTemplateId, Long submitterId,
                                       Map<String, Object> formData,
                                       Map<String, ExceededFieldInfoDTO> exceededInfo,
                                       String submissionId);
}
