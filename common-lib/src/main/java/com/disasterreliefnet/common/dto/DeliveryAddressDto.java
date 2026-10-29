package com.disasterreliefnet.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Standard delivery address record.
 */
public record DeliveryAddressDto(
    @NotBlank String street,
    @NotBlank String city,
    @NotBlank String state,
    @NotBlank @Size(min = 2, max = 2) String country,
    @NotBlank String zip
) {}
