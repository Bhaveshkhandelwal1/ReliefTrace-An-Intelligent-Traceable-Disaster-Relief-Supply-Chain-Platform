package com.disasterreliefnet.finance.dto;

import com.disasterreliefnet.finance.domain.LedgerEntryType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for a ledger entry.
 */
public record LedgerEntryResponse(
    UUID entryId,
    LedgerEntryType entryType,
    BigDecimal amount,
    String currency,
    UUID referenceId,
    String description,
    Instant occurredAt,
    Instant createdAt
) {}
