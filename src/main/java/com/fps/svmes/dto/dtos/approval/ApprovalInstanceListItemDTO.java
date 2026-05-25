package com.fps.svmes.dto.dtos.approval;

import lombok.Data;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
public class ApprovalInstanceListItemDTO {
    // Approval instance related fields
    private String approvalInstanceId;
    private String approvalTemplateId;
    private String approvalTemplateName;
    private Integer currentStepSequence;
    private List<ApprovalInstanceListStepDTO> approvalSteps;
    private String approvalProcessStatus;
    private Date createdAt;
    private Instant updatedAt;
    private Object createdBy;
    private Long updatedBy;

    // Form submission related fields
    private String formSubmissionId;
    private String formSubmissionState;
    private Integer formSubmissionVersion;

    // Form template related fields
    private String collectionName;
    private Long formTemplateId;
    private String formTemplateName;

    // Related entities
    private Object relatedInspectorIds;
    private Object relatedInspectors;
    private Object relatedProductIds;
    private Object relatedProducts;
    private Object relatedBatchIds;
    private Object relatedBatches;
    private Object relatedTeamId;
    private Object relatedTeams;
    private Object relatedShiftId;
    private Object relatedShifts;
    
    private Boolean isAlarmTriggered;
    private Map<String, Object> formData;
}
