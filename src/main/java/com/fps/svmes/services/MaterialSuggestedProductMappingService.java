package com.fps.svmes.services;

import com.fps.svmes.models.sql.production.MaterialSuggestedProductMapping;

import java.util.List;

public interface MaterialSuggestedProductMappingService {
    MaterialSuggestedProductMapping create(MaterialSuggestedProductMapping mapping);
    MaterialSuggestedProductMapping update(Long id, MaterialSuggestedProductMapping mapping);
    /** Active (status = 1) mappings only. */
    List<MaterialSuggestedProductMapping> findAll();
    MaterialSuggestedProductMapping findById(Long id);

    /** Active (status = 1) mapping only. */
    MaterialSuggestedProductMapping findByMaterialId(Long materialId);

    /** Soft delete: sets status = 0. */
    void delete(Long id);
}
