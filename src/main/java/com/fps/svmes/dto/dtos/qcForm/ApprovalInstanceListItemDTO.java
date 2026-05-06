package com.fps.svmes.dto.dtos.qcForm;

import lombok.Data;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
public class ApprovalInstanceListItemDTO {
    private String submissionId;
    private String collectionName;
    private Long formTemplateId;
    private String formTemplateName;
    private String formSubmissionState;
    private String approvalInstanceId;
    private String approvalTemplateId;
    private Integer status;
    private Integer currentStepSequence;
    private List<ApprovalInstanceListStepDTO> approvalSteps;
    private Integer approvalInstanceVersion;
    private Integer formSubmissionVersion;
    private Date createdAt;
    private Instant updatedAt;
    private Object createdBy;
    private Long updatedBy;
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
