package com.disasterreliefnet.matching.client.dto;

import com.disasterreliefnet.common.dto.DeliveryAddressDto;
import com.disasterreliefnet.common.enums.AidCategory;
import com.disasterreliefnet.common.enums.RequestStatus;
import com.disasterreliefnet.common.enums.UrgencyLevel;

import java.math.BigDecimal;
import java.util.UUID;

public record AidRequestClientDto(
    UUID requestId,
    UUID receiverId,
    UUID disasterId,
    AidCategory category,
    String description,
    BigDecimal quantityRequested,
    String unit,
    UrgencyLevel urgencyLevel,
    DeliveryAddressDto deliveryAddress,
    RequestStatus status
) {}
