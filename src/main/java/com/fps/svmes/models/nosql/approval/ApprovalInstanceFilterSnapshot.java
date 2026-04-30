package com.fps.svmes.models.nosql.approval;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class ApprovalInstanceFilterSnapshot {
    private Long formTemplateId;
    private String formSubmissionState;
    private Integer formSubmissionVersion;
    private String versionGroupId;
    private Date createdAt;
    private Long createdBy;
    private List<Long> relatedInspectorIds;
    private List<Long> relatedProductIds;
    private List<Long> relatedBatchIds;
    private Long relatedTeamId;
    private Long relatedShiftId;
    private Boolean isAlarmTriggered;
}
