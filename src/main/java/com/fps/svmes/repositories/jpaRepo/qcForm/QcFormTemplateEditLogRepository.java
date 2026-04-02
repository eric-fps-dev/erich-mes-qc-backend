package com.fps.svmes.repositories.jpaRepo.qcForm;

import com.fps.svmes.models.sql.qcForm.QcFormTemplateEditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QcFormTemplateEditLogRepository extends JpaRepository<QcFormTemplateEditLog, Long> {
    List<QcFormTemplateEditLog> findByTemplateIdOrderByEditedAtDesc(Long templateId);
    long countByTemplateId(Long templateId);
}
