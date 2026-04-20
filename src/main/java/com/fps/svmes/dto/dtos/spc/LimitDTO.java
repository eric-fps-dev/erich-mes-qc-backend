package com.fps.svmes.dto.dtos.spc;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LimitDTO {
    private Double lowLimit;
    private Double maxLimit;
}
