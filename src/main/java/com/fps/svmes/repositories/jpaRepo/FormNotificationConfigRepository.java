package com.fps.svmes.repositories.jpaRepo;

import com.fps.svmes.models.sql.notification.FormNotificationConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FormNotificationConfigRepository extends JpaRepository<FormNotificationConfig, Long> {
    Optional<FormNotificationConfig> findByFormTemplateId(Long formTemplateId);
}
