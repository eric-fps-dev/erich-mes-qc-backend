package com.fps.svmes.models.sql.qcForm;

import jakarta.persistence.*;
import lombok.Data;
import java.time.OffsetDateTime;

@Entity
@Table(name = "qc_snapshot_submission", schema = "quality_management")
@Data
public class QcSnapshotSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "snapshot_id")
    private Long snapshotId;

    @Column(name = "submission_id")
    private String submissionId;

    @Column(name = "collection_name")
    private String collectionName;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "has_abnormal")
    private Boolean hasAbnormal;

    @Column(name = "abnormal_field_count")
    private Integer abnormalFieldCount;
}
