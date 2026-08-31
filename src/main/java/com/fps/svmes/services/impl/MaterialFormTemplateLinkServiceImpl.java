package com.fps.svmes.services.impl;

import com.fps.svmes.models.sql.production.MaterialFormTemplateLink;
import com.fps.svmes.repositories.jpaRepo.production.MaterialFormTemplateLinkRepository;
import com.fps.svmes.services.MaterialFormTemplateLinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MaterialFormTemplateLinkServiceImpl implements MaterialFormTemplateLinkService {

    private final MaterialFormTemplateLinkRepository repository;

    @Override
    public MaterialFormTemplateLink create(MaterialFormTemplateLink link) {
        // Ignore any client-supplied id — otherwise repository.save() treats this as an
        // update and can silently overwrite an unrelated existing row.
        link.setId(null);
        if (link.getActive() == null) {
            link.setActive(Boolean.TRUE);
        }
        link.setCreationDetails(link.getCreatedBy(), 1);
        return repository.save(link);
    }

    @Override
    public MaterialFormTemplateLink update(Long id, MaterialFormTemplateLink link) {
        MaterialFormTemplateLink existing = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Material form template link not found: " + id));
        if (link.getScope() != null) {
            existing.setScope(link.getScope());
        }
        if (link.getMaterialId() != null) {
            existing.setMaterialId(link.getMaterialId());
        }
        if (link.getMaterialName() != null) {
            existing.setMaterialName(link.getMaterialName());
        }
        if (link.getDefaultQcTemplateId() != null) {
            existing.setDefaultQcTemplateId(link.getDefaultQcTemplateId());
        }
        if (link.getDefaultQcTemplateName() != null) {
            existing.setDefaultQcTemplateName(link.getDefaultQcTemplateName());
        }
        if (link.getActive() != null) {
            existing.setActive(link.getActive());
        }
        existing.setUpdateDetails(link.getUpdatedBy(), existing.getStatus());
        return repository.save(existing);
    }

    @Override
    public List<MaterialFormTemplateLink> find(String scope, Long materialId) {
        boolean hasScope = scope != null && !scope.isBlank();
        if (hasScope && materialId != null) {
            return repository.findByScopeAndMaterialIdAndStatus(scope, materialId, 1);
        }
        if (materialId != null) {
            return repository.findByMaterialIdAndStatus(materialId, 1);
        }
        if (hasScope) {
            return repository.findByScopeAndStatus(scope, 1);
        }
        return repository.findByStatus(1);
    }

    @Override
    public MaterialFormTemplateLink findById(Long id) {
        return repository.findById(id).orElse(null);
    }

    @Override
    public void delete(Long id) {
        repository.findById(id).ifPresent(existing -> {
            existing.setUpdateDetails(existing.getUpdatedBy(), 0);
            repository.save(existing);
        });
    }
}
