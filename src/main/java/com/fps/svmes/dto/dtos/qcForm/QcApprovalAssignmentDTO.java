package com.fps.svmes.dto.dtos.qcForm;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class QcApprovalAssignmentDTO {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("submission_id")
    private String submissionId;

    @JsonProperty("qc_form_template_id")
    private Long qcFormTemplateId;

    @JsonProperty("qc_form_template_name")
    private String qcFormTemplateName;

    @JsonProperty("mongo_collection")
    private String mongoCollection;

    @JsonProperty("approval_type")
    private String approvalType;

    @JsonProperty("state")
    private String state;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    @JsonProperty("updated_at")
    private OffsetDateTime updatedAt;
}
