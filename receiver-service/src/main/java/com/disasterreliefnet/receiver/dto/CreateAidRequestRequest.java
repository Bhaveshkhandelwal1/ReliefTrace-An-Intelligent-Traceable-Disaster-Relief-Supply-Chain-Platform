package com.disasterreliefnet.receiver.dto;

import com.disasterreliefnet.common.dto.DeliveryAddressDto;
import com.disasterreliefnet.common.enums.AidCategory;
import com.disasterreliefnet.common.enums.UrgencyLevel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request body for submitting a new aid request.
 */
public record CreateAidRequestRequest(
    @NotNull UUID disasterId,
    @NotNull AidCategory category,
    @NotBlank @Size(max = 1000) String description,
    @NotNull @DecimalMin("0.01") BigDecimal quantityRequested,
    @NotBlank @Size(max = 20) String unit,
    @NotNull UrgencyLevel urgencyLevel,
    @Valid @NotNull DeliveryAddressDto deliveryAddress
) {}
