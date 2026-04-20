package com.fps.svmes.repositories.jpaRepo.subscription;

import com.fps.svmes.models.sql.subscription.WeeklyReportSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WeeklyReportSubscriptionRepository extends JpaRepository<WeeklyReportSubscription, Integer> {

    List<WeeklyReportSubscription> findByStatusAndIsActive(Integer status, Boolean isActive);

    Optional<WeeklyReportSubscription> findByEmailAndStatus(String email, Integer status);

    boolean existsByEmailAndStatus(String email, Integer status);

    @Query("SELECT s FROM WeeklyReportSubscription s WHERE s.status = 1 AND s.isActive = true")
    List<WeeklyReportSubscription> findAllActiveSubscriptions();
}
