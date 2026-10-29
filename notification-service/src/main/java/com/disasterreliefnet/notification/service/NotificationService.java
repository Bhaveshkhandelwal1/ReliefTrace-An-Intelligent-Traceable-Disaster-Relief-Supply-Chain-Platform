package com.disasterreliefnet.notification.service;

import com.disasterreliefnet.common.enums.NotificationChannel;
import com.disasterreliefnet.common.enums.NotificationStatus;
import com.disasterreliefnet.common.exception.ResourceNotFoundException;
import com.disasterreliefnet.notification.domain.NotificationLog;
import com.disasterreliefnet.notification.domain.NotificationPreference;
import com.disasterreliefnet.notification.dto.NotificationLogResponse;
import com.disasterreliefnet.notification.mapper.NotificationMapper;
import com.disasterreliefnet.notification.repository.NotificationLogRepository;
import com.disasterreliefnet.notification.sender.NotificationSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Core notification dispatch and logging engine.
 */
@Service
@Transactional(readOnly = true)
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationLogRepository logRepository;
    private final NotificationPreferenceService preferenceService;
    private final NotificationSender notificationSender;
    private final NotificationMapper notificationMapper;

    public NotificationService(
        NotificationLogRepository logRepository,
        NotificationPreferenceService preferenceService,
        NotificationSender notificationSender,
        NotificationMapper notificationMapper
    ) {
        this.logRepository = logRepository;
        this.preferenceService = preferenceService;
        this.notificationSender = notificationSender;
        this.notificationMapper = notificationMapper;
    }

    @Transactional
    public void dispatch(String userId, String subject, String body, UUID eventId, String eventType) {
        log.info("Dispatching notification to user: {} for event: {}", userId, eventType);

        NotificationPreference preference = preferenceService.getOrCreatePreference(userId);

        if (preference.isEmailEnabled()) {
            sendAndLog(userId, NotificationChannel.EMAIL, subject, body, eventId, eventType);
        }
        if (preference.isSmsEnabled()) {
            sendAndLog(userId, NotificationChannel.SMS, subject, body, eventId, eventType);
        }
        if (preference.isPushEnabled()) {
            sendAndLog(userId, NotificationChannel.PUSH, subject, body, eventId, eventType);
        }
    }

    private void sendAndLog(
        String userId,
        NotificationChannel channel,
        String subject,
        String body,
        UUID eventId,
        String eventType
    ) {
        boolean success = false;
        try {
            success = notificationSender.send(userId, channel, subject, body);
        } catch (Exception ex) {
            log.error("Failed to send notification via {}: {}", channel, ex.getMessage(), ex);
        }

        NotificationLog entry = new NotificationLog();
        entry.setUserId(userId);
        entry.setChannel(channel);
        entry.setSubject(subject);
        entry.setBody(body);
        entry.setEventId(eventId);
        entry.setEventType(eventType);
        entry.setStatus(success ? NotificationStatus.SENT : NotificationStatus.FAILED);
        entry.setSentAt(success ? Instant.now() : null);

        logRepository.save(entry);
    }

    public Page<NotificationLogResponse> getLogs(String userId, Pageable pageable) {
        if (userId != null && !userId.isBlank()) {
            return logRepository.findAllByUserId(userId, pageable)
                .map(notificationMapper::toLogResponse);
        }
        return logRepository.findAll(pageable)
            .map(notificationMapper::toLogResponse);
    }

    public NotificationLogResponse getLogById(UUID logId) {
        NotificationLog entry = logRepository.findByLogId(logId)
            .orElseThrow(() -> new ResourceNotFoundException("NotificationLog", logId));
        return notificationMapper.toLogResponse(entry);
    }
}
