package com.disasterreliefnet.receiver.mapper;

import com.disasterreliefnet.common.dto.DeliveryAddressDto;
import com.disasterreliefnet.receiver.domain.AidRequest;
import com.disasterreliefnet.receiver.domain.Receiver;
import com.disasterreliefnet.receiver.dto.AidRequestResponse;
import com.disasterreliefnet.receiver.dto.ReceiverResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * MapStruct mapper for Receiver and AidRequest domain ↔ DTO conversions.
 */
@Mapper(componentModel = "spring")
public interface ReceiverMapper {

    @Mapping(target = "currentLocation", source = ".", qualifiedByName = "receiverToAddress")
    @Mapping(target = "vulnerabilityTags", source = "vulnerabilityTags", qualifiedByName = "tagsToList")
    ReceiverResponse toReceiverResponse(Receiver receiver);

    @Mapping(target = "receiverId", source = "receiver.receiverId")
    @Mapping(target = "deliveryAddress", source = ".", qualifiedByName = "aidRequestToAddress")
    AidRequestResponse toAidRequestResponse(AidRequest aidRequest);

    @Named("receiverToAddress")
    default DeliveryAddressDto receiverToAddress(Receiver receiver) {
        if (receiver == null) {
            return null;
        }
        return new DeliveryAddressDto(
            receiver.getStreet(),
            receiver.getCity(),
            receiver.getState(),
            receiver.getCountry(),
            receiver.getZip()
        );
    }

    @Named("aidRequestToAddress")
    default DeliveryAddressDto aidRequestToAddress(AidRequest aidRequest) {
        if (aidRequest == null) {
            return null;
        }
        return new DeliveryAddressDto(
            aidRequest.getDeliveryStreet(),
            aidRequest.getDeliveryCity(),
            aidRequest.getDeliveryState(),
            aidRequest.getDeliveryCountry(),
            aidRequest.getDeliveryZip()
        );
    }

    @Named("tagsToList")
    default List<String> tagsToList(String tags) {
        if (tags == null || tags.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.asList(tags.split(","));
    }
}
