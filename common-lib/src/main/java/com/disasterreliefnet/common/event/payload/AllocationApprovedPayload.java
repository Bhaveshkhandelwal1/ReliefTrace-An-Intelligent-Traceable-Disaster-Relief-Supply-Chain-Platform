package com.disasterreliefnet.common.event.payload;

import com.disasterreliefnet.common.dto.DeliveryAddressDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Event payload emitted when a coordinator approves an inventory allocation (Topic: allocation.approved).
 */
public record AllocationApprovedPayload(
    @NotNull UUID allocationId,
    @NotNull UUID requestId,
    @NotNull UUID receiverId,
    @NotNull UUID disasterId,
    @NotNull UUID itemId,
    @NotNull UUID warehouseId,
    @NotNull BigDecimal quantity,
    @NotBlank String unit,
    @NotBlank String approvedBy,
    @NotNull DeliveryAddressDto deliveryAddress
) {}
