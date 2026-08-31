package com.fps.svmes.repositories.jpaRepo.production;

import com.fps.svmes.models.sql.production.MaterialQcRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MaterialQcRequestRepository extends JpaRepository<MaterialQcRequest, Long> {

    @Query("SELECT r FROM MaterialQcRequest r WHERE "
            + "(:materialId IS NULL OR r.materialId = :materialId) AND "
            + "(:materialLotNumber IS NULL OR r.materialLotNumber = :materialLotNumber) AND "
            + "(:formTemplateId IS NULL OR r.formTemplateId = :formTemplateId)")
    Page<MaterialQcRequest> findByFilters(
            @Param("materialId") Long materialId,
            @Param("materialLotNumber") String materialLotNumber,
            @Param("formTemplateId") Long formTemplateId,
            Pageable pageable);
}
