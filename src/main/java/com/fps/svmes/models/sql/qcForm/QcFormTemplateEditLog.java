package com.fps.svmes.models.sql.qcForm;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "qc_form_template_edit_log", schema = "quality_management")
@Data
@NoArgsConstructor
public class QcFormTemplateEditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template_id", nullable = false)
    private Long templateId;

    @Column(name = "edited_by", nullable = false)
    private Long editedBy;

    @Column(name = "edited_at", nullable = false)
    private OffsetDateTime editedAt;

    @Column(name = "change_summary", length = 500)
    private String changeSummary;
}
