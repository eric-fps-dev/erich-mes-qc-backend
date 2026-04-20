package com.fps.svmes.repositories.jpaRepo.alert;

import com.fps.svmes.models.sql.alert.AlertRecord;
import com.fps.svmes.repositories.projection.alert.IdCountProjection;
import com.fps.svmes.repositories.projection.alert.InspectionCountProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertRecordRepository extends JpaRepository<AlertRecord, Long>, JpaSpecificationExecutor<AlertRecord> {
    List<AlertRecord> findByStatus(Integer status);
    void deleteBySubmissionId(String submissionId);
    void deleteBySubmissionIdIn(List<String> submissionIds);

    @Query(value = """
        select ar.alert_status as id, count(*) as cnt
        from quality_management.qc_alert_record ar
        where ar.status = :status and ar.alert_status is not null
        group by ar.alert_status
        """, nativeQuery = true)
    List<IdCountProjection> countByAlertStatus(@Param("status") int status);

    @Query(value = """
        select ar.risk_level_id as id, count(*) as cnt
        from quality_management.qc_alert_record ar
        where ar.status = :status and ar.risk_level_id is not null
        group by ar.risk_level_id
        """, nativeQuery = true)
    List<IdCountProjection> countByRiskLevel(@Param("status") int status);

    @Query(value = """
        select ar.inspection_item_key as "key",
               max(ar.inspection_item_label) as label,
               count(*) as cnt
        from quality_management.qc_alert_record ar
        where ar.status = :status and ar.inspection_item_key is not null
        group by ar.inspection_item_key
        """, nativeQuery = true)
    List<InspectionCountProjection> countByInspectionItem(@Param("status") int status);
}

