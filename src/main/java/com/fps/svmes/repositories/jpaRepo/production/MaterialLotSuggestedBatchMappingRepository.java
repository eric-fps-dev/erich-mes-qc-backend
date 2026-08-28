package com.fps.svmes.repositories.jpaRepo.production;

import com.fps.svmes.models.sql.production.MaterialLotSuggestedBatchMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MaterialLotSuggestedBatchMappingRepository extends JpaRepository<MaterialLotSuggestedBatchMapping, Long> {
    Optional<MaterialLotSuggestedBatchMapping> findByMaterialLotNumberAndStatus(String materialLotNumber, Integer status);
    List<MaterialLotSuggestedBatchMapping> findByStatus(Integer status);
}
