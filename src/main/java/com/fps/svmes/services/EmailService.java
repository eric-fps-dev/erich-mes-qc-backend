package com.fps.svmes.services;

import java.util.List;
import java.util.Map;

public interface EmailService {

    void sendHtmlEmail(String to, String subject, String htmlContent);

    void sendHtmlEmailWithAttachments(String to, List<String> bccRecipients, String subject, String htmlContent, Map<String, byte[]> attachments);

    void sendWeeklyReport(String to, String subject, String htmlContent);
}
