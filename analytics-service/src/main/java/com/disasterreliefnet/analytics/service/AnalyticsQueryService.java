package com.disasterreliefnet.analytics.service;

import com.disasterreliefnet.analytics.domain.DisasterSummary;
import com.disasterreliefnet.analytics.dto.*;
import com.disasterreliefnet.analytics.mapper.AnalyticsMapper;
import com.disasterreliefnet.analytics.repository.DisasterSummaryRepository;
import com.disasterreliefnet.analytics.repository.DonationFactRepository;
import com.disasterreliefnet.analytics.repository.RequestFactRepository;
import com.disasterreliefnet.analytics.repository.ShipmentFactRepository;
import com.disasterreliefnet.common.enums.DisasterStatus;
import com.disasterreliefnet.common.enums.RequestStatus;
import com.disasterreliefnet.common.enums.ShipmentStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Read-only aggregation query service providing KPI responses and dashboard models.
 */
@Service
@Transactional(readOnly = true)
public class AnalyticsQueryService {

    private final DonationFactRepository donationFactRepository;
    private final RequestFactRepository requestFactRepository;
    private final ShipmentFactRepository shipmentFactRepository;
    private final DisasterSummaryRepository disasterSummaryRepository;
    private final AnalyticsMapper analyticsMapper;

    public AnalyticsQueryService(
        DonationFactRepository donationFactRepository,
        RequestFactRepository requestFactRepository,
        ShipmentFactRepository shipmentFactRepository,
        DisasterSummaryRepository disasterSummaryRepository,
        AnalyticsMapper analyticsMapper
    ) {
        this.donationFactRepository = donationFactRepository;
        this.requestFactRepository = requestFactRepository;
        this.shipmentFactRepository = shipmentFactRepository;
        this.disasterSummaryRepository = disasterSummaryRepository;
        this.analyticsMapper = analyticsMapper;
    }

    public DonationSummaryResponse getDonationSummary(UUID disasterId, Instant fromDate, Instant toDate) {
        long totalCount = donationFactRepository.countDonations(disasterId, fromDate, toDate);
        BigDecimal totalMonetary = donationFactRepository.sumMonetaryAmount(disasterId, fromDate, toDate);
        BigDecimal totalInKind = donationFactRepository.sumInKindKg(disasterId, fromDate, toDate);

        return new DonationSummaryResponse(
            totalCount,
            totalMonetary,
            totalInKind,
            "USD",
            new PeriodDto(fromDate, toDate)
        );
    }

    public RequestSummaryResponse getRequestSummary(UUID disasterId, Instant fromDate, Instant toDate) {
        long total = requestFactRepository.countRequests(disasterId, fromDate, toDate);
        long pending = requestFactRepository.countByStatus(RequestStatus.PENDING, disasterId, fromDate, toDate);
        long verified = requestFactRepository.countByStatus(RequestStatus.VERIFIED, disasterId, fromDate, toDate);
        long allocated = requestFactRepository.countByStatus(RequestStatus.ALLOCATED, disasterId, fromDate, toDate);
        long delivered = requestFactRepository.countByStatus(RequestStatus.DELIVERED, disasterId, fromDate, toDate);
        long rejected = requestFactRepository.countByStatus(RequestStatus.REJECTED, disasterId, fromDate, toDate);

        return new RequestSummaryResponse(
            total,
            pending,
            verified,
            allocated,
            delivered,
            rejected,
            new PeriodDto(fromDate, toDate)
        );
    }

    public ShipmentSummaryResponse getShipmentSummary(UUID disasterId, Instant fromDate, Instant toDate) {
        long total = shipmentFactRepository.countShipments(disasterId, fromDate, toDate);
        long delivered = shipmentFactRepository.countByStatus(ShipmentStatus.DELIVERED, disasterId, fromDate, toDate);
        long inTransit = shipmentFactRepository.countByStatus(ShipmentStatus.PICKED_UP, disasterId, fromDate, toDate);
        long disputed = shipmentFactRepository.countByStatus(ShipmentStatus.DISPUTED, disasterId, fromDate, toDate);
        double avgHours = shipmentFactRepository.avgDeliveryHours(disasterId, fromDate, toDate);

        return new ShipmentSummaryResponse(
            total,
            delivered,
            inTransit,
            disputed,
            avgHours,
            new PeriodDto(fromDate, toDate)
        );
    }

    public List<DisasterSummaryResponse> getDisastersSummary(DisasterStatus status) {
        List<DisasterSummary> summaries = status != null
            ? disasterSummaryRepository.findAllByStatus(status)
            : disasterSummaryRepository.findAll();

        return summaries.stream()
            .map(analyticsMapper::toDisasterSummaryResponse)
            .toList();
    }

    public DashboardResponse getDashboard(UUID disasterId) {
        DonationSummaryResponse donationSummary = getDonationSummary(disasterId, null, null);
        RequestSummaryResponse requestSummary = getRequestSummary(disasterId, null, null);
        ShipmentSummaryResponse shipmentSummary = getShipmentSummary(disasterId, null, null);
        long activeCount = disasterSummaryRepository.countByStatus(DisasterStatus.ACTIVE);

        return new DashboardResponse(
            disasterId,
            donationSummary,
            requestSummary,
            shipmentSummary,
            activeCount
        );
    }
}
