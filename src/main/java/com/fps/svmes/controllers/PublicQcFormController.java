package com.fps.svmes.controllers;

import com.fps.svmes.dto.dtos.qcForm.QcFormTemplateDTO;
import com.fps.svmes.dto.responses.ResponseResult;
import com.fps.svmes.services.QcFormDataService;
import com.fps.svmes.services.QcFormTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Anonymous, unauthenticated access for external form fill-in links (no token,
 * no expiry — just formId). Kept isolated from QcFormTemplateController /
 * QcFormDataController so the "/public/**" whitelist in SecurityConstants
 * cannot accidentally widen to any authenticated endpoint.
 */
@RestController
@Slf4j
@RequestMapping("/public/qc-forms")
@RequiredArgsConstructor
@Tag(name = "Public QC Form API", description = "Unauthenticated form fill-in for external users")
public class PublicQcFormController {

    // Placeholder submitter id for anonymous, external submissions.
    private static final Long EXTERNAL_SUBMITTER_USER_ID = 0L;

    private final QcFormTemplateService qcFormTemplateService;
    private final QcFormDataService qcFormDataService;

    @GetMapping("/{formId}")
    @Operation(summary = "Get a QC form template by ID (public)", description = "Anonymous fetch of a form template for an external fill-in link.")
    public ResponseResult<QcFormTemplateDTO> getPublicTemplate(@PathVariable Long formId) {
        try {
            QcFormTemplateDTO template = qcFormTemplateService.getTemplateById(formId);
            return ResponseResult.success(template);
        } catch (Exception e) {
            log.error("Error retrieving public template with ID: {}", formId, e);
            return ResponseResult.fail("Error retrieving template", e);
        }
    }

    // Submission is disabled for now — an anonymous, unauthenticated POST with no
    // token/expiry/rate-limiting would let anyone flood a form's collection.
    // Re-enable once there's some form of throttling/anti-spam in place; the logic
    // below is otherwise ready to go.
    /*
    @PostMapping("/{formId}/submit")
    @Operation(summary = "Submit a QC form (public)", description = "Anonymous form submission from an external fill-in link.")
    public ResponseResult<Map<String, Object>> submitPublicForm(@PathVariable Long formId, @RequestBody Map<String, Object> formData) {
        try {
            String collectionName = "form_template_" + formId + "_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
            Map<String, Object> result = qcFormDataService.insertFormData(collectionName, EXTERNAL_SUBMITTER_USER_ID, formData, false);
            return ResponseResult.success(result);
        } catch (IllegalArgumentException e) {
            return ResponseResult.fail("Invalid form template or form data", e);
        } catch (Exception e) {
            log.error("Error submitting public form for formId: {}", formId, e);
            return ResponseResult.fail("Error submitting form data", e);
        }
    }
    */
}
