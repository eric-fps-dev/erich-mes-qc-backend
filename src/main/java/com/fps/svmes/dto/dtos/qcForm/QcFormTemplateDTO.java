package com.fps.svmes.dto.dtos.qcForm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fps.svmes.dto.dtos.CommonDTO;
import jakarta.persistence.Id;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL) // Ignore null fields in serialization
public class QcFormTemplateDTO extends CommonDTO {
    @Id
    @JsonProperty("id")
    private Long id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("form_template_json")
    private String formTemplateJson;

    @JsonProperty("approval_type")
    private String approvalType;

    @JsonProperty("approval_template_id")
    private String approvalTemplateId;

    @JsonProperty("has_edit_history")
    private Boolean hasEditHistory;
}
