package com.fps.svmes.services;

import com.fps.svmes.models.sql.production.MaterialLotSuggestedBatchMapping;

import java.util.List;

public interface MaterialLotSuggestedBatchMappingService {
    MaterialLotSuggestedBatchMapping create(MaterialLotSuggestedBatchMapping mapping);
    MaterialLotSuggestedBatchMapping update(Long id, MaterialLotSuggestedBatchMapping mapping);
    /** Active (status = 1) mappings only. */
    List<MaterialLotSuggestedBatchMapping> findAll();
    MaterialLotSuggestedBatchMapping findById(Long id);

    /** Active (status = 1) mapping only. */
    MaterialLotSuggestedBatchMapping findByMaterialLotNumber(String materialLotNumber);

    /** Soft delete: sets status = 0. */
    void delete(Long id);
}
