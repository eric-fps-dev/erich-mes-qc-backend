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
    private Object relatedProductIds;
    private Object relatedBatchIds;
    private Object relatedTeamId;
    private Object relatedShiftId;
    private Map<String, Object> formData;
}
