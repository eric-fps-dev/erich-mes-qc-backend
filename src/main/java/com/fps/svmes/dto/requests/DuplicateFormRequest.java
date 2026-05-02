package com.fps.svmes.dto.requests;

import lombok.Data;

@Data
public class DuplicateFormRequest {
    private String sourceNodeId;
    private Integer requestedBy;
}
