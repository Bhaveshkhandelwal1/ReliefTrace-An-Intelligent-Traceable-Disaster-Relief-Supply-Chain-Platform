package com.disasterreliefnet.finance.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for processing a donation payment against an external gateway.
 */
public record ProcessPaymentRequest(
    @NotBlank String gatewayToken
) {}
