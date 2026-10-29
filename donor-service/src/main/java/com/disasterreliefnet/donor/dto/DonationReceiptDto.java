package com.disasterreliefnet.donor.dto;

import com.disasterreliefnet.common.enums.DonationType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Official acknowledgement receipt for tax and audit compliance.
 */
public record DonationReceiptDto(
    String receiptNumber,
    UUID donationId,
    UUID donorId,
    String donorName,
    String donorEmail,
    DonationType donationType,
    BigDecimal amount,
    String currency,
    String itemDescription,
    Instant issuedAt,
    boolean taxDeductible
) {}
