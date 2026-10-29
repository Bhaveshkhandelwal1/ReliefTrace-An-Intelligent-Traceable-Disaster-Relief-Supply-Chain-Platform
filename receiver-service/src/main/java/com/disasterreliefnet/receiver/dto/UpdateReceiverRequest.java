package com.disasterreliefnet.receiver.dto;

import com.disasterreliefnet.common.dto.DeliveryAddressDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

import java.util.List;

/**
 * Request body for updating a receiver profile (partial fields).
 */
public record UpdateReceiverRequest(
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$") String phone,
    @Valid DeliveryAddressDto currentLocation,
    @Min(1) int householdSize,
    List<String> vulnerabilityTags
) {}
