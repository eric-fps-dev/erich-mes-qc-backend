package com.fps.svmes.repositories.jpaRepo.production;

import com.fps.svmes.models.sql.production.SuggestedBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SuggestedBatchRepository extends JpaRepository<SuggestedBatch, Long> {
    Optional<SuggestedBatch> findByCode(String code);
    List<SuggestedBatch> findByStatus(Integer status);
}
