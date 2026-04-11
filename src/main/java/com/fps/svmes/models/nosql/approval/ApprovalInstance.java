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

    @Field("formTemplateId")
    private String formTemplateId;

    @Field("currentStepSequence")
    private Integer currentStepSequence;

    @Field("approvalSteps")
    private List<ApprovalInstanceStep> approvalSteps;

    @Field("action_log")
    private List<ApprovalActionLog> actionLog;

    @Field("versionNumber")
    private Integer versionNumber;

    @Field("filterSnapshot")
    private ApprovalInstanceFilterSnapshot filterSnapshot;
}
