package com.disasterreliefnet.analytics.service;

import com.disasterreliefnet.analytics.domain.DisasterSummary;
import com.disasterreliefnet.analytics.dto.*;
import com.disasterreliefnet.analytics.mapper.AnalyticsMapper;
import com.disasterreliefnet.analytics.mapper.AnalyticsMapperImpl;
import com.disasterreliefnet.analytics.repository.DisasterSummaryRepository;
import com.disasterreliefnet.analytics.repository.DonationFactRepository;
import com.disasterreliefnet.analytics.repository.RequestFactRepository;
import com.disasterreliefnet.analytics.repository.ShipmentFactRepository;
import com.disasterreliefnet.common.enums.DisasterStatus;
import com.disasterreliefnet.common.enums.DisasterType;
import com.disasterreliefnet.common.enums.RequestStatus;
import com.disasterreliefnet.common.enums.SeverityLevel;
import com.disasterreliefnet.common.enums.ShipmentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsQueryServiceTest {

    @Mock
    private DonationFactRepository donationFactRepository;

    @Mock
    private RequestFactRepository requestFactRepository;

    @Mock
    private ShipmentFactRepository shipmentFactRepository;

    @Mock
    private DisasterSummaryRepository disasterSummaryRepository;

    private final AnalyticsMapper analyticsMapper = new AnalyticsMapperImpl();
    private AnalyticsQueryService queryService;

    @BeforeEach
    void setUp() {
        queryService = new AnalyticsQueryService(
            donationFactRepository,
            requestFactRepository,
            shipmentFactRepository,
            disasterSummaryRepository,
            analyticsMapper
        );
    }

    @Test
    @DisplayName("Should aggregate donation summary metrics")
    void getDonationSummary_Success() {
        UUID disasterId = UUID.randomUUID();
        when(donationFactRepository.countDonations(eq(disasterId), any(), any())).thenReturn(25L);
        when(donationFactRepository.sumMonetaryAmount(eq(disasterId), any(), any())).thenReturn(BigDecimal.valueOf(50000.00));
        when(donationFactRepository.sumInKindKg(eq(disasterId), any(), any())).thenReturn(BigDecimal.valueOf(1200.50));

        DonationSummaryResponse response = queryService.getDonationSummary(disasterId, null, null);

        assertNotNull(response);
        assertEquals(25L, response.totalDonations());
        assertEquals(BigDecimal.valueOf(50000.00), response.totalMonetaryAmount());
        assertEquals(BigDecimal.valueOf(1200.50), response.totalInKindKg());
    }

    @Test
    @DisplayName("Should aggregate request summary metrics")
    void getRequestSummary_Success() {
        UUID disasterId = UUID.randomUUID();
        when(requestFactRepository.countRequests(eq(disasterId), any(), any())).thenReturn(100L);
        when(requestFactRepository.countByStatus(eq(RequestStatus.PENDING), eq(disasterId), any(), any())).thenReturn(30L);
        when(requestFactRepository.countByStatus(eq(RequestStatus.VERIFIED), eq(disasterId), any(), any())).thenReturn(20L);
        when(requestFactRepository.countByStatus(eq(RequestStatus.ALLOCATED), eq(disasterId), any(), any())).thenReturn(25L);
        when(requestFactRepository.countByStatus(eq(RequestStatus.DELIVERED), eq(disasterId), any(), any())).thenReturn(15L);
        when(requestFactRepository.countByStatus(eq(RequestStatus.REJECTED), eq(disasterId), any(), any())).thenReturn(10L);

        RequestSummaryResponse response = queryService.getRequestSummary(disasterId, null, null);

        assertNotNull(response);
        assertEquals(100L, response.totalRequests());
        assertEquals(30L, response.pendingCount());
        assertEquals(15L, response.deliveredCount());
    }

    @Test
    @DisplayName("Should aggregate shipment summary metrics")
    void getShipmentSummary_Success() {
        UUID disasterId = UUID.randomUUID();
        when(shipmentFactRepository.countShipments(eq(disasterId), any(), any())).thenReturn(50L);
        when(shipmentFactRepository.countByStatus(eq(ShipmentStatus.DELIVERED), eq(disasterId), any(), any())).thenReturn(40L);
        when(shipmentFactRepository.countByStatus(eq(ShipmentStatus.PICKED_UP), eq(disasterId), any(), any())).thenReturn(8L);
        when(shipmentFactRepository.countByStatus(eq(ShipmentStatus.DISPUTED), eq(disasterId), any(), any())).thenReturn(2L);
        when(shipmentFactRepository.avgDeliveryHours(eq(disasterId), any(), any())).thenReturn(3.5);

        ShipmentSummaryResponse response = queryService.getShipmentSummary(disasterId, null, null);

        assertNotNull(response);
        assertEquals(50L, response.totalShipments());
        assertEquals(40L, response.deliveredCount());
        assertEquals(3.5, response.avgDeliveryHours());
    }

    @Test
    @DisplayName("Should return disaster situation reports list")
    void getDisastersSummary_Success() {
        DisasterSummary summary = new DisasterSummary();
        summary.setDisasterId(UUID.randomUUID());
        summary.setDisasterName("Flood 2026");
        summary.setType(DisasterType.FLOOD);
        summary.setSeverityLevel(SeverityLevel.SEVERE);
        summary.setStatus(DisasterStatus.ACTIVE);

        when(disasterSummaryRepository.findAllByStatus(DisasterStatus.ACTIVE)).thenReturn(List.of(summary));

        List<DisasterSummaryResponse> list = queryService.getDisastersSummary(DisasterStatus.ACTIVE);

        assertEquals(1, list.size());
        assertEquals("Flood 2026", list.get(0).disasterName());
    }

    @Test
    @DisplayName("Should generate consolidated executive dashboard")
    void getDashboard_Success() {
        when(disasterSummaryRepository.countByStatus(DisasterStatus.ACTIVE)).thenReturn(3L);

        DashboardResponse dashboard = queryService.getDashboard(null);

        assertNotNull(dashboard);
        assertEquals(3L, dashboard.activeDisasters());
        assertNotNull(dashboard.donationSummary());
        assertNotNull(dashboard.requestSummary());
        assertNotNull(dashboard.shipmentSummary());
    }
}
