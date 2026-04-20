package com.fps.svmes.services;

import com.fps.svmes.models.sql.production.SuggestedBatch;
import java.util.List;

public interface SuggestedBatchService {
    SuggestedBatch create(SuggestedBatch batch);
    SuggestedBatch update(SuggestedBatch batch);
    List<SuggestedBatch> findAll();
    SuggestedBatch findById(Long id);
    SuggestedBatch findByCode(String code);
    List<SuggestedBatch> findByStatus(Integer status);
    void softDelete(Long id);
}