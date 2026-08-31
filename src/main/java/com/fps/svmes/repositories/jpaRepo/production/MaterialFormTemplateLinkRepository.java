package com.fps.svmes.repositories.jpaRepo.production;

import com.fps.svmes.models.sql.production.MaterialFormTemplateLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaterialFormTemplateLinkRepository extends JpaRepository<MaterialFormTemplateLink, Long> {
    List<MaterialFormTemplateLink> findByScopeAndMaterialIdAndStatus(String scope, Long materialId, Integer status);
    List<MaterialFormTemplateLink> findByMaterialIdAndStatus(Long materialId, Integer status);
    List<MaterialFormTemplateLink> findByScopeAndStatus(String scope, Integer status);
    List<MaterialFormTemplateLink> findByStatus(Integer status);
}
