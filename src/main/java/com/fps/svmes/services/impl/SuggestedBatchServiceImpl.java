package com.fps.svmes.services.impl;

import com.fps.svmes.models.sql.production.SuggestedBatch;
import com.fps.svmes.models.sql.production.SuggestedProduct;
import com.fps.svmes.repositories.jpaRepo.production.SuggestedBatchRepository;
import com.fps.svmes.services.SuggestedBatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SuggestedBatchServiceImpl implements SuggestedBatchService {

    private final SuggestedBatchRepository repository;

    @Override
    public SuggestedBatch create(SuggestedBatch batch) {
        batch.setCreatedAt(OffsetDateTime.now());
        batch.setStatus(1);
        return repository.save(batch);
    }

    @Override
    public SuggestedBatch update(SuggestedBatch batch) {
        batch.setUpdatedAt(OffsetDateTime.now());
        return repository.save(batch);
    }

    @Override
    public List<SuggestedBatch> findAll() {
        return repository.findAll();
    }

    @Override
    public List<SuggestedBatch> findByStatus(Integer status) {
        return repository.findByStatus(status);
    }

    @Override
    public SuggestedBatch findById(Long id) {
        return repository.findById(id).orElse(null);
    }

    @Override
    public SuggestedBatch findByCode(String code) {
        return repository.findByCode(code).orElse(null);
    }

    @Override
    public void softDelete(Long id) {
        repository.findById(id).ifPresent(batch -> {
            batch.setStatus(0);
            batch.setUpdatedAt(OffsetDateTime.now());
            repository.save(batch);
        });
    }
}