package com.disasterreliefnet.common.event;

/**
 * Standard Kafka topic names defined in the DisasterReliefNet Event Catalogue (ARCHITECTURE.md Section 4).
 */
public final class KafkaTopics {

    private KafkaTopics() {
        // Constants class
    }

    public static final String DONATION_REGISTERED = "donation.registered";
    public static final String REQUEST_CREATED = "request.created";
    public static final String REQUEST_VERIFIED = "request.verified";
    public static final String ALLOCATION_APPROVED = "allocation.approved";
    public static final String ALLOCATION_REJECTED = "allocation.rejected";
    public static final String STOCK_RESERVED = "stock.reserved";
    public static final String STOCK_RESERVATION_FAILED = "stock.reservation.failed";
    public static final String SHIPMENT_ASSIGNED = "shipment.assigned";
    public static final String SHIPMENT_PICKED_UP = "shipment.picked_up";
    public static final String SHIPMENT_DELIVERED = "shipment.delivered";
    public static final String SHIPMENT_CANCELLED = "shipment.cancelled";
    public static final String SHIPMENT_FAILED = "shipment.failed";
    public static final String STOCK_LOW = "stock.low";
    public static final String DELIVERY_DISPUTED = "delivery.disputed";
    public static final String PAYMENT_COMPLETED = "payment.completed";
    public static final String DISASTER_DECLARED = "disaster.declared";
    public static final String DISASTER_UPDATED = "disaster.updated";
    public static final String DISASTER_CLOSED = "disaster.closed";
    public static final String ROAD_CLOSED = "disaster.road.closed";
}
