package com.disasterreliefnet.matching.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AllocationSuggestionResponse(
    UUID requestId,
    UUID itemId,
    UUID warehouseId,
    String itemName,
    BigDecimal availableQty,
    double matchScore,
    String rationale
) {}
