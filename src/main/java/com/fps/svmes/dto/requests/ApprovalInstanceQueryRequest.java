package com.fps.svmes.dto.requests;

import lombok.Data;
import org.springframework.data.domain.Sort;

@Data
public class ApprovalInstanceQueryRequest {
    private int page = 0;
    private int size = 10;
    private String sortField = "created_at";
    private Sort.Direction sortDirection = Sort.Direction.DESC;
    private boolean includeFormData;
    private Long submitterUserId;
    private Long inspectorUserId;
    private Long suggestedProductId;
    private Long suggestedBatchId;
    private Long teamId;
    private Long shiftId;
    private Long formTemplateId;
    private String approvalTemplateId;
    private String createdAtStart;
    private String createdAtEnd;
    private String formSubmissionState;
}
