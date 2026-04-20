package com.fps.svmes.dto.requests;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class ApprovalActionRequest {
    private String submissionId;
    private String collectionName;
    private String role;
    private Integer approverId;
    private String comment;
    private boolean suggestRetest;

    @JsonProperty("eSignature")
    private String eSignature;
}
