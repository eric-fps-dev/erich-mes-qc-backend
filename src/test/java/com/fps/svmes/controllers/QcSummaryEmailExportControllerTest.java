package com.fps.svmes.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fps.svmes.dto.requests.QcSummaryEmailExportRequest;
import com.fps.svmes.dto.responses.QcSummaryEmailExportResponse;
import com.fps.svmes.services.QcSummaryEmailExportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class QcSummaryEmailExportControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final StubService service = new StubService();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new QcSummaryEmailExportController(service))
                .setValidator(validator)
                .build();
    }

    @Test
    void shouldReturn400WhenRecipientsAreMissing() throws Exception {
        QcSummaryEmailExportRequest request = validRequest();
        request.setRecipients(List.of());

        mockMvc.perform(post("/export-send-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenNoFormatsAreSelected() throws Exception {
        QcSummaryEmailExportRequest request = validRequest();
        request.setFormats(new QcSummaryEmailExportRequest.Formats(false, false, false));

        mockMvc.perform(post("/export-send-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenPayloadIsMalformed() throws Exception {
        mockMvc.perform(post("/export-send-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"start_date\":"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn500WhenServiceFails() throws Exception {
        service.runtimeException = new RuntimeException("boom");

        mockMvc.perform(post("/export-send-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Accept-Language", "en-US")
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void shouldReturn200ForValidRequest() throws Exception {
        service.response = new QcSummaryEmailExportResponse("ok", 1, List.of("file.zip"));

        mockMvc.perform(post("/export-send-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Accept-Language", "en-US")
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk());
    }

    private QcSummaryEmailExportRequest validRequest() {
        return new QcSummaryEmailExportRequest(
                "2026-04-01T00:00:00Z",
                "2026-04-08T00:00:00Z",
                1,
                2,
                3,
                4,
                5L,
                "America/Los_Angeles",
                List.of("qc@example.com"),
                new QcSummaryEmailExportRequest.Formats(true, false, false),
                Map.of("pass_rate", "data:image/png;base64,abc")
        );
    }

    private static class StubService implements QcSummaryEmailExportService {
        private QcSummaryEmailExportResponse response = new QcSummaryEmailExportResponse("ok", 1, List.of("file.zip"));
        private RuntimeException runtimeException;

        @Override
        public QcSummaryEmailExportResponse exportAndSendEmail(QcSummaryEmailExportRequest request, String acceptLanguage) {
            if (runtimeException != null) {
                throw runtimeException;
            }
            return response;
        }
    }
}
