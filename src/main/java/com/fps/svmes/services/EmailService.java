package com.fps.svmes.services;

public interface EmailService {

    void sendHtmlEmail(String to, String subject, String htmlContent);

    void sendWeeklyReport(String to, String subject, String htmlContent);
}
