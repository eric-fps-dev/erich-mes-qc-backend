package com.fps.svmes.dto.dtos.reporting;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeBucketedOptionDTO {
    private String label;           // Option display name (e.g., "Pass", "Fail")
    private int value;              // Option value/ID
    private List<Integer> counts;   // Time-bucketed counts [12, 8, 15, ...]
}
