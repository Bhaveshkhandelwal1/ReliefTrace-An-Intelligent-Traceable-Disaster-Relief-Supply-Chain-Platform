package com.disasterreliefnet.common.event.payload;

import com.disasterreliefnet.common.enums.DonationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Event payload emitted when a donor registers a monetary or in-kind donation (Topic: donation.registered).
 */
public record DonationRegisteredPayload(
    @NotNull UUID donationId,
    @NotNull UUID donorId,
    @NotNull DonationType donationType,
    BigDecimal amount,
    String currency,
    String itemDescription,
    BigDecimal quantityKg,
    UUID disasterId,
    @NotBlank String donorEmail,
    @NotBlank String donorName
) {}
