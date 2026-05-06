package com.fps.svmes.services.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fps.shared.entity.primary.team.Team;
import com.fps.svmes.dto.requests.QcSummaryEmailExportRequest;
import com.fps.svmes.dto.responses.QcSummaryEmailExportResponse;
import com.fps.svmes.models.sql.production.SuggestedBatch;
import com.fps.svmes.models.sql.production.SuggestedProduct;
import com.fps.svmes.models.sql.qcForm.QcFormTemplate;
import com.fps.svmes.repositories.jpaRepo.production.SuggestedBatchRepository;
import com.fps.svmes.repositories.jpaRepo.production.SuggestedProductRepository;
import com.fps.svmes.repositories.jpaRepo.qcForm.QcFormTemplateRepository;
import com.fps.svmes.repositories.jpaRepo.user.TeamRepository;
import com.fps.svmes.services.EmailService;
import com.fps.svmes.services.QcSummaryEmailExportService;
import com.fps.svmes.services.QcTaskSubmissionLogsService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class QcSummaryEmailExportServiceImpl implements QcSummaryEmailExportService {
    private static final DateTimeFormatter UTC_DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'");

    private final RestTemplate restTemplate;
    private final EmailService emailService;
    private final QcTaskSubmissionLogsService qcTaskSubmissionLogsService;
    private final ObjectMapper objectMapper;
    private final TeamRepository teamRepository;
    private final SuggestedProductRepository suggestedProductRepository;
    private final SuggestedBatchRepository suggestedBatchRepository;
    private final QcFormTemplateRepository qcFormTemplateRepository;

    @Value("${qc.snapshot.api.url}")
    private String qcSnapshotApiUrl;

    @Value("${spring.mail.username:noreply@example.com}")
    private String fromEmail;

    @Override
    public QcSummaryEmailExportResponse exportAndSendEmail(QcSummaryEmailExportRequest request, String acceptLanguage) {
        LinkedHashMap<String, byte[]> attachments = new LinkedHashMap<>();
        List<Map<String, Object>> documents = List.of();
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(ZonedDateTime.now());
        boolean english = isEnglish(acceptLanguage);

        if (request.getFormats().isPdf() || request.getFormats().isExcel()) {
            documents = fetchDocumentList(request);
        }

        if (request.getFormats().isPdf()) {
            attachments.put("qc_summary_pdf_records_" + timestamp + ".zip",
                    zipDocuments(documents, document -> qcTaskSubmissionLogsService.exportDocumentToPdf(toDocument(document)), "pdf"));
        }

        if (request.getFormats().isExcel()) {
            attachments.put("qc_summary_excel_records_" + timestamp + ".zip",
                    zipDocuments(documents, document -> qcTaskSubmissionLogsService.exportDocumentToExcel(toDocument(document)), "xlsx"));
        }

        if (request.getFormats().isAi()) {
            attachments.put("qc_summary_ai_report_" + timestamp + ".pdf", fetchAiPdf(request, acceptLanguage));
        }

        String subject = english
                ? String.format("QC Summary Export %s to %s", request.getStartDate(), request.getEndDate())
                : String.format("QC汇总导出 %s 至 %s", request.getStartDate(), request.getEndDate());
        String body = buildEmailBody(request, attachments.keySet().stream().toList(), english);

        emailService.sendHtmlEmailWithAttachments(
                StringUtils.hasText(fromEmail) ? fromEmail : "noreply@example.com",
                request.getRecipients(),
                subject,
                body,
                attachments
        );

        return new QcSummaryEmailExportResponse(
                english ? "QC summary export email sent successfully." : "QC汇总导出邮件发送成功。",
                request.getRecipients().size(),
                new ArrayList<>(attachments.keySet())
        );
    }

    private List<Map<String, Object>> fetchDocumentList(QcSummaryEmailExportRequest request) {
        URI uri = UriComponentsBuilder.fromHttpUrl(qcSnapshotApiUrl)
                .path("/summary/document-list")
                .queryParam("start_date", request.getStartDate())
                .queryParam("end_date", request.getEndDate())
                .queryParamIfPresent("team_id", Optional.ofNullable(request.getTeamId()))
                .queryParamIfPresent("shift_id", Optional.ofNullable(request.getShiftId()))
                .queryParamIfPresent("product_id", Optional.ofNullable(request.getProductId()))
                .queryParamIfPresent("batch_id", Optional.ofNullable(request.getBatchId()))
                .queryParamIfPresent("form_template_id", Optional.ofNullable(request.getFormTemplateId()))
                .build(true)
                .toUri();

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    HttpEntity.EMPTY,
                    new ParameterizedTypeReference<>() {
                    }
            );
            Map<String, Object> body = response.getBody();
            if (body == null || body.get("data") == null) {
                throw new IllegalStateException("Snapshot service returned an empty document list response.");
            }
            return objectMapper.convertValue(body.get("data"), new TypeReference<List<Map<String, Object>>>() {
            });
        } catch (RestClientException e) {
            throw new RuntimeException("Failed to retrieve document list from snapshot service.", e);
        }
    }

    private byte[] fetchAiPdf(QcSummaryEmailExportRequest request, String acceptLanguage) {
        URI uri = UriComponentsBuilder.fromHttpUrl(qcSnapshotApiUrl)
                .path("/summary/export-pdf-report")
                .build(true)
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (StringUtils.hasText(acceptLanguage)) {
            headers.set(HttpHeaders.ACCEPT_LANGUAGE, acceptLanguage);
        }

        AiReportPayload payload = new AiReportPayload(
                request.getStartDate(),
                request.getEndDate(),
                request.getTeamId(),
                request.getShiftId(),
                request.getProductId(),
                request.getBatchId(),
                request.getFormTemplateId(),
                StringUtils.hasText(request.getTimezone()) ? request.getTimezone() : "UTC",
                request.getCharts() == null ? Map.of() : request.getCharts()
        );

        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    uri,
                    HttpMethod.POST,
                    new HttpEntity<>(payload, headers),
                    byte[].class
            );
            byte[] body = response.getBody();
            if (body == null || body.length == 0) {
                throw new IllegalStateException("Snapshot service returned an empty AI PDF report.");
            }
            return body;
        } catch (RestClientException e) {
            throw new RuntimeException("Failed to generate AI PDF report from snapshot service.", e);
        }
    }

    private byte[] zipDocuments(List<Map<String, Object>> documents,
                                Function<Map<String, Object>, byte[]> contentProducer,
                                String extension) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
            for (Map<String, Object> document : documents) {
                byte[] content = contentProducer.apply(document);
                ZipEntry entry = new ZipEntry(buildDocumentFilename(document, extension));
                zipOutputStream.putNextEntry(entry);
                zipOutputStream.write(content);
                zipOutputStream.closeEntry();
            }
            zipOutputStream.finish();
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to create export ZIP attachment.", e);
        }
    }

    private Document toDocument(Map<String, Object> rawDocument) {
        return new Document(objectMapper.convertValue(rawDocument, Map.class));
    }

    private String buildDocumentFilename(Map<String, Object> document, String extension) {
        Map<String, Object> uncategorized = asMap(document.get("uncategorized"));
        String templateName = sanitizeFilename(Objects.toString(uncategorized.get("qc_form_template_name"), "unknown"));
        Object inspectorsValue = uncategorized.get("related_inspectors");
        String inspectors;
        if (inspectorsValue instanceof List<?> list) {
            inspectors = list.stream().map(String::valueOf).reduce((left, right) -> left + "_" + right).orElse("");
        } else {
            inspectors = Objects.toString(inspectorsValue, "");
        }
        String safeInspectors = sanitizeFilename(inspectors);
        String createdAt = formatTimestampForFilename(Objects.toString(document.get("created_at"), null));
        return templateName + "_" + safeInspectors + "_" + createdAt + "." + extension;
    }

    private String buildEmailBody(QcSummaryEmailExportRequest request, List<String> attachmentNames, boolean english) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><body style=\"font-family: Arial, 'Microsoft YaHei', sans-serif;\">");
        html.append("<h2>").append(english ? "QC Summary Export" : "QC汇总导出").append("</h2>");
        html.append("<p>").append(english ? "The requested QC summary export is attached." : "所请求的QC汇总导出已作为附件发送。").append("</p>");
        html.append("<ul>");
        html.append("<li>").append(english ? "Date range (UTC)" : "日期范围（UTC）").append(": ")
                .append(escape(formatUtcDisplay(request.getStartDate()))).append(" ~ ").append(escape(formatUtcDisplay(request.getEndDate()))).append("</li>");
        html.append("<li>").append(english ? "Filters" : "筛选条件").append(": ")
                .append(escape(String.join(", ", buildFilterSummary(request, english)))).append("</li>");
        html.append("<li>").append(english ? "Attachments" : "附件").append(": ")
                .append(escape(String.join(", ", attachmentNames))).append("</li>");
        html.append("<li>").append(english ? "Generated at" : "生成时间").append(": ")
                .append(escape(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z").format(ZonedDateTime.now()))).append("</li>");
        html.append("</ul>");
        html.append("</body></html>");
        return html.toString();
    }

    private List<String> buildFilterSummary(QcSummaryEmailExportRequest request, boolean english) {
        List<String> filters = new ArrayList<>();
        if (request.getTeamId() != null) {
            String label = teamRepository.findById(request.getTeamId())
                    .map(Team::getName)
                    .orElse("ID " + request.getTeamId());
            filters.add((english ? "Team" : "班组") + ": " + label);
        }
        if (request.getShiftId() != null) {
            filters.add((english ? "Shift" : "班次") + ": ID " + request.getShiftId());
        }
        if (request.getProductId() != null) {
            String label = suggestedProductRepository.findById(request.getProductId().longValue())
                    .map(SuggestedProduct::getName)
                    .orElse("ID " + request.getProductId());
            filters.add((english ? "Product" : "产品") + ": " + label);
        }
        if (request.getBatchId() != null) {
            String label = suggestedBatchRepository.findById(request.getBatchId().longValue())
                    .map(batch -> StringUtils.hasText(batch.getCode()) ? batch.getCode() : batch.getName())
                    .orElse("ID " + request.getBatchId());
            filters.add((english ? "Batch" : "批次") + ": " + label);
        }
        if (request.getFormTemplateId() != null) {
            String label = qcFormTemplateRepository.findById(request.getFormTemplateId())
                    .map(QcFormTemplate::getName)
                    .orElse("ID " + request.getFormTemplateId());
            filters.add((english ? "Form Template" : "表单模板") + ": " + label);
        }
        if (filters.isEmpty()) {
            filters.add(english ? "None" : "无");
        }
        return filters;
    }

    private Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> rawMap) {
            Map<String, Object> map = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                map.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            return map;
        }
        return Map.of();
    }

    private String formatTimestampForFilename(String createdAt) {
        if (!StringUtils.hasText(createdAt)) {
            return "invalid_date";
        }
        try {
            return DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                    .format(DateTimeFormatter.ISO_DATE_TIME.parse(createdAt));
        } catch (Exception ignored) {
            try {
                return DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                        .format(ZonedDateTime.parse(createdAt));
            } catch (Exception ignoredAgain) {
                return "invalid_date";
            }
        }
    }

    private String formatUtcDisplay(String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            return "";
        }
        try {
            Instant instant = Instant.parse(rawValue);
            return UTC_DISPLAY_FORMATTER.format(instant.atZone(ZoneOffset.UTC));
        } catch (Exception ignored) {
            try {
                return UTC_DISPLAY_FORMATTER.format(ZonedDateTime.parse(rawValue).withZoneSameInstant(ZoneOffset.UTC));
            } catch (Exception ignoredAgain) {
                return rawValue;
            }
        }
    }

    private String sanitizeFilename(String input) {
        String value = StringUtils.hasText(input) ? input.trim() : "unknown";
        return value.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private String escape(String value) {
        return HtmlUtils.htmlEscape(value == null ? "" : value);
    }

    private boolean isEnglish(String acceptLanguage) {
        return acceptLanguage != null && acceptLanguage.toLowerCase(Locale.ROOT).startsWith("en");
    }

    @Data
    @AllArgsConstructor
    private static class AiReportPayload {
        @JsonProperty("start_date")
        private String startDate;
        @JsonProperty("end_date")
        private String endDate;
        @JsonProperty("team_id")
        private Integer teamId;
        @JsonProperty("shift_id")
        private Integer shiftId;
        @JsonProperty("product_id")
        private Integer productId;
        @JsonProperty("batch_id")
        private Integer batchId;
        @JsonProperty("form_template_id")
        private Long formTemplateId;
        private String timezone;
        private Map<String, String> charts;
    }
}
