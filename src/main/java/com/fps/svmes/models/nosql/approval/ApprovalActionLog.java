package com.fps.svmes.models.nosql.approval;

import com.fps.svmes.enums.approval.ApprovalAction;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Data
public class ApprovalActionLog {
    @Field("logId")
    private String logId;

    @Field("action")
    private ApprovalAction action;

    @Field("stepSequence")
    private Integer stepSequence;

    @Field("comments")
    private String comments;

    @Field("eSignature")
    private String eSignature;

    @Field("suggestRetest")
    private Boolean suggestRetest;

    @Field("actorUserId")
    private String actorUserId;

    @Field("actorUserName")
    private String actorUserName;

    @Field("actorRoleId")
    private String actorRoleId;

    @Field("actorRoleName")
    private String actorRoleName;

    @Field("formSubmissionSnapshot")
    private Object formSubmissionSnapshot;

    @Field("formTemplateSnapshot")
    private Object formTemplateSnapshot;

    @Field("actedAt")
    private Instant actedAt;
}
