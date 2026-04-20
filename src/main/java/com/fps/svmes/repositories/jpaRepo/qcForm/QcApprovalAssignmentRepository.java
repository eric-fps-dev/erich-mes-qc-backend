package com.fps.svmes.repositories.jpaRepo.qcForm;

import com.fps.svmes.models.sql.qcForm.QcApprovalAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QcApprovalAssignmentRepository extends
        JpaRepository<QcApprovalAssignment, Long>,
        JpaSpecificationExecutor<QcApprovalAssignment> {
    Optional<QcApprovalAssignment> findBySubmissionId(String submissionId);
    void deleteBySubmissionId(String submissionId);
}
