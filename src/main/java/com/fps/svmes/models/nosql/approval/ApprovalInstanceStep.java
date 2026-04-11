package com.fps.svmes.models.nosql.approval;

import com.fps.svmes.enums.approval.ApprovalStepState;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
public class ApprovalInstanceStep {
    @Field("sequence")
    private Integer sequence;

    @Field("stepName")
    private String stepName;

    @Field("requiredUserId")
    private String requiredUserId;

    @Field("requiredRoleId")
    private String requiredRoleId;

    @Field("requiredType")
    private String requiredType;

    @Field("last_action_record")
    private ApprovalActionLog lastActionRecord;

    @Field("stepState")
    private ApprovalStepState stepState;

    @Field("resetCounter")
    private Integer resetCounter;
}
