package com.disasterreliefnet.transport.mapper;

import com.disasterreliefnet.common.dto.DeliveryAddressDto;
import com.disasterreliefnet.transport.domain.Driver;
import com.disasterreliefnet.transport.domain.Shipment;
import com.disasterreliefnet.transport.domain.ShipmentTrackingEvent;
import com.disasterreliefnet.transport.domain.Vehicle;
import com.disasterreliefnet.transport.dto.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper converting between transport domain entities and their DTOs.
 */
@Mapper(componentModel = "spring")
public interface TransportMapper {

    // -----------------------------------------------------------------------
    // Driver
    // -----------------------------------------------------------------------

    @Mapping(target = "driverId",         ignore = true)
    @Mapping(target = "available",        constant = "true")
    @Mapping(target = "activeShipmentId", ignore = true)
    @Mapping(target = "version",          ignore = true)
    @Mapping(target = "createdAt",        ignore = true)
    @Mapping(target = "updatedAt",        ignore = true)
    @Mapping(target = "deletedAt",        ignore = true)
    Driver toDriver(CreateDriverRequest request);

    DriverResponse toDriverResponse(Driver driver);

    // -----------------------------------------------------------------------
    // Vehicle
    // -----------------------------------------------------------------------

    @Mapping(target = "vehicleId",   ignore = true)
    @Mapping(target = "active",      constant = "true")
    @Mapping(target = "version",     ignore = true)
    @Mapping(target = "createdAt",   ignore = true)
    @Mapping(target = "updatedAt",   ignore = true)
    @Mapping(target = "deletedAt",   ignore = true)
    Vehicle toVehicle(CreateVehicleRequest request);

    VehicleResponse toVehicleResponse(Vehicle vehicle);

    // -----------------------------------------------------------------------
    // Shipment
    // -----------------------------------------------------------------------

    @Mapping(target = "shipmentId",          ignore = true)
    @Mapping(target = "driverId",            ignore = true)
    @Mapping(target = "vehicleId",           ignore = true)
    @Mapping(target = "status",              ignore = true)
    @Mapping(target = "pickedUpAt",          ignore = true)
    @Mapping(target = "deliveredAt",         ignore = true)
    @Mapping(target = "proofOfDeliveryUrl",  ignore = true)
    @Mapping(target = "recipientSignature",  ignore = true)
    @Mapping(target = "disputeReason",       ignore = true)
    @Mapping(target = "disputeEvidenceUrl",  ignore = true)
    @Mapping(target = "version",             ignore = true)
    @Mapping(target = "createdAt",           ignore = true)
    @Mapping(target = "updatedAt",           ignore = true)
    @Mapping(target = "deletedAt",           ignore = true)
    @Mapping(target = "pickupStreet",   source = "pickupAddress.street")
    @Mapping(target = "pickupCity",     source = "pickupAddress.city")
    @Mapping(target = "pickupState",    source = "pickupAddress.state")
    @Mapping(target = "pickupCountry",  source = "pickupAddress.country")
    @Mapping(target = "pickupZip",      source = "pickupAddress.zip")
    @Mapping(target = "deliveryStreet",  source = "deliveryAddress.street")
    @Mapping(target = "deliveryCity",    source = "deliveryAddress.city")
    @Mapping(target = "deliveryState",   source = "deliveryAddress.state")
    @Mapping(target = "deliveryCountry", source = "deliveryAddress.country")
    @Mapping(target = "deliveryZip",     source = "deliveryAddress.zip")
    Shipment toShipment(CreateShipmentRequest request);

    @Mapping(target = "pickupAddress",   expression = "java(toAddress(s.getPickupStreet(), s.getPickupCity(), s.getPickupState(), s.getPickupCountry(), s.getPickupZip()))")
    @Mapping(target = "deliveryAddress", expression = "java(toAddress(s.getDeliveryStreet(), s.getDeliveryCity(), s.getDeliveryState(), s.getDeliveryCountry(), s.getDeliveryZip()))")
    ShipmentResponse toShipmentResponse(Shipment s);

    // -----------------------------------------------------------------------
    // Tracking event
    // -----------------------------------------------------------------------

    TrackingEventDto toTrackingEventDto(ShipmentTrackingEvent event);

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    default DeliveryAddressDto toAddress(String street, String city, String state, String country, String zip) {
        return new DeliveryAddressDto(street, city, state, country, zip);
    }
}
