package com.disasterreliefnet.receiver.dto;

import com.disasterreliefnet.common.dto.DeliveryAddressDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;
import java.util.UUID;

/**
 * Request body for registering a new receiver / beneficiary profile.
 */
public record CreateReceiverRequest(
    @NotBlank @Size(max = 100) String firstName,
    @NotBlank @Size(max = 100) String lastName,
    @Email String email,
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$") String phone,
    @NotNull UUID disasterId,
    @Valid @NotNull DeliveryAddressDto currentLocation,
    @Min(1) int householdSize,
    List<String> vulnerabilityTags
) {}
