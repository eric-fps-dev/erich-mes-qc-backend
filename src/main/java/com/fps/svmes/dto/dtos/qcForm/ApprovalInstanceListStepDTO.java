package com.fps.svmes.dto.dtos.qcForm;

import com.fps.svmes.enums.approval.ApprovalStepState;
import lombok.Data;

@Data
public class ApprovalInstanceListStepDTO {
    private Integer sequence;
    private String stepName;
    private String requiredUserId;
    private String requiredRoleId;
    private String requiredType;
    private ApprovalStepState stepState;
    private Integer resetCounter;
}
