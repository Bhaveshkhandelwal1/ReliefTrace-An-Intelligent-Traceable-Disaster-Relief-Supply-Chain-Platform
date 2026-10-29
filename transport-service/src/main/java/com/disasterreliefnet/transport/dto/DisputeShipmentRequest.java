package com.disasterreliefnet.transport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for raising a delivery dispute.
 *
 * @param reason      Human-readable reason for the dispute
 * @param evidenceUrl Optional URL of supporting evidence
 */
public record DisputeShipmentRequest(
        @NotBlank @Size(max = 1000) String reason,
        @Size(max = 500) String evidenceUrl
) {}
