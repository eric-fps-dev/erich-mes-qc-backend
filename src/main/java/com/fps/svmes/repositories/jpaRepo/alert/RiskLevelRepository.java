package com.fps.svmes.repositories.jpaRepo.alert;

import com.fps.svmes.models.sql.alert.RiskLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RiskLevelRepository extends JpaRepository<RiskLevel, Integer> {

}
