package com.disasterreliefnet.donor.dto;

import com.disasterreliefnet.common.enums.DonorType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateDonorRequest(
    @Size(max = 100) String firstName,
    @Size(max = 100) String lastName,
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$") String phone,
    DonorType donorType,
    @Valid AddressDto address,
    @Size(max = 50) String taxId
) {}
