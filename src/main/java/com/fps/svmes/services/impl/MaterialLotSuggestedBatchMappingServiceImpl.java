package com.fps.svmes.services.impl;

import com.fps.svmes.models.sql.production.MaterialLotSuggestedBatchMapping;
import com.fps.svmes.repositories.jpaRepo.production.MaterialLotSuggestedBatchMappingRepository;
import com.fps.svmes.services.MaterialLotSuggestedBatchMappingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MaterialLotSuggestedBatchMappingServiceImpl implements MaterialLotSuggestedBatchMappingService {

    private final MaterialLotSuggestedBatchMappingRepository repository;

    @Override
    public MaterialLotSuggestedBatchMapping create(MaterialLotSuggestedBatchMapping mapping) {
        // Ignore any client-supplied id — otherwise repository.save() treats this as an
        // update and can silently overwrite an unrelated existing row.
        mapping.setId(null);
        mapping.setCreationDetails(mapping.getCreatedBy(), 1);
        return repository.save(mapping);
    }

    @Override
    public MaterialLotSuggestedBatchMapping update(Long id, MaterialLotSuggestedBatchMapping mapping) {
        MaterialLotSuggestedBatchMapping existing = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Material lot suggested batch mapping not found: " + id));
        if (mapping.getMaterialLotNumber() != null) {
            existing.setMaterialLotNumber(mapping.getMaterialLotNumber());
        }
        if (mapping.getSuggestedBatchId() != null) {
            existing.setSuggestedBatchId(mapping.getSuggestedBatchId());
        }
        existing.setUpdateDetails(mapping.getUpdatedBy(), existing.getStatus());
        return repository.save(existing);
    }

    @Override
    public List<MaterialLotSuggestedBatchMapping> findAll() {
        return repository.findByStatus(1);
    }

    @Override
    public MaterialLotSuggestedBatchMapping findById(Long id) {
        return repository.findById(id).orElse(null);
    }

    @Override
    public MaterialLotSuggestedBatchMapping findByMaterialLotNumber(String materialLotNumber) {
        return repository.findByMaterialLotNumberAndStatus(materialLotNumber, 1).orElse(null);
    }

    @Override
    public void delete(Long id) {
        repository.findById(id).ifPresent(existing -> {
            existing.setUpdateDetails(existing.getUpdatedBy(), 0);
            repository.save(existing);
        });
    }
}
