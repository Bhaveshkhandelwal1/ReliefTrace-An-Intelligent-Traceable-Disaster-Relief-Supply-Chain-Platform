package com.disasterreliefnet.disaster.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Response payload representing a situational update for a disaster.
 */
public record DisasterUpdateResponse(
    UUID updateId,
    UUID disasterId,
    String title,
    String content,
    String source,
    String postedBy,
    Instant createdAt
) {}
