package com.fps.svmes.models.sql.qcForm;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fps.svmes.converters.JsonNodeConverter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fps.svmes.models.sql.Common;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "qc_form_template", schema = "quality_management")
@Data
public class QcFormTemplate extends Common {

    @Id
    @JsonProperty("id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "form_template_json")
    private String formTemplateJson;

    @JsonProperty("approval_type")
    private String approvalType;

}