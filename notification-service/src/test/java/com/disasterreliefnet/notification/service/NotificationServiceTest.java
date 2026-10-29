package com.disasterreliefnet.notification.service;

import com.disasterreliefnet.common.enums.NotificationChannel;
import com.disasterreliefnet.common.enums.NotificationStatus;
import com.disasterreliefnet.common.exception.ResourceNotFoundException;
import com.disasterreliefnet.notification.domain.NotificationLog;
import com.disasterreliefnet.notification.domain.NotificationPreference;
import com.disasterreliefnet.notification.dto.NotificationLogResponse;
import com.disasterreliefnet.notification.mapper.NotificationMapper;
import com.disasterreliefnet.notification.mapper.NotificationMapperImpl;
import com.disasterreliefnet.notification.repository.NotificationLogRepository;
import com.disasterreliefnet.notification.sender.NotificationSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationLogRepository logRepository;

    @Mock
    private NotificationPreferenceService preferenceService;

    @Mock
    private NotificationSender notificationSender;

    private final NotificationMapper notificationMapper = new NotificationMapperImpl();
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(
            logRepository,
            preferenceService,
            notificationSender,
            notificationMapper
        );
    }

    @Test
    @DisplayName("Should dispatch notification only to channels enabled in user preference")
    void dispatch_OnlyEmailEnabled() {
        String userId = "user-123";
        NotificationPreference preference = new NotificationPreference();
        preference.setUserId(userId);
        preference.setEmailEnabled(true);
        preference.setSmsEnabled(false);
        preference.setPushEnabled(false);

        when(preferenceService.getOrCreatePreference(userId)).thenReturn(preference);
        when(notificationSender.send(eq(userId), eq(NotificationChannel.EMAIL), any(), any())).thenReturn(true);

        UUID eventId = UUID.randomUUID();
        notificationService.dispatch(userId, "Alert", "Body content", eventId, "DonationRegistered");

        verify(notificationSender, times(1)).send(eq(userId), eq(NotificationChannel.EMAIL), eq("Alert"), eq("Body content"));
        verify(notificationSender, never()).send(eq(userId), eq(NotificationChannel.SMS), any(), any());
        verify(notificationSender, never()).send(eq(userId), eq(NotificationChannel.PUSH), any(), any());

        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(logRepository, times(1)).save(captor.capture());
        NotificationLog saved = captor.getValue();
        assertEquals(NotificationChannel.EMAIL, saved.getChannel());
        assertEquals(NotificationStatus.SENT, saved.getStatus());
        assertEquals(eventId, saved.getEventId());
    }

    @Test
    @DisplayName("Should dispatch to all channels when all enabled")
    void dispatch_AllChannelsEnabled() {
        String userId = "user-123";
        NotificationPreference preference = new NotificationPreference();
        preference.setUserId(userId);
        preference.setEmailEnabled(true);
        preference.setSmsEnabled(true);
        preference.setPushEnabled(true);

        when(preferenceService.getOrCreatePreference(userId)).thenReturn(preference);
        when(notificationSender.send(eq(userId), any(), any(), any())).thenReturn(true);

        notificationService.dispatch(userId, "Alert", "Body content", UUID.randomUUID(), "ShipmentAssigned");

        verify(notificationSender, times(3)).send(eq(userId), any(), eq("Alert"), eq("Body content"));
        verify(logRepository, times(3)).save(any(NotificationLog.class));
    }

    @Test
    @DisplayName("Should retrieve notification log by ID")
    void getLogById_Success() {
        UUID logId = UUID.randomUUID();
        NotificationLog entry = new NotificationLog();
        entry.setLogId(logId);
        entry.setUserId("user-1");
        entry.setChannel(NotificationChannel.EMAIL);
        entry.setSubject("Test Subject");
        entry.setBody("Test Body");
        entry.setStatus(NotificationStatus.SENT);

        when(logRepository.findByLogId(logId)).thenReturn(Optional.of(entry));

        NotificationLogResponse response = notificationService.getLogById(logId);

        assertNotNull(response);
        assertEquals(logId, response.logId());
        assertEquals("user-1", response.userId());
        assertEquals(NotificationChannel.EMAIL, response.channel());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when log not found")
    void getLogById_NotFound() {
        UUID logId = UUID.randomUUID();
        when(logRepository.findByLogId(logId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> notificationService.getLogById(logId));
    }

    @Test
    @DisplayName("Should retrieve paginated logs for a specific user")
    void getLogs_ByUser() {
        NotificationLog entry = new NotificationLog();
        entry.setLogId(UUID.randomUUID());
        entry.setUserId("user-1");

        Pageable pageable = PageRequest.of(0, 10);
        when(logRepository.findAllByUserId("user-1", pageable))
            .thenReturn(new PageImpl<>(List.of(entry), pageable, 1));

        Page<NotificationLogResponse> page = notificationService.getLogs("user-1", pageable);

        assertEquals(1, page.getTotalElements());
        verify(logRepository).findAllByUserId("user-1", pageable);
    }
}
