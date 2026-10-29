package com.disasterreliefnet.notification.repository;

import com.disasterreliefnet.common.enums.NotificationStatus;
import com.disasterreliefnet.notification.domain.NotificationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link NotificationLog} entities.
 */
public interface NotificationLogRepository extends JpaRepository<NotificationLog, UUID> {

    Optional<NotificationLog> findByLogId(UUID logId);

    Page<NotificationLog> findAllByUserId(String userId, Pageable pageable);

    Page<NotificationLog> findAllByStatus(NotificationStatus status, Pageable pageable);
}
