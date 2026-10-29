package com.disasterreliefnet.common.event.payload;

import com.disasterreliefnet.common.dto.DeliveryAddressDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Event payload emitted when warehouse inventory is reserved (Topic: stock.reserved).
 */
public record StockReservedPayload(
    @NotNull UUID reservationId,
    @NotNull UUID allocationId,
    @NotNull UUID requestId,
    @NotNull UUID receiverId,
    @NotNull UUID itemId,
    @NotNull UUID warehouseId,
    @NotNull BigDecimal quantity,
    @NotBlank String unit,
    @NotNull DeliveryAddressDto pickupAddress,
    @NotNull DeliveryAddressDto deliveryAddress
) {}
