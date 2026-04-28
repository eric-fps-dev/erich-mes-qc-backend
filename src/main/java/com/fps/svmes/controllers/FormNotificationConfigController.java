package com.fps.svmes.controllers;

import com.fps.svmes.dto.dtos.notification.FormNotificationConfigDTO;
import com.fps.svmes.dto.responses.ResponseResult;
import com.fps.svmes.services.FormNotificationConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequestMapping("/form-notification-config")
@RequiredArgsConstructor
@Tag(name = "Form Notification Config API", description = "Per-form responsible person & email notification settings")
public class FormNotificationConfigController {

    private final FormNotificationConfigService service;

    @GetMapping("")
    @Operation(summary = "Get config by form template ID")
    public ResponseResult<FormNotificationConfigDTO> getByFormTemplateId(@RequestParam Long formTemplateId) {
        try {
            FormNotificationConfigDTO dto = service.getByFormTemplateId(formTemplateId);
            return ResponseResult.success(dto);
        } catch (Exception e) {
            log.error("Error fetching notification config for formTemplateId={}", formTemplateId, e);
            return ResponseResult.fail("Error fetching notification config", e);
        }
    }

    @PostMapping("")
    @Operation(summary = "Create or update config (upsert)")
    public ResponseResult<FormNotificationConfigDTO> save(@RequestBody FormNotificationConfigDTO dto) {
        try {
            FormNotificationConfigDTO saved = service.save(dto);
            log.info("Notification config saved for formTemplateId={}", dto.getFormTemplateId());
            return ResponseResult.success(saved);
        } catch (Exception e) {
            log.error("Error saving notification config", e);
            return ResponseResult.fail("Error saving notification config", e);
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete config by ID")
    public ResponseResult<Void> delete(@PathVariable Long id) {
        try {
            service.delete(id);
            log.info("Notification config deleted, id={}", id);
            return ResponseResult.success(null);
        } catch (Exception e) {
            log.error("Error deleting notification config id={}", id, e);
            return ResponseResult.fail("Error deleting notification config", e);
        }
    }
}
