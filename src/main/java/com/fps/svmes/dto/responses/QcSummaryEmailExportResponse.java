package com.fps.svmes.dto.responses;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QcSummaryEmailExportResponse {
    private String message;
    private int recipientCount;
    private List<String> attachmentNames;
}
