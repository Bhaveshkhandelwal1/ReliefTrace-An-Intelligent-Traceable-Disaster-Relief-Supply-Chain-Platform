package com.disasterreliefnet.verification.dto;

import jakarta.validation.constraints.Size;

public record ApproveVerificationRequest(
    @Size(max = 1000, message = "Review notes cannot exceed 1000 characters")
    String reviewNotes
) {}
