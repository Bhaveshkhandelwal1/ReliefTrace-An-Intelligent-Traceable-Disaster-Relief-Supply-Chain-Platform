package com.disasterreliefnet.inventory.dto;

import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * Request payload for modifying item details or updating its expiry timestamp.
 */
public record UpdateItemRequest(
    @Size(max = 200) String name,
    Instant expiresAt
) {}
