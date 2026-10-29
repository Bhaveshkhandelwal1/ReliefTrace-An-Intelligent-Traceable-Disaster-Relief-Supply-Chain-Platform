package com.disasterreliefnet.notification.service;

import com.disasterreliefnet.notification.domain.NotificationPreference;
import com.disasterreliefnet.notification.dto.NotificationPreferenceResponse;
import com.disasterreliefnet.notification.dto.UpdatePreferenceRequest;
import com.disasterreliefnet.notification.mapper.NotificationMapper;
import com.disasterreliefnet.notification.repository.NotificationPreferenceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Service managing user notification preferences across communication channels.
 */
@Service
@Transactional(readOnly = true)
public class NotificationPreferenceService {

    private static final Logger log = LoggerFactory.getLogger(NotificationPreferenceService.class);

    private final NotificationPreferenceRepository preferenceRepository;
    private final NotificationMapper notificationMapper;

    public NotificationPreferenceService(
        NotificationPreferenceRepository preferenceRepository,
        NotificationMapper notificationMapper
    ) {
        this.preferenceRepository = preferenceRepository;
        this.notificationMapper = notificationMapper;
    }

    public NotificationPreference getOrCreatePreference(String userId) {
        return preferenceRepository.findByUserId(userId)
            .orElseGet(() -> {
                NotificationPreference preference = new NotificationPreference();
                preference.setUserId(userId);
                preference.setEmailEnabled(true);
                preference.setSmsEnabled(false);
                preference.setPushEnabled(false);
                preference.setUpdatedAt(Instant.now());
                return preference;
            });
    }

    public NotificationPreferenceResponse getPreferences(String userId) {
        return notificationMapper.toPreferenceResponse(getOrCreatePreference(userId));
    }

    @Transactional
    public NotificationPreferenceResponse updatePreferences(String userId, UpdatePreferenceRequest request) {
        log.info("Updating notification preferences for user ID: {}", userId);

        NotificationPreference preference = preferenceRepository.findByUserId(userId)
            .orElseGet(() -> {
                NotificationPreference p = new NotificationPreference();
                p.setUserId(userId);
                return p;
            });

        preference.setEmailEnabled(request.emailEnabled());
        preference.setSmsEnabled(request.smsEnabled());
        preference.setPushEnabled(request.pushEnabled());
        preference.setUpdatedAt(Instant.now());

        NotificationPreference saved = preferenceRepository.save(preference);
        return notificationMapper.toPreferenceResponse(saved);
    }
}
