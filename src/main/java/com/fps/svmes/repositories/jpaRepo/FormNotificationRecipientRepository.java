package com.fps.svmes.repositories.jpaRepo;

import com.fps.svmes.models.sql.notification.FormNotificationRecipient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FormNotificationRecipientRepository extends JpaRepository<FormNotificationRecipient, Long> {
    List<FormNotificationRecipient> findByConfigIdAndStatus(Long configId, Integer status);
    void deleteByConfigId(Long configId);
}
