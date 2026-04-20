package com.fps.svmes.dto.dtos.reporting;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.security.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WidgetDataDTO {
    private String name;
    private String label;
    private String type;
    private List<OptionItemDTO> optionItems;
    private List<Double> chartData; // Holds numeric data
    private List<String> xaxisData; // Holds formatted timestamps

    // NEW: Time-bucketed data for categorical trends
    private List<TimeBucketedOptionDTO> timeBucketedData;  // Each option with counts array
    private List<String> bucketLabels;                      // ["2024-01-01", "2024-01-02", ...]
    private String bucketType;                              // "hourly", "daily", "weekly"

    private boolean deleted;

    public WidgetDataDTO(String name, String label, String type, List<OptionItemDTO> optionItems, List<Double> chartData, List<String> xaxisData) {
        this.name = name;
        this.label = label;
        this.type = type;
        this.optionItems = optionItems;
        this.chartData = chartData;
        this.xaxisData = xaxisData;
    }
}
