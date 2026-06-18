package com.fps.svmes.models.nosql.approval;

import com.fps.shared.entity.BaseInstant;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

@Document(collection = "qc-approval-instance")
@Data
@EqualsAndHashCode(callSuper = true)
public class ApprovalInstance extends BaseInstant {
    @Id
    private String id;

    @Field("formSubmissionId")
    private String formSubmissionId;

    @Field("formSubmissionCollectionName")
    private String formSubmissionCollectionName;

    @Field("approvalTemplateId")
    private String approvalTemplateId;

    @Field("approvalTemplateName")
    private String approvalTemplateName;

    @Field("formTemplateId")
    private String formTemplateId;

    @Field("currentStepSequence")
    private Integer currentStepSequence;

    @Field("approvalSteps")
    private List<ApprovalInstanceStep> approvalSteps;

    @Field("approvalProcessStatus")
    private String approvalProcessStatus;

    @Field("actionLog")
    private List<ApprovalActionLog> actionLog;

    @Field("isAlarmTriggered")
    private Boolean isAlarmTriggered;

    @Field("filterSnapshot")
    private FormSubmissionSnapshotForFilter filterSnapshot;
}
