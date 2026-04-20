package com.fps.svmes.schedulers;

import com.fps.svmes.dto.dtos.subscription.WeeklyReportSubscriptionDTO;
import com.fps.svmes.models.sql.subscription.WeeklyReportSubscription;
import com.fps.svmes.repositories.jpaRepo.subscription.WeeklyReportSubscriptionRepository;
import com.fps.svmes.services.EmailService;
import com.fps.svmes.services.WeeklyReportSubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class WeeklyReportScheduler {

    private final WeeklyReportSubscriptionService subscriptionService;
    private final WeeklyReportSubscriptionRepository subscriptionRepository; // Needed for direct access to check language if not in DTO or just to fetch by ID
    private final EmailService emailService;
    private final RestTemplate restTemplate;

    @Value("${qc.snapshot.api.url:http://localhost:8081}")
    private String qcSnapshotApiUrl;

    /**
     * Runs every Monday at 8:00 AM
     * Cron format: second minute hour day-of-month month day-of-week
     */
    @Scheduled(cron = "0 0 8 * * MON")
    public void sendWeeklyReports() {
        // Pass null to use per-subscriber language preference
        sendWeeklyReports(null);
    }

    /**
     * Internal method to send reports.
     * @param langOverride If provided, forces this language for ALL subscribers (for testing).
     *                     If null, uses each subscriber's preferred language.
     */
    public void sendWeeklyReports(String langOverride) {
        log.info("Starting weekly report email job (Override Language: {})...", langOverride);

        List<WeeklyReportSubscriptionDTO> subscriptions = subscriptionService.getActiveSubscriptions();

        if (subscriptions.isEmpty()) {
            log.info("No active subscriptions found. Skipping weekly report.");
            return;
        }

        // Calculate date range (last 7 days)
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(7);
        String startDateStr = startDate.format(DateTimeFormatter.ISO_DATE);
        String endDateStr = endDate.format(DateTimeFormatter.ISO_DATE);

        if (langOverride != null && !langOverride.isEmpty()) {
            // Case 1: Override language provided (Test mode)
            log.info("Sending weekly report to ALL subscribers in language: {}", langOverride);
            sendBatch(subscriptions, startDateStr, endDateStr, langOverride);
        } else {
            // Case 2: Use per-subscriber language preference
            log.info("Sending weekly report using subscriber preferences");
            
            // Group by language (default to 'en' if null)
            Map<String, List<WeeklyReportSubscriptionDTO>> byLang = subscriptions.stream()
                    .collect(Collectors.groupingBy(s -> s.getLanguage() != null ? s.getLanguage() : "en"));

            for (Map.Entry<String, List<WeeklyReportSubscriptionDTO>> entry : byLang.entrySet()) {
                String lang = entry.getKey();
                List<WeeklyReportSubscriptionDTO> recipients = entry.getValue();
                log.info("Processing batch for language: {} ({} recipients)", lang, recipients.size());
                sendBatch(recipients, startDateStr, endDateStr, lang);
            }
        }
    }

    /**
     * Sends report to a specific subscriber immediately
     */
    public void sendReportToSubscriber(Integer subscriptionId) {
        Optional<WeeklyReportSubscription> subOpt = subscriptionRepository.findById(subscriptionId);
        if (subOpt.isEmpty()) {
            throw new RuntimeException("Subscription not found: " + subscriptionId);
        }
        WeeklyReportSubscription sub = subOpt.get();
        if (!Boolean.TRUE.equals(sub.getIsActive())) {
             log.warn("Sending report to INACTIVE subscriber: {}", sub.getEmail());
        }

        // Calculate date range (last 7 days)
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(7);
        String startDateStr = startDate.format(DateTimeFormatter.ISO_DATE);
        String endDateStr = endDate.format(DateTimeFormatter.ISO_DATE);

        String lang = sub.getLanguage() != null ? sub.getLanguage() : "en";
        String reportContent = generateWeeklyReportContent(startDateStr, endDateStr, lang);
        
        String subject = lang.equalsIgnoreCase("en")
                ? String.format("QC Weekly Report: %s ~ %s", startDateStr, endDateStr)
                : String.format("QC质检周报: %s ~ %s", startDateStr, endDateStr);

        emailService.sendWeeklyReport(sub.getEmail(), subject, reportContent);
        log.info("Sent immediate report to subscriber: {} (Language: {})", sub.getEmail(), lang);
    }

    private void sendBatch(List<WeeklyReportSubscriptionDTO> recipients, String startDate, String endDate, String lang) {
        // Generate content ONCE for this language
        String reportContent = generateWeeklyReportContent(startDate, endDate, lang);
        String subject = lang.equalsIgnoreCase("en")
                ? String.format("QC Weekly Report: %s ~ %s", startDate, endDate)
                : String.format("QC质检周报: %s ~ %s", startDate, endDate);

        int successCount = 0;
        int failCount = 0;

        for (WeeklyReportSubscriptionDTO sub : recipients) {
            try {
                emailService.sendWeeklyReport(sub.getEmail(), subject, reportContent);
                successCount++;
                log.info("Weekly report sent to: {}", sub.getEmail());
            } catch (Exception e) {
                failCount++;
                log.error("Failed to send report to: {}", sub.getEmail(), e);
            }
        }
        log.info("Batch completed for language {}. Success: {}, Failed: {}", lang, successCount, failCount);
    }

    /**
     * Manual trigger for testing - can be called via controller
     */
    public void sendWeeklyReportsManual(String lang) {
        sendWeeklyReports(lang);
    }

    @SuppressWarnings("unchecked")
    private String generateWeeklyReportContent(String startDate, String endDate, String lang) {
        try {
            // Try to get HTML report from qc-snapshot-service
            String htmlReportUrl = String.format("%s/summary/weekly-email-report?start_date=%s&end_date=%s&lang=%s",
                    qcSnapshotApiUrl, startDate, endDate, lang);

            try {
                String htmlReport = restTemplate.getForObject(htmlReportUrl, String.class);
                if (htmlReport != null && !htmlReport.isEmpty()) {
                    return htmlReport;
                }
            } catch (Exception e) {
                log.warn("Could not fetch HTML report from snapshot service, falling back to basic report", e);
            }

            // Fallback: Get card stats and build basic HTML
            String cardStatsUrl = String.format("%s/summary/card-stats?start_date=%s&end_date=%s",
                    qcSnapshotApiUrl, startDate, endDate);

            Map<String, Object> cardStats = restTemplate.getForObject(cardStatsUrl, Map.class);

            return buildBasicHtmlReport(startDate, endDate, cardStats, lang);
        } catch (Exception e) {
            log.error("Error generating weekly report content", e);
            return buildErrorReport(startDate, endDate, lang);
        }
    }

    private String buildBasicHtmlReport(String startDate, String endDate, Map<String, Object> cardStats, String lang) {
        boolean isEn = "en".equalsIgnoreCase(lang);
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html><head>");
        html.append("<meta charset=\"UTF-8\">");
        html.append("<style>");
        html.append("body { font-family: Arial, 'Microsoft YaHei', sans-serif; margin: 0; padding: 20px; background-color: #f5f5f5; }");
        html.append(".container { max-width: 600px; margin: 0 auto; background: white; border-radius: 8px; padding: 30px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }");
        html.append("h1 { color: #2c3e50; border-bottom: 3px solid #3498db; padding-bottom: 10px; }");
        html.append("h2 { color: #34495e; margin-top: 25px; }");
        html.append("table { border-collapse: collapse; width: 100%; margin: 15px 0; }");
        html.append("th, td { border: 1px solid #ddd; padding: 12px 8px; text-align: left; }");
        html.append("th { background-color: #3498db; color: white; }");
        html.append("tr:nth-child(even) { background-color: #f9f9f9; }");
        html.append(".footer { margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee; color: #7f8c8d; font-size: 12px; }");
        html.append("</style>");
        html.append("</head><body>");
        html.append("<div class=\"container\">");
        html.append("<h1>").append(isEn ? "QC Weekly Summary Report" : "QC质检周报").append("</h1>");
        html.append("<p><strong>").append(isEn ? "Report Period" : "报告周期").append(":</strong> ").append(startDate).append(" ~ ").append(endDate).append("</p>");

        if (cardStats != null && !cardStats.isEmpty()) {
            html.append("<h2>").append(isEn ? "Key Metrics" : "关键指标").append("</h2>");
            html.append("<table>");
            html.append("<tr><th>").append(isEn ? "Metric" : "指标").append("</th><th>").append(isEn ? "Value" : "数值").append("</th></tr>");

            for (Map.Entry<String, Object> entry : cardStats.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                html.append("<tr><td>").append(formatMetricName(key)).append("</td>");
                html.append("<td>").append(value != null ? value.toString() : "-").append("</td></tr>");
            }
            html.append("</table>");
        } else {
            html.append("<p>").append(isEn ? "No data available." : "暂无数据").append("</p>");
        }

        html.append("<div class=\"footer\">");
        html.append("<p>").append(isEn ? "This is an automated report." : "此邮件由系统自动生成。").append("</p>");
        html.append("</div>");
        html.append("</div>");
        html.append("</body></html>");

        return html.toString();
    }

    private String buildErrorReport(String startDate, String endDate, String lang) {
        boolean isEn = "en".equalsIgnoreCase(lang);
        return String.format(
                "<!DOCTYPE html><html><body>" +
                        "<h2>%s</h2>" +
                        "<p>%s: %s ~ %s</p>" +
                        "<p style=\"color: red;\">%s</p>" +
                        "</body></html>",
                isEn ? "QC Weekly Report" : "QC质检周报",
                isEn ? "Period" : "周期",
                startDate, endDate,
                isEn ? "Error generating report." : "生成报告时出错。"
        );
    }

    private String formatMetricName(String key) {
        // Convert snake_case or camelCase to readable format
        return key.replace("_", " ")
                .replaceAll("([a-z])([A-Z])", "$1 $2")
                .substring(0, 1).toUpperCase() + key.substring(1).replace("_", " ");
    }
}