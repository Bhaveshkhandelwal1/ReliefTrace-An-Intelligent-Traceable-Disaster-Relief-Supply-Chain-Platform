package com.disasterreliefnet.disaster.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload to publish a situational update/advisory for a disaster.
 */
public record CreateDisasterUpdateRequest(
    @NotBlank @Size(max = 200) String title,
    @NotBlank @Size(max = 5000) String content,
    @Size(max = 200) String source
) {}
