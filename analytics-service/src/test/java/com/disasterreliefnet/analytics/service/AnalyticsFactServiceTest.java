package com.disasterreliefnet.analytics.service;

import com.disasterreliefnet.analytics.domain.*;
import com.disasterreliefnet.analytics.repository.*;
import com.disasterreliefnet.common.dto.AffectedAreaDto;
import com.disasterreliefnet.common.dto.DeliveryAddressDto;
import com.disasterreliefnet.common.enums.*;
import com.disasterreliefnet.common.event.payload.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsFactServiceTest {

    @Mock
    private DonationFactRepository donationFactRepository;

    @Mock
    private RequestFactRepository requestFactRepository;

    @Mock
    private ShipmentFactRepository shipmentFactRepository;

    @Mock
    private DisasterSummaryRepository disasterSummaryRepository;

    private AnalyticsFactService factService;

    @BeforeEach
    void setUp() {
        factService = new AnalyticsFactService(
            donationFactRepository,
            requestFactRepository,
            shipmentFactRepository,
            disasterSummaryRepository
        );
    }

    @Test
    @DisplayName("Should materialise donation fact and update disaster summary")
    void recordDonation_Success() {
        UUID donationId = UUID.randomUUID();
        UUID disasterId = UUID.randomUUID();
        DonationRegisteredPayload payload = new DonationRegisteredPayload(
            donationId,
            UUID.randomUUID(),
            DonationType.MONETARY,
            BigDecimal.valueOf(500.00),
            "USD",
            null,
            null,
            disasterId,
            "donor@relief.org",
            "Donor Name"
        );

        DisasterSummary summary = new DisasterSummary();
        summary.setDisasterId(disasterId);
        summary.setTotalDonationsLinked(5);
        summary.setTotalMonetaryDonated(BigDecimal.valueOf(1000.00));

        when(donationFactRepository.findByDonationId(donationId)).thenReturn(Optional.empty());
        when(disasterSummaryRepository.findByDisasterId(disasterId)).thenReturn(Optional.of(summary));

        factService.recordDonation(payload);

        verify(donationFactRepository).save(any(DonationFact.class));
        verify(disasterSummaryRepository).save(summary);
        assertEquals(6, summary.getTotalDonationsLinked());
        assertEquals(BigDecimal.valueOf(1500.00), summary.getTotalMonetaryDonated());
    }

    @Test
    @DisplayName("Should materialise request fact and increment requests created")
    void recordRequestCreated_Success() {
        UUID requestId = UUID.randomUUID();
        UUID disasterId = UUID.randomUUID();
        RequestCreatedPayload payload = new RequestCreatedPayload(
            requestId,
            UUID.randomUUID(),
            disasterId,
            AidCategory.FOOD,
            "Ration pack",
            BigDecimal.valueOf(10),
            "PACKS",
            UrgencyLevel.HIGH,
            new DeliveryAddressDto("123 St", "City", "ST", "US", "12345"),
            "receiver@relief.org",
            "Receiver"
        );

        DisasterSummary summary = new DisasterSummary();
        summary.setDisasterId(disasterId);
        summary.setTotalRequestsCreated(10);

        when(requestFactRepository.findByRequestId(requestId)).thenReturn(Optional.empty());
        when(disasterSummaryRepository.findByDisasterId(disasterId)).thenReturn(Optional.of(summary));

        factService.recordRequestCreated(payload);

        verify(requestFactRepository).save(any(RequestFact.class));
        verify(disasterSummaryRepository).save(summary);
        assertEquals(11, summary.getTotalRequestsCreated());
    }

    @Test
    @DisplayName("Should update request status to VERIFIED when verification approved")
    void recordRequestVerified_Success() {
        UUID requestId = UUID.randomUUID();
        RequestFact fact = new RequestFact();
        fact.setRequestId(requestId);
        fact.setStatus(RequestStatus.PENDING);

        when(requestFactRepository.findByRequestId(requestId)).thenReturn(Optional.of(fact));

        RequestVerifiedPayload payload = new RequestVerifiedPayload(
            requestId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            "APPROVED",
            "coordinator-1",
            "Verified on ground"
        );

        factService.recordRequestVerified(payload);

        assertEquals(RequestStatus.VERIFIED, fact.getStatus());
        verify(requestFactRepository).save(fact);
    }

    @Test
    @DisplayName("Should materialise shipment fact upon driver assignment")
    void recordShipmentAssigned_Success() {
        UUID shipmentId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();

        ShipmentAssignedPayload payload = new ShipmentAssignedPayload(
            shipmentId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            driverId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Driver Dan",
            "TRK-1234",
            Instant.now(),
            new DeliveryAddressDto("St", "City", "ST", "US", "12345")
        );

        when(shipmentFactRepository.findByShipmentId(shipmentId)).thenReturn(Optional.empty());

        factService.recordShipmentAssigned(payload);

        verify(shipmentFactRepository).save(any(ShipmentFact.class));
    }

    @Test
    @DisplayName("Should record disaster declaration")
    void recordDisasterDeclared_Success() {
        UUID disasterId = UUID.randomUUID();
        DisasterDeclaredPayload payload = new DisasterDeclaredPayload(
            disasterId,
            "Hurricane Helene",
            DisasterType.HURRICANE,
            SeverityLevel.CATASTROPHIC,
            new AffectedAreaDto(27.95, -82.45, 100.0, "Tampa Bay", "US"),
            Instant.now()
        );

        when(disasterSummaryRepository.findByDisasterId(disasterId)).thenReturn(Optional.empty());

        factService.recordDisasterDeclared(payload);

        verify(disasterSummaryRepository).save(any(DisasterSummary.class));
    }

    @Test
    @DisplayName("Should close disaster summary")
    void recordDisasterClosed_Success() {
        UUID disasterId = UUID.randomUUID();
        DisasterSummary summary = new DisasterSummary();
        summary.setDisasterId(disasterId);
        summary.setStatus(DisasterStatus.ACTIVE);

        when(disasterSummaryRepository.findByDisasterId(disasterId)).thenReturn(Optional.of(summary));

        DisasterClosedPayload payload = new DisasterClosedPayload(
            disasterId,
            "Emergency phase concluded",
            "Director",
            Instant.now()
        );

        factService.recordDisasterClosed(payload);

        assertEquals(DisasterStatus.CLOSED, summary.getStatus());
        verify(disasterSummaryRepository).save(summary);
    }
}
