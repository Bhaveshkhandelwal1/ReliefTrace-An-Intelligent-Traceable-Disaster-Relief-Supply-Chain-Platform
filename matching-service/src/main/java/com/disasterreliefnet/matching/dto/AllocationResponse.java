package com.disasterreliefnet.matching.dto;

import com.disasterreliefnet.common.enums.AllocationStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AllocationResponse(
    UUID allocationId,
    UUID requestId,
    UUID receiverId,
    UUID disasterId,
    UUID itemId,
    UUID warehouseId,
    BigDecimal quantity,
    String unit,
    AllocationStatus status,
    String approvedBy,
    String notes,
    String rejectionReason,
    double priorityScore,
    Instant createdAt,
    Instant updatedAt
) {}
