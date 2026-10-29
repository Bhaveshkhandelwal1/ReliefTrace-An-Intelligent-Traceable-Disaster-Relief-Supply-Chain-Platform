package com.disasterreliefnet.donor.dto;

import com.disasterreliefnet.common.enums.DonorType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateDonorRequest(
    @NotBlank @Size(max = 100) String firstName,
    @NotBlank @Size(max = 100) String lastName,
    @NotBlank @Email String email,
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$") String phone,
    DonorType donorType,
    @Valid AddressDto address,
    @Size(max = 50) String taxId
) {}
