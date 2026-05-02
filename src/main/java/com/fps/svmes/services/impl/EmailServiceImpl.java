package com.fps.svmes.services.impl;

import com.fps.svmes.services.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@example.com}")
    private String fromEmail;

    @Override
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        sendHtmlEmailWithAttachments(to, List.of(), subject, htmlContent, Map.of());
    }

    @Override
    public void sendHtmlEmailWithAttachments(String to, List<String> bccRecipients, String subject, String htmlContent, Map<String, byte[]> attachments) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            if (bccRecipients != null && !bccRecipients.isEmpty()) {
                helper.setBcc(bccRecipients.toArray(new String[0]));
            }
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            if (attachments != null && !attachments.isEmpty()) {
                for (Map.Entry<String, byte[]> entry : attachments.entrySet()) {
                    helper.addAttachment(entry.getKey(), new ByteArrayResource(entry.getValue()));
                }
            }

            mailSender.send(message);
            log.info("Email sent successfully to: {} (bccRecipients={}, attachments={})",
                    to,
                    bccRecipients == null ? 0 : bccRecipients.size(),
                    attachments == null ? 0 : attachments.size());
        } catch (MessagingException e) {
            log.error("Failed to send email to: {}", to, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    @Override
    public void sendWeeklyReport(String to, String subject, String htmlContent) {
        sendHtmlEmail(to, subject, htmlContent);
        log.info("Weekly report sent to: {}", to);
    }
}
