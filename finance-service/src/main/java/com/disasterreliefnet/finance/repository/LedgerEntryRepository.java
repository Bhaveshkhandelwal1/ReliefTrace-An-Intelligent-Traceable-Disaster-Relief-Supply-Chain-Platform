package com.disasterreliefnet.finance.repository;

import com.disasterreliefnet.finance.domain.LedgerEntry;
import com.disasterreliefnet.finance.domain.LedgerEntryType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link LedgerEntry} entities.
 */
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    Page<LedgerEntry> findAllByOccurredAtBetween(Instant fromDate, Instant toDate, Pageable pageable);

    List<LedgerEntry> findAllByReferenceId(UUID referenceId);

    @Query("SELECT COALESCE(SUM(l.amount), 0) FROM LedgerEntry l WHERE l.entryType = :entryType")
    BigDecimal sumAmountByEntryType(@Param("entryType") LedgerEntryType entryType);

    @Query("SELECT COALESCE(SUM(l.amount), 0) FROM LedgerEntry l WHERE l.entryType = :entryType AND l.occurredAt >= :fromDate AND l.occurredAt <= :toDate")
    BigDecimal sumAmountByEntryTypeAndOccurredAtBetween(
        @Param("entryType") LedgerEntryType entryType,
        @Param("fromDate") Instant fromDate,
        @Param("toDate") Instant toDate
    );
}
