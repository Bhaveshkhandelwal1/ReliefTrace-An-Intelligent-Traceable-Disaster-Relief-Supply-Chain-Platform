package com.disasterreliefnet.finance.domain;

/**
 * Types of double-entry ledger entries for platform financial accounting.
 */
public enum LedgerEntryType {
    DONATION_IN,
    DISBURSEMENT_OUT,
    FEE,
    ADJUSTMENT
}
