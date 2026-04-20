package com.fps.svmes.dto.dtos.spc;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SPCDTO {
    private String fieldId;

    private String fieldName;

    private Integer timeSeriesCount;

    private LimitDTO limits;

    private List<TimeSeriesDTO> timeSeries;
}
