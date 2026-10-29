package com.disasterreliefnet.finance.service;

import com.disasterreliefnet.finance.domain.LedgerEntry;
import com.disasterreliefnet.finance.domain.LedgerEntryType;
import com.disasterreliefnet.finance.dto.LedgerEntryResponse;
import com.disasterreliefnet.finance.mapper.FinanceMapper;
import com.disasterreliefnet.finance.repository.LedgerEntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Service maintaining the double-entry accounting ledger entries.
 */
@Service
@Transactional(readOnly = true)
public class LedgerService {

    private static final Logger log = LoggerFactory.getLogger(LedgerService.class);

    private final LedgerEntryRepository ledgerEntryRepository;
    private final FinanceMapper financeMapper;

    public LedgerService(LedgerEntryRepository ledgerEntryRepository, FinanceMapper financeMapper) {
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.financeMapper = financeMapper;
    }

    @Transactional
    public LedgerEntry recordEntry(
        LedgerEntryType entryType,
        BigDecimal amount,
        String currency,
        UUID referenceId,
        String description
    ) {
        log.info("Recording ledger entry type: {}, amount: {} {}, reference: {}", entryType, amount, currency, referenceId);

        LedgerEntry entry = new LedgerEntry();
        entry.setEntryType(entryType);
        entry.setAmount(amount);
        entry.setCurrency(currency != null ? currency : "USD");
        entry.setReferenceId(referenceId);
        entry.setDescription(description);
        entry.setOccurredAt(Instant.now());

        LedgerEntry saved = ledgerEntryRepository.save(entry);
        log.info("Ledger entry recorded with ID: {}", saved.getEntryId());
        return saved;
    }

    public Page<LedgerEntryResponse> getLedgerEntries(Instant fromDate, Instant toDate, Pageable pageable) {
        Instant from = fromDate != null ? fromDate : Instant.now().minus(365, ChronoUnit.DAYS);
        Instant to = toDate != null ? toDate : Instant.now().plus(1, ChronoUnit.DAYS);
        return ledgerEntryRepository.findAllByOccurredAtBetween(from, to, pageable)
            .map(financeMapper::toLedgerEntryResponse);
    }
}
