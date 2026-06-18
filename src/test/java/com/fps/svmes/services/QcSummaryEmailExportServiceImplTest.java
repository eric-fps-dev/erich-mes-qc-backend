package com.fps.svmes.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fps.svmes.dto.dtos.qcForm.QcTaskSubmissionLogsDTO;
import com.fps.svmes.dto.requests.FormSubmissionActionRequest;
import com.fps.svmes.dto.requests.QcSummaryEmailExportRequest;
import com.fps.svmes.dto.responses.QcSummaryEmailExportResponse;
import com.fps.svmes.services.impl.QcSummaryEmailExportServiceImpl;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QcSummaryEmailExportServiceImplTest {

    private StubRestTemplate restTemplate;
    private CapturingEmailService emailService;
    private StubQcTaskSubmissionLogsService logsService;
    private QcSummaryEmailExportServiceImpl service;

    @BeforeEach
    void setUp() {
        restTemplate = new StubRestTemplate();
        emailService = new CapturingEmailService();
        logsService = new StubQcTaskSubmissionLogsService();
        service = new QcSummaryEmailExportServiceImpl(
                restTemplate,
                emailService,
                logsService,
                new ObjectMapper(),
                null,
                null,
                null,
                null
        );
        ReflectionTestUtils.setField(service, "qcSnapshotApiUrl", "http://snapshot");
        ReflectionTestUtils.setField(service, "fromEmail", "noreply@example.com");
    }

    @Test
    void pdfOnlyShouldSendOneZipAttachment() throws Exception {
        restTemplate.documentListResponse = sampleDocumentListResponse();
        logsService.pdfBytes = "pdf-bytes".getBytes();

        QcSummaryEmailExportResponse response = service.exportAndSendEmail(baseRequest(true, false, false), "en-US");

        assertEquals("noreply@example.com", emailService.to);
        assertEquals(List.of("one@example.com", "two@example.com"), emailService.bccRecipients);
        assertEquals(1, emailService.attachments.size());
        assertEquals(List.of("qc_example_Inspector A_20260401_120000.pdf"), zipEntries(emailService.attachments.values().iterator().next()));
        assertEquals(2, response.getRecipientCount());
    }

    @Test
    void excelOnlyShouldSendOneZipAttachment() throws Exception {
        restTemplate.documentListResponse = sampleDocumentListResponse();
        logsService.excelBytes = "xlsx-bytes".getBytes();

        service.exportAndSendEmail(baseRequest(false, true, false), "en-US");

        assertEquals(1, emailService.attachments.size());
        assertEquals(List.of("qc_example_Inspector A_20260401_120000.xlsx"), zipEntries(emailService.attachments.values().iterator().next()));
    }

    @Test
    void aiOnlyShouldSendOnePdfAttachment() {
        restTemplate.aiPdfBytes = "ai-pdf".getBytes();

        service.exportAndSendEmail(baseRequest(false, false, true), "en-US");

        assertEquals(0, restTemplate.getCallCount);
        assertEquals(1, restTemplate.postCallCount);
        assertEquals(1, emailService.attachments.size());
        assertTrue(emailService.attachments.keySet().iterator().next().endsWith(".pdf"));
        assertArrayEquals("ai-pdf".getBytes(), emailService.attachments.values().iterator().next());
    }

    @Test
    void combinedSelectionsShouldAttachAllArtifacts() {
        restTemplate.documentListResponse = sampleDocumentListResponse();
        restTemplate.aiPdfBytes = "ai-pdf".getBytes();
        logsService.pdfBytes = "pdf-bytes".getBytes();
        logsService.excelBytes = "xlsx-bytes".getBytes();

        service.exportAndSendEmail(baseRequest(true, true, true), "en-US");

        assertEquals(3, emailService.attachments.size());
        assertEquals(1, restTemplate.getCallCount);
        assertEquals(1, restTemplate.postCallCount);
    }

    @Test
    void shouldPropagateSnapshotFailures() {
        restTemplate.getException = new RuntimeException("downstream");

        assertThrows(RuntimeException.class, () -> service.exportAndSendEmail(baseRequest(true, false, false), "en-US"));
    }

    private Map<String, Object> sampleDocumentListResponse() {
        return Map.of(
                "data", List.of(Map.of(
                        "_id", "abc123",
                        "created_at", "2026-04-01T12:00:00",
                        "created_by", 7,
                        "uncategorized", Map.of(
                                "qc_form_template_name", "qc/example",
                                "related_inspectors", List.of("Inspector A")
                        )
                ))
        );
    }

    private QcSummaryEmailExportRequest baseRequest(boolean pdf, boolean excel, boolean ai) {
        return new QcSummaryEmailExportRequest(
                "2026-04-01T00:00:00Z",
                "2026-04-08T00:00:00Z",
                null,
                null,
                null,
                null,
                null,
                "America/Los_Angeles",
                List.of("one@example.com", "two@example.com"),
                new QcSummaryEmailExportRequest.Formats(pdf, excel, ai),
                Map.of("chart", "data:image/png;base64,abc")
        );
    }

    private List<String> zipEntries(byte[] zipBytes) throws Exception {
        try (ZipInputStream inputStream = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ArrayList<String> entries = new ArrayList<>();
            ZipEntry entry;
            while ((entry = inputStream.getNextEntry()) != null) {
                entries.add(entry.getName());
            }
            return entries;
        }
    }

    private static class StubRestTemplate extends RestTemplate {
        private Map<String, Object> documentListResponse;
        private byte[] aiPdfBytes;
        private RuntimeException getException;
        private RuntimeException postException;
        private int getCallCount;
        private int postCallCount;

        @Override
        @SuppressWarnings("unchecked")
        public <T> ResponseEntity<T> exchange(URI url, HttpMethod method, HttpEntity<?> requestEntity, ParameterizedTypeReference<T> responseType) {
            getCallCount++;
            if (getException != null) {
                throw getException;
            }
            return ResponseEntity.ok((T) documentListResponse);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> ResponseEntity<T> exchange(URI url, HttpMethod method, HttpEntity<?> requestEntity, Class<T> responseType) {
            postCallCount++;
            if (postException != null) {
                throw postException;
            }
            return ResponseEntity.ok((T) aiPdfBytes);
        }
    }

    private static class CapturingEmailService implements EmailService {
        private String to;
        private List<String> bccRecipients;
        private String subject;
        private String htmlContent;
        private Map<String, byte[]> attachments = Map.of();

        @Override
        public void sendHtmlEmail(String to, String subject, String htmlContent) {
            this.to = to;
            this.subject = subject;
            this.htmlContent = htmlContent;
        }

        @Override
        public void sendHtmlEmailWithAttachments(String to, List<String> bccRecipients, String subject, String htmlContent, Map<String, byte[]> attachments) {
            this.to = to;
            this.bccRecipients = bccRecipients;
            this.subject = subject;
            this.htmlContent = htmlContent;
            this.attachments = attachments;
        }

        @Override
        public void sendWeeklyReport(String to, String subject, String htmlContent) {
            sendHtmlEmail(to, subject, htmlContent);
        }
    }

    private static class StubQcTaskSubmissionLogsService implements QcTaskSubmissionLogsService {
        private byte[] pdfBytes = new byte[0];
        private byte[] excelBytes = new byte[0];

        @Override
        public QcTaskSubmissionLogsDTO insertLog(QcTaskSubmissionLogsDTO dto) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<QcTaskSubmissionLogsDTO> getAllByCreatedByAndTaskId(Integer createdBy, Long dispatchedTaskId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Document getDocumentBySubmissionId(String submissionId, Long formId, Integer createdBy, Optional<String> inputCollectionName) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Document> getDocumentsByQcFormTemplateIdAndCreatedBy(Long qcFormTemplateId, Integer createdBy) {
            throw new UnsupportedOperationException();
        }

        @Override
        public byte[] exportDocumentsToExcel(List<Document> documents) {
            throw new UnsupportedOperationException();
        }

        @Override
        public byte[] exportDocumentToExcel(Document document) {
            return excelBytes;
        }

        @Override
        public byte[] exportDocumentToPdf(Document document) {
            return pdfBytes;
        }

        @Override
         public void deleteSubmissionLog(String submissionId, @NonNull  String collectionName) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Document getRawDocumentBySubmissionId(String submissionId, String collectionName) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Map<String, Object>> getFormTemplateFieldList(Long formId) {
            throw new UnsupportedOperationException();
        }
    }
}
