package com.disasterreliefnet.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Address DTO record for warehouse locations.
 */
public record AddressDto(
    @NotBlank String street,
    @NotBlank String city,
    @NotBlank String state,
    @NotBlank @Size(min = 2, max = 2) String country,
    @NotBlank String zip
) {}
