package com.disasterreliefnet.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for authorizing an aid disbursement payout.
 */
public record AuthorizeDisbursementRequest(
    @NotBlank String approvedBy,
    @Size(max = 500) String notes
) {}
