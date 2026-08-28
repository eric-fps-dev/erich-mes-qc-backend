package com.fps.svmes.repositories.jpaRepo.production;

import com.fps.svmes.models.sql.production.MaterialSuggestedProductMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MaterialSuggestedProductMappingRepository extends JpaRepository<MaterialSuggestedProductMapping, Long> {
    Optional<MaterialSuggestedProductMapping> findByMaterialIdAndStatus(Long materialId, Integer status);
    List<MaterialSuggestedProductMapping> findByStatus(Integer status);
}
