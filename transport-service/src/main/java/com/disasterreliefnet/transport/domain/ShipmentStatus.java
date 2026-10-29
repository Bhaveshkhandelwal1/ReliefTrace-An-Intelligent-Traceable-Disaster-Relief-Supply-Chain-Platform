package com.disasterreliefnet.transport.domain;

/**
 * Lifecycle status of a relief shipment.
 */
public enum ShipmentStatus {
    CREATED,
    ASSIGNED,
    PICKED_UP,
    IN_TRANSIT,
    DELIVERED,
    DISPUTED,
    FAILED,
    CANCELLED
}
