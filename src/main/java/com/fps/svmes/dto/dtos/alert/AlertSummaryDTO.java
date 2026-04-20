package com.fps.svmes.dto.dtos.alert;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertSummaryDTO {
    private Map<String, Long> alertStatusCounts;
    private Map<String, Long> riskLevelCounts;
    private Map<String, Long> productCounts;
    private Map<String, Long> inspectionItemCounts;
}

