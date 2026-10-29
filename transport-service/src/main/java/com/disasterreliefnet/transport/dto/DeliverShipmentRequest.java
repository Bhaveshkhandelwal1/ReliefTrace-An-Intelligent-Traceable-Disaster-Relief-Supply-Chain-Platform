package com.disasterreliefnet.transport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * Request DTO for confirming delivery of a shipment.
 *
 * @param proofOfDeliveryUrl URL of the uploaded proof-of-delivery document
 * @param recipientSignature Optional digital signature string
 * @param deliveredAt        Actual delivery timestamp
 */
public record DeliverShipmentRequest(
        @NotBlank @Size(max = 500) String proofOfDeliveryUrl,
        @Size(max = 500) String recipientSignature,
        @NotNull Instant deliveredAt
) {}
