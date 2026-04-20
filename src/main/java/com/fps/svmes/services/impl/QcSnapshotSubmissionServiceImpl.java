package com.fps.svmes.services.impl;

import com.fps.svmes.models.sql.qcForm.QcSnapshotSubmission;
import com.fps.svmes.repositories.jpaRepo.qcForm.QcSnapshotSubmissionRepository;
import com.fps.svmes.services.QcSnapshotSubmissionService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class QcSnapshotSubmissionServiceImpl implements QcSnapshotSubmissionService {

    @Autowired
    private QcSnapshotSubmissionRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public void updateSubmissionId(String oldSubmissionId, String newSubmissionId) {
        QcSnapshotSubmission submission = repository.findBySubmissionId(oldSubmissionId);
        if (submission != null) {
            submission.setSubmissionId(newSubmissionId);
            repository.save(submission);
            log.info("Updated QcSnapshotSubmission from {} to {}", oldSubmissionId, newSubmissionId);
        } else {
            log.warn("QcSnapshotSubmission not found for submissionId: {}", oldSubmissionId);
        }
    }

    @Override
    @Transactional("transactionManager")
    public void deleteBySubmissionId(String submissionId) {
        // Delete from child tables (Scenario C)
        deleteFromChildTables(submissionId);

        // Delete the link
        repository.deleteBySubmissionId(submissionId);
        log.info("Deleted QcSnapshotSubmission and children for submissionId: {}", submissionId);
    }

    @Override
    @Transactional("transactionManager")
    public void deleteBySubmissionIds(List<String> submissionIds) {
        if (submissionIds != null && !submissionIds.isEmpty()) {
            for (String submissionId : submissionIds) {
                deleteFromChildTables(submissionId);
            }
            repository.deleteBySubmissionIds(submissionIds);
            log.info("Deleted QcSnapshotSubmissions and children for submissionIds: {}", submissionIds);
        }
    }

    private void deleteFromChildTables(String submissionId) {
        String[] tables = {
            "qc_snapshot_item",
            "qc_snapshot_batch",
            "qc_snapshot_product",
            "qc_snapshot_shift",
            "qc_snapshot_team",
            "qc_snapshot_inspector",
            "qc_snapshot_retest"
        };

        for (String table : tables) {
            String sql = "DELETE FROM quality_management." + table + " WHERE submission_id = :submissionId";
            entityManager.createNativeQuery(sql)
                    .setParameter("submissionId", submissionId)
                    .executeUpdate();
        }
    }
}
