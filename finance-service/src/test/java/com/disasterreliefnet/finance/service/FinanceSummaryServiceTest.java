package com.disasterreliefnet.finance.service;

import com.disasterreliefnet.finance.domain.LedgerEntryType;
import com.disasterreliefnet.finance.dto.FinanceSummaryResponse;
import com.disasterreliefnet.finance.repository.LedgerEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinanceSummaryServiceTest {

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    private FinanceSummaryService financeSummaryService;

    @BeforeEach
    void setUp() {
        financeSummaryService = new FinanceSummaryService(ledgerEntryRepository);
    }

    @Test
    @DisplayName("Should aggregate financial metrics and compute net balance")
    void getSummary_Success() {
        when(ledgerEntryRepository.sumAmountByEntryTypeAndOccurredAtBetween(eq(LedgerEntryType.DONATION_IN), any(), any()))
            .thenReturn(BigDecimal.valueOf(10000.00));
        when(ledgerEntryRepository.sumAmountByEntryTypeAndOccurredAtBetween(eq(LedgerEntryType.DISBURSEMENT_OUT), any(), any()))
            .thenReturn(BigDecimal.valueOf(3500.00));

        FinanceSummaryResponse summary = financeSummaryService.getSummary(Instant.now(), Instant.now());

        assertNotNull(summary);
        assertEquals(BigDecimal.valueOf(10000.00), summary.totalDonationsReceived());
        assertEquals(BigDecimal.valueOf(3500.00), summary.totalDisbursed());
        assertEquals(BigDecimal.valueOf(6500.00), summary.netBalance());
    }
}
