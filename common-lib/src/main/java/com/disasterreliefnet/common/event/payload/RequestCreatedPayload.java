package com.disasterreliefnet.common.event.payload;

import com.disasterreliefnet.common.dto.DeliveryAddressDto;
import com.disasterreliefnet.common.enums.AidCategory;
import com.disasterreliefnet.common.enums.UrgencyLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Event payload emitted when an aid request is submitted by a receiver (Topic: request.created).
 */
public record RequestCreatedPayload(
    @NotNull UUID requestId,
    @NotNull UUID receiverId,
    @NotNull UUID disasterId,
    @NotNull AidCategory category,
    @NotBlank String description,
    @NotNull BigDecimal quantityRequested,
    @NotBlank String unit,
    @NotNull UrgencyLevel urgencyLevel,
    @NotNull DeliveryAddressDto deliveryAddress,
    @NotBlank String receiverEmail,
    @NotBlank String receiverName
) {}
