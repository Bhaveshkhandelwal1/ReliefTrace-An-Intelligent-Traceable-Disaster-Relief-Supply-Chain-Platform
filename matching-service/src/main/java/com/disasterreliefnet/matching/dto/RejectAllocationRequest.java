package com.disasterreliefnet.matching.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectAllocationRequest(
    @NotBlank(message = "Rejection reason is mandatory")
    @Size(max = 500, message = "Reason cannot exceed 500 characters")
    String reason
) {}
