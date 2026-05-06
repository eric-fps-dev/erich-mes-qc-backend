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
    private Object relatedInspectors;
    private List<Long> relatedProductIds;
    private Object relatedProducts;
    private List<Long> relatedBatchIds;
    private Object relatedBatches;
    private Long relatedTeamId;
    private Object relatedTeams;
    private Long relatedShiftId;
    private Object relatedShifts;
    private Boolean isAlarmTriggered;
}
