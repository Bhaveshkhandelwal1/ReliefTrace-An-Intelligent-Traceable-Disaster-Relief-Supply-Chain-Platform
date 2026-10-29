package com.disasterreliefnet.donor.dto;

import com.disasterreliefnet.common.enums.DonationStatus;
import com.disasterreliefnet.common.enums.DonationType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DonationResponse(
    UUID donationId,
    UUID donorId,
    DonationType donationType,
    BigDecimal amount,
    String currency,
    String itemDescription,
    BigDecimal quantityKg,
    UUID disasterId,
    DonationStatus status,
    String notes,
    String receiptNumber,
    Instant receiptIssuedAt,
    List<DonationItemDto> items,
    Instant createdAt,
    Instant updatedAt
) {}
