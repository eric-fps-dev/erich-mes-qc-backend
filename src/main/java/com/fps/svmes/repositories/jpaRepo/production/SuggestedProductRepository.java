package com.fps.svmes.repositories.jpaRepo.production;

import com.fps.svmes.models.sql.production.SuggestedProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SuggestedProductRepository extends JpaRepository<SuggestedProduct, Long> {
    Optional<SuggestedProduct> findByCode(String code);
    List<SuggestedProduct> findByStatus(Integer status);
}
