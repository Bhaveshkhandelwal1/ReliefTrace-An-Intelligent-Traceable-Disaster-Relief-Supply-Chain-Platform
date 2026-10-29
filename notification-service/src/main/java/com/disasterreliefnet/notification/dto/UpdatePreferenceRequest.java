package com.disasterreliefnet.notification.dto;

/**
 * Request DTO for updating user notification preferences.
 */
public record UpdatePreferenceRequest(
    boolean emailEnabled,
    boolean smsEnabled,
    boolean pushEnabled
) {}
