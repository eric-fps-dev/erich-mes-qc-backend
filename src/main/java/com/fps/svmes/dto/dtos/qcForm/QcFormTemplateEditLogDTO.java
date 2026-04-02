package com.fps.svmes.dto.dtos.qcForm;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class QcFormTemplateEditLogDTO {
    private Long id;
    private Long templateId;
    private String editorName;
    private OffsetDateTime editedAt;
    private String changeSummary;
}
