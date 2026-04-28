package com.fps.svmes.services.impl;

import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailServiceImplTest {

    private CapturingMailSender mailSender;
    private EmailServiceImpl emailService;

    @BeforeEach
    void setUp() {
        mailSender = new CapturingMailSender();
        emailService = new EmailServiceImpl(mailSender);
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@example.com");
    }

    @Test
    void sendHtmlEmailWithAttachmentsShouldPopulateVisibleToBccAndAttachments() throws Exception {
        emailService.sendHtmlEmailWithAttachments(
                "noreply@example.com",
                List.of("one@example.com", "two@example.com"),
                "Subject",
                "<b>hello</b>",
                Map.of("report.zip", "zip-bytes".getBytes())
        );

        MimeMessage sent = mailSender.sentMessage;
        assertEquals("noreply@example.com", sent.getRecipients(Message.RecipientType.TO)[0].toString());
        assertEquals(2, sent.getRecipients(Message.RecipientType.BCC).length);
        Object content = sent.getContent();
        assertTrue(content instanceof jakarta.mail.Multipart);
        jakarta.mail.Multipart multipart = (jakarta.mail.Multipart) content;
        assertEquals(2, multipart.getCount());
    }

    @Test
    void sendWeeklyReportShouldKeepExistingBehavior() throws Exception {
        emailService.sendWeeklyReport("weekly@example.com", "Weekly", "<p>body</p>");

        MimeMessage sent = mailSender.sentMessage;
        assertEquals("weekly@example.com", sent.getRecipients(Message.RecipientType.TO)[0].toString());
        assertEquals("Weekly", sent.getSubject());
    }

    private static class CapturingMailSender extends JavaMailSenderImpl {
        private MimeMessage sentMessage;

        @Override
        public MimeMessage createMimeMessage() {
            return new MimeMessage(Session.getInstance(new Properties()));
        }

        @Override
        public MimeMessage createMimeMessage(InputStream contentStream) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void send(MimeMessage mimeMessage) {
            this.sentMessage = mimeMessage;
        }

        @Override
        public void send(MimeMessage... mimeMessages) {
            this.sentMessage = mimeMessages[0];
        }

        @Override
        public void send(SimpleMailMessage simpleMessage) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void send(SimpleMailMessage... simpleMessages) {
            throw new UnsupportedOperationException();
        }
    }
}
