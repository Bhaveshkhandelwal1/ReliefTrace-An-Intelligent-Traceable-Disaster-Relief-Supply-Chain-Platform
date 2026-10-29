package com.disasterreliefnet.donor.dto;

import com.disasterreliefnet.common.enums.DonationType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateDonationRequest(
    @NotNull DonationType donationType,
    @DecimalMin("0.01") BigDecimal amount,
    @Size(min = 3, max = 3) String currency,
    @Size(max = 500) String itemDescription,
    @DecimalMin("0.001") BigDecimal quantityKg,
    UUID disasterId,
    @Size(max = 1000) String notes,
    List<@Valid DonationItemDto> items
) {}
