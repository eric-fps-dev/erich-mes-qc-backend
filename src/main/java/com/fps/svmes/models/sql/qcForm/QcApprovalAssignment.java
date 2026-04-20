package com.fps.svmes.models.sql.qcForm;

import jakarta.persistence.*;
import lombok.Data;
import java.time.OffsetDateTime;

@Entity
@Table(name = "qc_approval_assignment", schema = "quality_management")
@Data
public class QcApprovalAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "submission_id")
    private String submissionId;

    @Column(name = "qc_form_template_id")
    private Long qcFormTemplateId;

    @Column(name = "qc_form_template_name")
    private String qcFormTemplateName;

    @Column(name = "mongo_collection")
    private String mongoCollection;

    @Column(name = "approval_type")
    private String approvalType;

    @Column(name = "state")
    private String state;

    @Column(name = "created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt = OffsetDateTime.now();
}