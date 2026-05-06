package com.fps.svmes.services;

import com.fps.svmes.dto.requests.QcSummaryEmailExportRequest;
import com.fps.svmes.dto.responses.QcSummaryEmailExportResponse;

public interface QcSummaryEmailExportService {
    QcSummaryEmailExportResponse exportAndSendEmail(QcSummaryEmailExportRequest request, String acceptLanguage);
}
