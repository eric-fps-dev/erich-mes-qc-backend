package com.fps.svmes.services.impl;

import com.fps.svmes.models.sql.production.MaterialSuggestedProductMapping;
import com.fps.svmes.repositories.jpaRepo.production.MaterialSuggestedProductMappingRepository;
import com.fps.svmes.services.MaterialSuggestedProductMappingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MaterialSuggestedProductMappingServiceImpl implements MaterialSuggestedProductMappingService {

    private final MaterialSuggestedProductMappingRepository repository;

    @Override
    public MaterialSuggestedProductMapping create(MaterialSuggestedProductMapping mapping) {
        // Ignore any client-supplied id — otherwise repository.save() treats this as an
        // update and can silently overwrite an unrelated existing row.
        mapping.setId(null);
        mapping.setCreationDetails(mapping.getCreatedBy(), 1);
        return repository.save(mapping);
    }

    @Override
    public MaterialSuggestedProductMapping update(Long id, MaterialSuggestedProductMapping mapping) {
        MaterialSuggestedProductMapping existing = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Material suggested product mapping not found: " + id));
        if (mapping.getMaterialId() != null) {
            existing.setMaterialId(mapping.getMaterialId());
        }
        if (mapping.getMaterialName() != null) {
            existing.setMaterialName(mapping.getMaterialName());
        }
        if (mapping.getSuggestedProductId() != null) {
            existing.setSuggestedProductId(mapping.getSuggestedProductId());
        }
        existing.setUpdateDetails(mapping.getUpdatedBy(), existing.getStatus());
        return repository.save(existing);
    }

    @Override
    public List<MaterialSuggestedProductMapping> findAll() {
        return repository.findByStatus(1);
    }

    @Override
    public MaterialSuggestedProductMapping findById(Long id) {
        return repository.findById(id).orElse(null);
    }

    @Override
    public MaterialSuggestedProductMapping findByMaterialId(Long materialId) {
        return repository.findByMaterialIdAndStatus(materialId, 1).orElse(null);
    }

    @Override
    public void delete(Long id) {
        repository.findById(id).ifPresent(existing -> {
            existing.setUpdateDetails(existing.getUpdatedBy(), 0);
            repository.save(existing);
        });
    }
}
