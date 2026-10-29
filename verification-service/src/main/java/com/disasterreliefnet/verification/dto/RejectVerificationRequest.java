package com.disasterreliefnet.verification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectVerificationRequest(
    @NotBlank(message = "Review notes are required when rejecting a case")
    @Size(max = 1000, message = "Review notes cannot exceed 1000 characters")
    String reviewNotes
) {}
