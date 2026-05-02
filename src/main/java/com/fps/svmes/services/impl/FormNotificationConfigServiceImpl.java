package com.fps.svmes.services.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fps.svmes.dto.dtos.alert.ExceededFieldInfoDTO;
import com.fps.svmes.dto.dtos.notification.FormNotificationConfigDTO;
import com.fps.svmes.dto.dtos.notification.FormNotificationRecipientDTO;
import com.fps.svmes.dto.dtos.qcForm.QcFormTemplateDTO;
import com.fps.svmes.models.sql.notification.FormNotificationConfig;
import com.fps.svmes.models.sql.notification.FormNotificationRecipient;
import com.fps.svmes.repositories.jpaRepo.FormNotificationConfigRepository;
import com.fps.svmes.repositories.jpaRepo.FormNotificationRecipientRepository;
import com.fps.svmes.services.EmailService;
import com.fps.svmes.services.FormNotificationConfigService;
import com.fps.svmes.services.QcFormTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FormNotificationConfigServiceImpl implements FormNotificationConfigService {

    private final FormNotificationConfigRepository configRepo;
    private final FormNotificationRecipientRepository recipientRepo;
    private final EmailService emailService;
    private final QcFormTemplateService qcFormTemplateService;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final Set<String> EXCLUDED_KEYS = Set.of(
            "e-signature", "exceeded_info", "approval_info",
            "version_group_id", "version", "created_at", "created_by", "submitter_name"
    );

    private static final Map<String, List<String>> META_KEY_MAP = Map.of(
            "product", List.of("related_products", "product", "product_name"),
            "batch", List.of("related_batches", "batch", "batch_code"),
            "shift", List.of("related_shifts", "belonging_shift", "shift"),
            "team", List.of("related_teams", "belonging_team", "team"),
            "inspector", List.of("related_inspectors", "qc_personnel", "inspector")
    );

    private static final Map<String, String> META_LABELS = Map.of(
            "product", "PRODUCT",
            "batch", "BATCH",
            "shift", "SHIFT",
            "team", "TEAM",
            "inspector", "INSPECTOR"
    );

    // ─── Public API ──────────────────────────────────────────────────────────

    @Override
    public FormNotificationConfigDTO getByFormTemplateId(Long formTemplateId) {
        Optional<FormNotificationConfig> opt = configRepo.findByFormTemplateId(formTemplateId);
        if (opt.isEmpty()) return null;
        FormNotificationConfig config = opt.get();
        List<FormNotificationRecipient> recipients = recipientRepo.findByConfigIdAndStatus(config.getId(), 1);
        return toDTO(config, recipients);
    }

    @Override
    @Transactional
    public FormNotificationConfigDTO save(FormNotificationConfigDTO dto) {
        FormNotificationConfig config = configRepo.findByFormTemplateId(dto.getFormTemplateId())
                .orElseGet(FormNotificationConfig::new);

        config.setFormTemplateId(dto.getFormTemplateId());
        config.setTriggerType(dto.getTriggerType() != null ? dto.getTriggerType() : "always");
        config.setDeliveryMethod(dto.getDeliveryMethod() != null ? dto.getDeliveryMethod() : "email");
        if (config.getId() == null) {
            config.setCreationDetails(null, 1);
        } else {
            config.setUpdateDetails(null, 1);
        }
        config = configRepo.save(config);

        recipientRepo.deleteByConfigId(config.getId());
        final Long configId = config.getId();
        if (dto.getRecipients() != null) {
            List<FormNotificationRecipient> newRecipients = dto.getRecipients().stream()
                    .map(r -> toEntity(r, configId))
                    .collect(Collectors.toList());
            recipientRepo.saveAll(newRecipients);
        }

        List<FormNotificationRecipient> saved = recipientRepo.findByConfigIdAndStatus(configId, 1);
        return toDTO(config, saved);
    }

    @Override
    @Transactional
    public void delete(Long configId) {
        recipientRepo.deleteByConfigId(configId);
        configRepo.deleteById(configId);
    }

    @Override
    public void triggerSubmissionNotification(Long formTemplateId, Long submitterId,
                                              Map<String, Object> formData,
                                              Map<String, ExceededFieldInfoDTO> exceededInfo,
                                              String submissionId) {
        try {
            Optional<FormNotificationConfig> opt = configRepo.findByFormTemplateId(formTemplateId);
            if (opt.isEmpty()) return;
            FormNotificationConfig config = opt.get();

            boolean hasAlerts = hasActualAlerts(exceededInfo);

            if ("on_alert".equals(config.getTriggerType()) && !hasAlerts) {
                log.debug("Skipping notification for formTemplateId={}: on_alert trigger but no exceeded fields", formTemplateId);
                return;
            }

            List<FormNotificationRecipient> recipients = recipientRepo.findByConfigIdAndStatus(config.getId(), 1);
            if (recipients.isEmpty()) return;

            String formName;
            String formTemplateJson = null;
            
            log.debug("Triggering notification for submission={}. FormData keys: {}. TemplateJson exists: {}", 
                    submissionId, formData != null ? formData.keySet() : "null", formTemplateJson != null);
            try {
                QcFormTemplateDTO template = qcFormTemplateService.getTemplateById(formTemplateId);
                formName = template.getName();
                formTemplateJson = template.getFormTemplateJson();
            } catch (Exception e) {
                formName = "Form #" + formTemplateId;
            }

            String subject = hasAlerts
                    ? "[MES QC ⚠] " + formName + " — Submission with exceeded fields"
                    : "[MES QC] " + formName + " — New submission";

            String html = buildEmailHtml(formName, submissionId, submitterId,
                    formData != null ? formData : Map.of(),
                    exceededInfo != null ? exceededInfo : Map.of(),
                    formTemplateJson);

            for (FormNotificationRecipient recipient : recipients) {
                try {
                    emailService.sendHtmlEmail(recipient.getUserEmail(), subject, html);
                    log.info("Notification sent to {} for formTemplateId={} submission={}",
                            recipient.getUserEmail(), formTemplateId, submissionId);
                } catch (Exception e) {
                    log.error("Failed to send notification to {}: {}", recipient.getUserEmail(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error in triggerSubmissionNotification for formTemplateId={}: {}", formTemplateId, e.getMessage(), e);
        }
    }

    // ─── Email HTML builders ──────────────────────────────────────────────────

    private String buildEmailHtml(String formName, String submissionId, Long submitterId,
                                   Map<String, Object> formData,
                                   Map<String, ExceededFieldInfoDTO> exceededInfo,
                                   String formTemplateJson) {
        String timestamp = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss xxx"));
        boolean hasAlerts = hasActualAlerts(exceededInfo);

        return "<!DOCTYPE html><html><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width'></head>"
                + "<body style='margin:0;padding:24px 16px;background:#f4f7f8;font-family:-apple-system,\"Helvetica Neue\",Arial,sans-serif;'>"
                + "<table width='640' cellpadding='0' cellspacing='0' align='center' "
                + "style='width:640px;max-width:100%;margin:0 auto;background:#fff;border-radius:6px;"
                + "overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.08);'>"
                + buildHeader(formName, submissionId, timestamp, hasAlerts)
                + buildContent(formData, exceededInfo, formTemplateJson)
                + buildFooter(formData, submitterId, timestamp)
                + "</table>"
                + "</body></html>";
    }

    private String buildHeader(String formName, String submissionId, String timestamp, boolean hasAlerts) {
        String alertBadge = hasAlerts
                ? "<span style='display:inline-block;background:#fef2f2;color:#dc2626;padding:2px 8px;"
                  + "border-radius:10px;font-size:11px;font-weight:600;margin-left:8px;vertical-align:middle;border:1px solid #fecaca;'>⚠ Alert</span>"
                : "";

        return "<tr><td style='background:#ffffff;padding:24px 32px;border-bottom:1px solid #f0f4f6;'>"
                + "<table width='100%' cellpadding='0' cellspacing='0'><tr>"
                + "<td style='vertical-align:middle;'>"
                + "<table cellpadding='0' cellspacing='0'><tr>"
                + "<td style='vertical-align:middle;width:52px;height:52px;background:#0085a4;"
                + "border-radius:50%;text-align:center;font-size:18px;font-weight:800;color:#fff;"
                + "letter-spacing:1px;'>FPS</td>"
                + "<td style='vertical-align:middle;padding-left:14px;'>"
                + "<div style='font-size:18px;font-weight:700;color:#2c3e50;'>" + escHtml(formName) + alertBadge + "</div>"
                + "<div style='font-size:11px;color:#7f8c8d;margin-top:4px;text-transform:uppercase;letter-spacing:0.5px;'>Quality Control Record</div>"
                + "</td></tr></table>"
                + "</td>"
                + "<td style='text-align:right;vertical-align:middle;'>"
                + "<div style='font-size:10px;color:#95a5a6;letter-spacing:0.5px;margin-bottom:5px;font-weight:700;'>RECORD NO.</div>"
                + "<div style='display:inline-block;font-size:12px;font-weight:600;color:#0085a4;"
                + "background:#e8f5f8;padding:2px 10px;border-radius:10px;'>" + escHtml(submissionId) + "</div>"
                + "<div style='font-size:11px;color:#95a5a6;margin-top:6px;'>" + timestamp + "</div>"
                + "</td>"
                + "</tr></table>"
                + "</td></tr>";
    }

    private String buildContent(Map<String, Object> formData, Map<String, ExceededFieldInfoDTO> exceededInfo,
                                 String formTemplateJson) {
        StringBuilder rows = new StringBuilder();
        if (formTemplateJson != null && !formTemplateJson.isBlank()) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> template = OBJECT_MAPPER.readValue(formTemplateJson, Map.class);
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> widgetList = (List<Map<String, Object>>) template.get("widgetList");
                traverseWidgets(widgetList, formData, exceededInfo, rows, new boolean[]{true});
            } catch (Exception e) {
                log.warn("Failed to parse formTemplateJson for email rendering, using flat list: {}", e.getMessage());
                buildFlatFieldRows(formData, exceededInfo, rows);
            }
        } else {
            buildFlatFieldRows(formData, exceededInfo, rows);
        }

        // Add Form Basic Fields section (before signature)
        rows.append(buildBasicFields(formData));

        // Signature section
        Object signature = findValueRecursive(formData, "e-signature");
        if (signature instanceof String sigStr && sigStr.startsWith("data:image")) {
            rows.append("<tr><td colspan='2' style='padding-top:24px;'>")
                .append("<div style='font-size:11px;color:#95a5a6;margin-bottom:8px;text-transform:uppercase;letter-spacing:0.5px;'>E-SIGNATURE</div>")
                .append("<img src='").append(sigStr).append("' style='max-width:220px;height:auto;border:1px solid #dde8ed;border-radius:4px;' alt='signature'/>")
                .append("</td></tr>");
        }

        return "<tr><td style='padding:20px 32px;'>"
                + "<table width='100%' cellpadding='0' cellspacing='0' style='border-collapse:collapse;'>"
                + rows
                + "</table></td></tr>";
    }

    private String buildBasicFields(Map<String, Object> formData) {
        StringBuilder sb = new StringBuilder();
        boolean hasData = false;
        
        StringBuilder fieldRows = new StringBuilder();
        for (String groupKey : List.of("product", "batch", "shift", "team", "inspector")) {
            List<String> possibleKeys = META_KEY_MAP.get(groupKey);
            Object val = null;
            for (String key : possibleKeys) {
                val = findValueRecursive(formData, key);
                if (val != null && !val.toString().isBlank() && !"-".equals(val.toString())) {
                    break; 
                }
            }
            
            if (val == null || val.toString().isBlank() || "-".equals(val.toString())) continue;
            
            String label = META_LABELS.getOrDefault(groupKey, groupKey.toUpperCase());
            hasData = true;
            
            fieldRows.append("<tr>")
              .append("<td style='width:50%;padding:8px 12px;border:1px solid #dde8ed;font-weight:600;color:#5a6a7a;font-size:12px;'>")
              .append(escHtml(label))
              .append("</td>")
              .append("<td style='padding:8px 12px;border:1px solid #dde8ed;font-size:13px;color:#2c3e50;'>")
              .append(escHtml(val.toString()))
              .append("</td></tr>");
        }
        
        if (hasData) {
            sb.append("<tr><td colspan='2' style='height:32px; border:none;'></td></tr>");
            sb.append("<tr><td colspan='2' style='padding:8px 0; border:none;'>")
              .append("<div style='font-size:11px; color:#95a5a6; font-weight:700; text-transform:uppercase; letter-spacing:0.5px;'>Contextual Info</div>")
              .append("</td></tr>");
            sb.append(fieldRows);
        }
        
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private void traverseWidgets(List<Map<String, Object>> widgetList, Map<String, Object> formData,
                                  Map<String, ExceededFieldInfoDTO> exceededInfo, StringBuilder sb,
                                  boolean[] firstBlock) {
        if (widgetList == null) return;
        for (Map<String, Object> widget : widgetList) {
            Map<String, Object> options = (Map<String, Object>) widget.getOrDefault("options", Map.of());
            String widgetType = String.valueOf(widget.getOrDefault("type", widget.getOrDefault("icon", "")));
            String name = String.valueOf(options.getOrDefault("name", ""));
            boolean formItemFlag = Boolean.TRUE.equals(widget.get("formItemFlag"));

            if ("divider".equals(widgetType) && !formItemFlag) {
                String text = (String) options.get("label");
                if (text != null && !text.isBlank()) {
                    if (!firstBlock[0]) {
                        sb.append("<tr><td colspan='2' style='height:20px;padding:0;border:none;'></td></tr>");
                    }
                    firstBlock[0] = false;
                    sb.append("<tr><td colspan='2' style='background:#0085a4;color:#fff;padding:8px 14px;"
                            + "font-size:13px;font-weight:700;letter-spacing:0.5px;border:1px solid #006f8a;'>")
                      .append(escHtml(text)).append("</td></tr>");
                }
            } else if (name.startsWith("static-text") && !formItemFlag) {
                String textContent = (String) options.get("textContent");
                if (textContent != null) {
                    String stripped = textContent.replaceAll("<[^>]+>", "").trim();
                    if (!stripped.isBlank()) {
                        sb.append("<tr><td colspan='2' style='background:#e8f5f8;padding:6px 14px;"
                                + "font-size:11px;font-weight:600;color:#4a8a99;font-style:italic;"
                                + "border:1px solid #dde8ed;border-left:3px solid #b2dce6;'>")
                          .append(escHtml(stripped)).append("</td></tr>");
                    }
                }
            } else if (formItemFlag) {
                String label = (String) options.get("label");
                String widgetName = (String) options.get("name");
                
                Object value = null;
                if (widgetName != null) value = findValueRecursive(formData, widgetName);
                if (value == null && label != null) value = findValueRecursive(formData, label);

                if (value != null && !"e-signature".equalsIgnoreCase(label) && !"e-signature".equalsIgnoreCase(widgetName)) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> optionItems = (List<Map<String, Object>>) options.get("optionItems");
                    if (optionItems == null) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> altOptions = (List<Map<String, Object>>) options.get("options");
                        optionItems = altOptions;
                    }
                    appendFieldRow(sb, label, widgetName, value, exceededInfo, optionItems);
                }
            }

            // Recurse into container widgets
            List<Map<String, Object>> cols = (List<Map<String, Object>>) widget.get("cols");
            if (cols != null) {
                for (Map<String, Object> col : cols)
                    traverseWidgets((List<Map<String, Object>>) col.get("widgetList"), formData, exceededInfo, sb, firstBlock);
            }
            List<Map<String, Object>> rowsList = (List<Map<String, Object>>) widget.get("rows");
            if (rowsList != null) {
                for (Map<String, Object> row : rowsList) {
                    List<Map<String, Object>> rowCols = (List<Map<String, Object>>) row.get("cols");
                    if (rowCols != null)
                        for (Map<String, Object> col : rowCols)
                            traverseWidgets((List<Map<String, Object>>) col.get("widgetList"), formData, exceededInfo, sb, firstBlock);
                }
            }
            List<Map<String, Object>> tabs = (List<Map<String, Object>>) widget.get("tabs");
            if (tabs != null) {
                for (Map<String, Object> tab : tabs)
                    traverseWidgets((List<Map<String, Object>>) tab.get("widgetList"), formData, exceededInfo, sb, firstBlock);
            }
            // Generic nested widgetList (only if no other container matched)
            if (cols == null && rowsList == null && tabs == null) {
                List<Map<String, Object>> nested = (List<Map<String, Object>>) widget.get("widgetList");
                if (nested != null) traverseWidgets(nested, formData, exceededInfo, sb, firstBlock);
            }
        }
    }

    private void buildFlatFieldRows(Map<String, Object> formData, Map<String, ExceededFieldInfoDTO> exceededInfo,
                                     StringBuilder sb) {
        Set<String> skip = new HashSet<>(EXCLUDED_KEYS);
        META_KEY_MAP.values().forEach(skip::addAll);
        for (Map.Entry<String, Object> entry : formData.entrySet()) {
            if (skip.contains(entry.getKey())) continue;
            Object val = entry.getValue();
            if (val == null) continue;
            appendFieldRow(sb, entry.getKey(), entry.getKey(), val, exceededInfo, null);
        }
    }

    private void appendFieldRow(StringBuilder sb, String label, String widgetName, Object value,
                                 Map<String, ExceededFieldInfoDTO> exceededInfo,
                                 List<Map<String, Object>> optionItems) {
        ExceededFieldInfoDTO info = null;
        if (widgetName != null) info = exceededInfo.get(widgetName);
        if (info == null && label != null) info = exceededInfo.get(label);

        String alertColor = getAlertColor(info);
        boolean isAlert = info != null && info.getResult() != null;
        String displayVal = formatValue(value, optionItems);
        String rangeText = getValidRange(info);

        sb.append("<tr>")
          .append("<td style='width:50%;padding:8px 12px;border:1px solid #dde8ed;"
                  + "font-weight:600;color:#5a6a7a;font-size:12px;'>")
          .append(escHtml(label != null ? label : widgetName))
          .append("</td>")
          .append("<td style='padding:8px 12px;border:1px solid #dde8ed;font-size:13px;color:")
          .append(alertColor).append(";")
          .append(isAlert ? "font-weight:bold;" : "")
          .append("'>")
          .append(displayVal);

        if (rangeText != null) {
            sb.append("<div style='font-size:10px;color:#95a5a6;margin-top:3px;font-weight:normal;'>")
              .append(escHtml(rangeText)).append("</div>");
        }

        sb.append("</td></tr>");
    }

    private String buildFooter(Map<String, Object> formData, Long submitterId, String timestamp) {
        // Try to find a human-readable name in common fields, prioritizing submitter-specific ones
        String[] potentialNameKeys = {
            "submitter_name", "submitter", "created_by_name", "operator",
            "related_inspectors", "qc_personnel", "inspector"
        };
        
        Object submitterName = null;
        for (String key : potentialNameKeys) {
            submitterName = findValueRecursive(formData, key);
            if (submitterName != null && !submitterName.toString().isBlank() && !"-".equals(submitterName.toString())) {
                break;
            }
        }

        String nameStr = (submitterName != null) ? submitterName.toString() : "User #" + submitterId;

        return "<tr><td style='border-top:2px solid #e8f5f8;padding:14px 32px 24px;background:#fafcfc;'>"
                + "<span style='font-size:12px;color:#7f8c8d;'>Submitter: <strong>" + escHtml(nameStr) + "</strong></span>"
                + "<span style='color:#bdc3c7;margin:0 8px;'>·</span>"
                + "<span style='font-size:12px;color:#7f8c8d;'>Submitted at: " + timestamp + "</span>"
                + "</td></tr>";
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Object findValueRecursive(Map<String, Object> data, String key) {
        if (data == null || key == null) return null;
        if (data.containsKey(key)) return data.get(key);
        for (Object val : data.values()) {
            if (val instanceof Map<?, ?> nested) {
                @SuppressWarnings("unchecked")
                Object found = findValueRecursive((Map<String, Object>) nested, key);
                if (found != null) return found;
            }
        }
        return null;
    }

    private String getAlertColor(ExceededFieldInfoDTO info) {
        if (info == null) return "#2c3e50";
        if (info.getResult() == null) return "#27ae60";
        return switch (info.getResult()) {
            case "high" -> "#e74c3c";
            case "low" -> "#2980b9";
            case "invalid" -> "#e67e22";
            default -> "#2c3e50";
        };
    }

    private String getValidRange(ExceededFieldInfoDTO info) {
        if (info == null) return null;
        if ("number".equals(info.getType())) {
            String min = info.getLowerLimit() != null ? info.getLowerLimit().toPlainString() : "-";
            String max = info.getUpperLimit() != null ? info.getUpperLimit().toPlainString() : "-";
            return "Valid range: " + min + " ~ " + max;
        }
        if ("options".equals(info.getType())) {
            List<String> labels = info.getValidOptionLabels();
            if (labels != null && !labels.isEmpty()) return "Valid options: " + String.join(", ", labels);
        }
        return null;
    }

    private boolean hasActualAlerts(Map<String, ExceededFieldInfoDTO> exceededInfo) {
        if (exceededInfo == null || exceededInfo.isEmpty()) {
            return false;
        }
        return exceededInfo.values().stream()
                .filter(Objects::nonNull)
                .map(ExceededFieldInfoDTO::getResult)
                .anyMatch(result -> result != null && !result.isBlank());
    }

    private static final Set<String> IMAGE_EXTS = Set.of(
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "tiff", "tif", "heic", "heif"
    );

    private String formatValue(Object val, List<Map<String, Object>> optionItems) {
        if (val == null) return "-";

        if (isUrlValue(val)) {
            if (val instanceof String s) return formatUrlValue(s);
            if (val instanceof List<?> list) {
                return list.stream()
                        .map(item -> formatUrlValue(item.toString()))
                        .collect(Collectors.joining(" "));
            }
        }

        if (optionItems != null && !optionItems.isEmpty()) {
            if (val instanceof List<?> list) {
                return escHtml(list.stream().map(o -> getOptionLabel(o, optionItems)).collect(Collectors.joining(", ")));
            }
            return escHtml(getOptionLabel(val, optionItems));
        }

        if (val instanceof List<?> list) return escHtml(list.stream().map(Object::toString).collect(Collectors.joining(", ")));
        if (val instanceof Boolean b) return b ? "Yes" : "No";
        String s = val.toString().trim();
        return s.isEmpty() ? "-" : escHtml(s);
    }

    private String formatUrlValue(String url) {
        if (url == null || url.isBlank()) return "-";
        boolean isImg = isImageUrl(url);
        String icon = isImg ? "🖼️ " : "📎 ";
        String filename = getFilename(url);
        String bgColor = isImg ? "#f0f9ff" : "#f5f7fa";
        String borderColor = isImg ? "#bae6fd" : "#dde8ed";
        String textColor = isImg ? "#0369a1" : "#0085a4";

        return "<a href='" + url + "' target='_blank' style='color:" + textColor + ";text-decoration:none;font-size:12px;"
                + "display:inline-block;margin:2px;padding:4px 10px;background:" + bgColor + ";border:1px solid " + borderColor + ";border-radius:4px;"
                + "font-weight:500;box-shadow:0 1px 2px rgba(0,0,0,0.05);'>"
                + icon + escHtml(filename) + "</a>";
    }

    private boolean isImageUrl(String url) {
        if (url == null) return false;
        String lower = url.toLowerCase();
        return IMAGE_EXTS.stream().anyMatch(ext -> lower.contains("." + ext));
    }

    private String getFilename(String url) {
        if (url == null || url.isBlank()) return "file";
        int lastSlash = url.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < url.length() - 1) {
            return url.substring(lastSlash + 1);
        }
        return "file";
    }


    private String getOptionLabel(Object val, List<Map<String, Object>> optionItems) {
        if (val == null || optionItems == null) return "-";
        String s = val.toString().trim();
        for (Map<String, Object> opt : optionItems) {
            Object optVal = opt.get("value");
            if (optVal != null && optVal.toString().trim().equals(s)) {
                Object label = opt.get("label");
                return label != null ? label.toString() : s;
            }
        }
        return s;
    }

    private boolean isUrlValue(Object val) {
        if (val instanceof String s) return s.startsWith("http") || s.contains("/files/");
        if (val instanceof List<?> list) return !list.isEmpty() && list.stream().allMatch(
                item -> item instanceof String str && (str.startsWith("http") || str.contains("/files/")));
        return false;
    }

    private String escHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    // ─── Mapping helpers ──────────────────────────────────────────────────────

    private FormNotificationConfigDTO toDTO(FormNotificationConfig config, List<FormNotificationRecipient> recipients) {
        FormNotificationConfigDTO dto = new FormNotificationConfigDTO();
        dto.setId(config.getId());
        dto.setFormTemplateId(config.getFormTemplateId());
        dto.setTriggerType(config.getTriggerType());
        dto.setDeliveryMethod(config.getDeliveryMethod());
        dto.setRecipients(recipients.stream().map(r -> {
            FormNotificationRecipientDTO rd = new FormNotificationRecipientDTO();
            rd.setUserId(r.getUserId());
            rd.setUserEmail(r.getUserEmail());
            rd.setUserName(r.getUserName());
            return rd;
        }).collect(Collectors.toList()));
        return dto;
    }

    private FormNotificationRecipient toEntity(FormNotificationRecipientDTO dto, Long configId) {
        FormNotificationRecipient entity = new FormNotificationRecipient();
        entity.setConfigId(configId);
        entity.setUserId(dto.getUserId());
        entity.setUserEmail(dto.getUserEmail());
        entity.setUserName(dto.getUserName());
        return entity;
    }
}
