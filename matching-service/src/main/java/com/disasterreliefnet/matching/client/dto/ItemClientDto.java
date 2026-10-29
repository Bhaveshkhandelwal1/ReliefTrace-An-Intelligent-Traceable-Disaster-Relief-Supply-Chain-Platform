package com.disasterreliefnet.matching.client.dto;

import com.disasterreliefnet.common.enums.AidCategory;

import java.math.BigDecimal;
import java.util.UUID;

public record ItemClientDto(
    UUID itemId,
    UUID warehouseId,
    String name,
    AidCategory category,
    String unit,
    BigDecimal quantityOnHand,
    BigDecimal quantityReserved,
    BigDecimal quantityAvailable
) {}
