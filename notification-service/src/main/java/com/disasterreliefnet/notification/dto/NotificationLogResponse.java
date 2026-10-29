package com.disasterreliefnet.notification.dto;

import com.disasterreliefnet.common.enums.NotificationChannel;
import com.disasterreliefnet.common.enums.NotificationStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO representing an audit log entry for a dispatched notification.
 */
public record NotificationLogResponse(
    UUID logId,
    String userId,
    NotificationChannel channel,
    String subject,
    String body,
    NotificationStatus status,
    Instant sentAt,
    Instant createdAt
) {}
