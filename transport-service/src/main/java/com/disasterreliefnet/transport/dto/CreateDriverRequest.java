package com.disasterreliefnet.transport.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for registering a new driver.
 *
 * @param userId        Keycloak user ID
 * @param firstName     Given name
 * @param lastName      Family name
 * @param email         Contact email
 * @param phone         Contact phone
 * @param licenseNumber Driving licence number
 */
public record CreateDriverRequest(
        @NotBlank String userId,
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Email String email,
        @NotBlank String phone,
        @NotBlank @Size(max = 50) String licenseNumber
) {}
