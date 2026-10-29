package com.disasterreliefnet.transport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for flagging a shipment as failed.
 */
public record FailShipmentRequest(
    @NotBlank @Size(max = 500) String reason
) {}
