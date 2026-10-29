package com.disasterreliefnet.receiver.dto;

import com.disasterreliefnet.common.dto.DeliveryAddressDto;
import com.disasterreliefnet.common.enums.AidCategory;
import com.disasterreliefnet.common.enums.RequestStatus;
import com.disasterreliefnet.common.enums.UrgencyLevel;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * API response representing an aid request.
 */
public record AidRequestResponse(
    UUID requestId,
    UUID receiverId,
    UUID disasterId,
    AidCategory category,
    String description,
    BigDecimal quantityRequested,
    String unit,
    UrgencyLevel urgencyLevel,
    DeliveryAddressDto deliveryAddress,
    RequestStatus status,
    UUID allocationId,
    Instant createdAt,
    Instant updatedAt
) {}
