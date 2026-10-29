package com.disasterreliefnet.finance.service;

import com.disasterreliefnet.finance.domain.LedgerEntryType;
import com.disasterreliefnet.finance.dto.FinanceSummaryResponse;
import com.disasterreliefnet.finance.repository.LedgerEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Service aggregating high-level platform financial metrics.
 */
@Service
@Transactional(readOnly = true)
public class FinanceSummaryService {

    private final LedgerEntryRepository ledgerEntryRepository;

    public FinanceSummaryService(LedgerEntryRepository ledgerEntryRepository) {
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    public FinanceSummaryResponse getSummary(Instant fromDate, Instant toDate) {
        Instant from = fromDate != null ? fromDate : Instant.now().minus(365, ChronoUnit.DAYS);
        Instant to = toDate != null ? toDate : Instant.now().plus(1, ChronoUnit.DAYS);

        BigDecimal totalDonations = ledgerEntryRepository.sumAmountByEntryTypeAndOccurredAtBetween(
            LedgerEntryType.DONATION_IN,
            from,
            to
        );
        BigDecimal totalDisbursed = ledgerEntryRepository.sumAmountByEntryTypeAndOccurredAtBetween(
            LedgerEntryType.DISBURSEMENT_OUT,
            from,
            to
        );

        BigDecimal net = totalDonations.subtract(totalDisbursed);

        return new FinanceSummaryResponse(
            totalDonations,
            totalDisbursed,
            net,
            "USD",
            fromDate,
            toDate
        );
    }
}
