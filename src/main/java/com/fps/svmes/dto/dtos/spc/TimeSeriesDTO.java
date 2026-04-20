package com.fps.svmes.dto.dtos.spc;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TimeSeriesDTO {
    private Timestamp timestamp;
    private Double value;
}
