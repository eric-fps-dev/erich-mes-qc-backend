package com.fps.svmes.repositories.jpaRepo.qcForm;

import com.fps.svmes.models.sql.qcForm.QcSnapshotSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QcSnapshotSubmissionRepository extends JpaRepository<QcSnapshotSubmission, Long> {
    
    QcSnapshotSubmission findBySubmissionId(String submissionId);

    @Modifying
    @Query("DELETE FROM QcSnapshotSubmission q WHERE q.submissionId = :submissionId")
    void deleteBySubmissionId(@Param("submissionId") String submissionId);

    @Modifying
    @Query("DELETE FROM QcSnapshotSubmission q WHERE q.submissionId IN :submissionIds")
    void deleteBySubmissionIds(@Param("submissionIds") List<String> submissionIds);
}
