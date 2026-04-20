package com.fps.svmes.repositories.jpaRepo.alert;

import com.fps.svmes.models.sql.alert.AlertStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AlertStatusRepository extends JpaRepository<AlertStatus, Integer> {
}
