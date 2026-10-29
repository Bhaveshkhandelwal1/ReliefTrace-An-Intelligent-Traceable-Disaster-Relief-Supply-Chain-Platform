package com.disasterreliefnet.analytics.service;

import com.disasterreliefnet.analytics.domain.*;
import com.disasterreliefnet.analytics.repository.*;
import com.disasterreliefnet.common.enums.*;
import com.disasterreliefnet.common.event.payload.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Service materialising event-stream facts and operational aggregates for read models.
 */
@Service
@Transactional
public class AnalyticsFactService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsFactService.class);

    private final DonationFactRepository donationFactRepository;
    private final RequestFactRepository requestFactRepository;
    private final ShipmentFactRepository shipmentFactRepository;
    private final DisasterSummaryRepository disasterSummaryRepository;

    public AnalyticsFactService(
        DonationFactRepository donationFactRepository,
        RequestFactRepository requestFactRepository,
        ShipmentFactRepository shipmentFactRepository,
        DisasterSummaryRepository disasterSummaryRepository
    ) {
        this.donationFactRepository = donationFactRepository;
        this.requestFactRepository = requestFactRepository;
        this.shipmentFactRepository = shipmentFactRepository;
        this.disasterSummaryRepository = disasterSummaryRepository;
    }

    public void recordDonation(DonationRegisteredPayload payload) {
        log.info("Materialising donation fact for donation ID: {}", payload.donationId());

        if (donationFactRepository.findByDonationId(payload.donationId()).isPresent()) {
            return;
        }

        DonationFact fact = new DonationFact();
        fact.setDonationId(payload.donationId());
        fact.setDonorId(payload.donorId());
        fact.setDisasterId(payload.disasterId());
        fact.setDonationType(payload.donationType());
        fact.setAmount(payload.amount());
        fact.setCurrency(payload.currency() != null ? payload.currency() : "USD");
        fact.setQuantityKg(payload.quantityKg());
        fact.setRegisteredAt(Instant.now());
        donationFactRepository.save(fact);

        if (payload.disasterId() != null) {
            disasterSummaryRepository.findByDisasterId(payload.disasterId()).ifPresent(summary -> {
                summary.setTotalDonationsLinked(summary.getTotalDonationsLinked() + 1);
                if (payload.amount() != null) {
                    summary.setTotalMonetaryDonated(summary.getTotalMonetaryDonated().add(payload.amount()));
                }
                disasterSummaryRepository.save(summary);
            });
        }
    }

    public void recordRequestCreated(RequestCreatedPayload payload) {
        log.info("Materialising request fact for request ID: {}", payload.requestId());

        if (requestFactRepository.findByRequestId(payload.requestId()).isPresent()) {
            return;
        }

        RequestFact fact = new RequestFact();
        fact.setRequestId(payload.requestId());
        fact.setReceiverId(payload.receiverId());
        fact.setDisasterId(payload.disasterId());
        fact.setCategory(payload.category());
        fact.setUrgencyLevel(payload.urgencyLevel());
        fact.setStatus(RequestStatus.PENDING);
        fact.setQuantityRequested(payload.quantityRequested());
        fact.setUnit(payload.unit());
        fact.setCreatedAtSource(Instant.now());
        requestFactRepository.save(fact);

        disasterSummaryRepository.findByDisasterId(payload.disasterId()).ifPresent(summary -> {
            summary.setTotalRequestsCreated(summary.getTotalRequestsCreated() + 1);
            disasterSummaryRepository.save(summary);
        });
    }

    public void recordRequestVerified(RequestVerifiedPayload payload) {
        log.info("Updating request fact verification for request ID: {}", payload.requestId());

        requestFactRepository.findByRequestId(payload.requestId()).ifPresent(fact -> {
            boolean approved = "APPROVED".equalsIgnoreCase(payload.outcome());
            fact.setStatus(approved ? RequestStatus.VERIFIED : RequestStatus.REJECTED);
            fact.setVerifiedAt(Instant.now());
            requestFactRepository.save(fact);
        });
    }

    public void recordAllocationApproved(AllocationApprovedPayload payload) {
        log.info("Updating request fact allocation for request ID: {}", payload.requestId());

        requestFactRepository.findByRequestId(payload.requestId()).ifPresent(fact -> {
            fact.setStatus(RequestStatus.ALLOCATED);
            requestFactRepository.save(fact);
        });
    }

    public void recordShipmentAssigned(ShipmentAssignedPayload payload) {
        log.info("Materialising shipment fact for shipment ID: {}", payload.shipmentId());

        ShipmentFact fact = shipmentFactRepository.findByShipmentId(payload.shipmentId())
            .orElseGet(() -> {
                ShipmentFact f = new ShipmentFact();
                f.setShipmentId(payload.shipmentId());
                f.setAllocationId(payload.allocationId());
                f.setReceiverId(payload.receiverId());
                return f;
            });

        fact.setDriverId(payload.driverId());
        fact.setStatus(ShipmentStatus.ASSIGNED);
        fact.setAssignedAt(Instant.now());
        shipmentFactRepository.save(fact);
    }

    public void recordShipmentDelivered(ShipmentDeliveredPayload payload) {
        log.info("Updating shipment fact delivery for shipment ID: {}", payload.shipmentId());

        shipmentFactRepository.findByShipmentId(payload.shipmentId()).ifPresent(fact -> {
            Instant deliveredTime = payload.deliveredAt() != null ? payload.deliveredAt() : Instant.now();
            fact.setStatus(ShipmentStatus.DELIVERED);
            fact.setDeliveredAt(deliveredTime);

            if (fact.getAssignedAt() != null) {
                double hours = Duration.between(fact.getAssignedAt(), deliveredTime).toMinutes() / 60.0;
                fact.setDeliveryHours(Math.max(0.0, hours));
            }

            shipmentFactRepository.save(fact);

            if (fact.getDisasterId() != null) {
                disasterSummaryRepository.findByDisasterId(fact.getDisasterId()).ifPresent(summary -> {
                    summary.setTotalRequestsDelivered(summary.getTotalRequestsDelivered() + 1);
                    disasterSummaryRepository.save(summary);
                });
            }
        });
    }

    public void recordDisasterDeclared(DisasterDeclaredPayload payload) {
        log.info("Materialising disaster summary for disaster: {}", payload.disasterId());

        if (disasterSummaryRepository.findByDisasterId(payload.disasterId()).isPresent()) {
            return;
        }

        DisasterSummary summary = new DisasterSummary();
        summary.setDisasterId(payload.disasterId());
        summary.setDisasterName(payload.name());
        summary.setType(payload.type());
        summary.setSeverityLevel(payload.severityLevel());
        summary.setStatus(DisasterStatus.ACTIVE);
        summary.setDeclaredAt(payload.occurredAt() != null ? payload.occurredAt() : Instant.now());
        disasterSummaryRepository.save(summary);
    }

    public void recordDisasterClosed(DisasterClosedPayload payload) {
        log.info("Closing disaster summary for disaster: {}", payload.disasterId());

        disasterSummaryRepository.findByDisasterId(payload.disasterId()).ifPresent(summary -> {
            summary.setStatus(DisasterStatus.CLOSED);
            summary.setClosedAt(payload.closedAt() != null ? payload.closedAt() : Instant.now());
            disasterSummaryRepository.save(summary);
        });
    }
}
