package com.disasterreliefnet.notification.service;

import com.disasterreliefnet.notification.domain.NotificationPreference;
import com.disasterreliefnet.notification.dto.NotificationPreferenceResponse;
import com.disasterreliefnet.notification.dto.UpdatePreferenceRequest;
import com.disasterreliefnet.notification.mapper.NotificationMapper;
import com.disasterreliefnet.notification.mapper.NotificationMapperImpl;
import com.disasterreliefnet.notification.repository.NotificationPreferenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationPreferenceServiceTest {

    @Mock
    private NotificationPreferenceRepository preferenceRepository;

    private final NotificationMapper notificationMapper = new NotificationMapperImpl();
    private NotificationPreferenceService preferenceService;

    @BeforeEach
    void setUp() {
        preferenceService = new NotificationPreferenceService(preferenceRepository, notificationMapper);
    }

    @Test
    @DisplayName("Should return default preferences (email enabled, sms/push disabled) when not found")
    void getOrCreatePreference_DefaultValues() {
        when(preferenceRepository.findByUserId("user-1")).thenReturn(Optional.empty());

        NotificationPreference preference = preferenceService.getOrCreatePreference("user-1");

        assertNotNull(preference);
        assertEquals("user-1", preference.getUserId());
        assertTrue(preference.isEmailEnabled());
        assertFalse(preference.isSmsEnabled());
        assertFalse(preference.isPushEnabled());
    }

    @Test
    @DisplayName("Should update user notification channel preferences")
    void updatePreferences_Success() {
        NotificationPreference existing = new NotificationPreference();
        existing.setUserId("user-1");
        existing.setEmailEnabled(true);
        existing.setSmsEnabled(false);
        existing.setPushEnabled(false);

        when(preferenceRepository.findByUserId("user-1")).thenReturn(Optional.of(existing));
        when(preferenceRepository.save(any(NotificationPreference.class))).thenAnswer(i -> i.getArgument(0));

        UpdatePreferenceRequest request = new UpdatePreferenceRequest(false, true, true);
        NotificationPreferenceResponse response = preferenceService.updatePreferences("user-1", request);

        assertNotNull(response);
        assertFalse(response.emailEnabled());
        assertTrue(response.smsEnabled());
        assertTrue(response.pushEnabled());
        verify(preferenceRepository).save(existing);
    }
}
