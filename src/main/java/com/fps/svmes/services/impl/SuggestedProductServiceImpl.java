package com.fps.svmes.services.impl;

import com.fps.svmes.models.sql.production.SuggestedProduct;
import com.fps.svmes.repositories.jpaRepo.production.SuggestedProductRepository;
import com.fps.svmes.services.SuggestedProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SuggestedProductServiceImpl implements SuggestedProductService {

    private final SuggestedProductRepository repository;

    @Override
    public SuggestedProduct create(SuggestedProduct product) {
        product.setCreatedAt(OffsetDateTime.now());
        product.setStatus(1);
        return repository.save(product);
    }

    @Override
    public SuggestedProduct update(SuggestedProduct product) {
        product.setUpdatedAt(OffsetDateTime.now());
        return repository.save(product);
    }

    @Override
    public List<SuggestedProduct> findAll() {
        return repository.findAll();
    }

    @Override
    public SuggestedProduct findById(Long id) {
        return repository.findById(id).orElse(null);
    }

    @Override
    public SuggestedProduct findByCode(String code) {
        return repository.findByCode(code).orElse(null);
    }

    @Override
    public void softDelete(Long id) {
        repository.findById(id).ifPresent(product -> {
            product.setStatus(0);
            product.setUpdatedAt(OffsetDateTime.now());
            repository.save(product);
        });
    }

    @Override
    public List<SuggestedProduct> findByStatus(Integer status) {
        return repository.findByStatus(status);
    }

}
