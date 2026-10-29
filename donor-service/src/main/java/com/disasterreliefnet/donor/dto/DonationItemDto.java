package com.disasterreliefnet.donor.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record DonationItemDto(
    UUID itemLineId,
    @NotBlank @Size(max = 200) String itemName,
    @NotBlank @Size(max = 30) String category,
    @NotNull @DecimalMin("0.001") BigDecimal quantity,
    @NotBlank @Size(max = 30) String unit,
    @DecimalMin("0.00") BigDecimal estimatedValue
) {}
