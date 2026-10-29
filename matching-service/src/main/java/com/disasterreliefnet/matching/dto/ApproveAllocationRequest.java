package com.disasterreliefnet.matching.dto;

import jakarta.validation.constraints.Size;

public record ApproveAllocationRequest(
    String approvedBy,

    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    String notes
) {}
