package com.fps.svmes.services;

import com.fps.svmes.models.sql.production.SuggestedProduct;
import java.util.List;

public interface SuggestedProductService {
    SuggestedProduct create(SuggestedProduct product);
    SuggestedProduct update(SuggestedProduct product);
    List<SuggestedProduct> findAll();
    SuggestedProduct findById(Long id);
    SuggestedProduct findByCode(String code);
    List<SuggestedProduct> findByStatus(Integer status);
    void softDelete(Long id);
}