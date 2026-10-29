package com.disasterreliefnet.receiver.dto;

import com.disasterreliefnet.common.dto.DeliveryAddressDto;
import com.disasterreliefnet.common.enums.VerificationStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * API response representing a receiver profile.
 */
public record ReceiverResponse(
    UUID receiverId,
    String firstName,
    String lastName,
    String email,
    String phone,
    UUID disasterId,
    DeliveryAddressDto currentLocation,
    int householdSize,
    List<String> vulnerabilityTags,
    VerificationStatus verificationStatus,
    Instant createdAt,
    Instant updatedAt
) {}
