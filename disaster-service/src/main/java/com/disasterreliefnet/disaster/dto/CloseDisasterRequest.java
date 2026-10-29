package com.disasterreliefnet.disaster.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload to mark an active disaster incident as closed.
 */
public record CloseDisasterRequest(
    @NotBlank @Size(max = 1000) String closureReason
) {}
