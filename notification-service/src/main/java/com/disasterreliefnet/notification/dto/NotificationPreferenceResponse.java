package com.disasterreliefnet.notification.dto;

import java.time.Instant;

/**
 * Response DTO for a user's notification channel preferences.
 */
public record NotificationPreferenceResponse(
    String userId,
    boolean emailEnabled,
    boolean smsEnabled,
    boolean pushEnabled,
    Instant updatedAt
) {}
