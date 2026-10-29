package com.disasterreliefnet.transport.dto;

import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating mutable driver fields.
 *
 * @param phone         New phone number (optional)
 * @param licenseNumber New licence number (optional)
 * @param available     Override availability flag (optional)
 */
public record UpdateDriverRequest(
        String phone,
        @Size(max = 50) String licenseNumber,
        Boolean available
) {}
