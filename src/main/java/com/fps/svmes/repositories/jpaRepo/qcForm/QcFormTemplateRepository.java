package com.fps.svmes.repositories.jpaRepo.qcForm;

import com.fps.svmes.models.sql.qcForm.QcFormTemplate;
import feign.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QcFormTemplateRepository extends JpaRepository<QcFormTemplate, Long> {
    List<QcFormTemplate> findAllByStatus(Integer status);

    @Query("SELECT f.formTemplateJson FROM QcFormTemplate f WHERE f.id = :formId")
    String findFormTemplateJsonById(@Param("formId") Long formId);

    Optional<QcFormTemplate> findById(Long id);

    @Query("SELECT q.approvalType FROM QcFormTemplate q WHERE q.id = :id")
    String findApprovalTypeById(@Param("id") Long id);
}
