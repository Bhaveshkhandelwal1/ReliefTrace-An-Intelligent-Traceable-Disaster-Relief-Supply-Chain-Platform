-- ============================================================
-- Finance Service DB Schema — V1
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ------------------------------------------------------------
-- payment_records
-- ------------------------------------------------------------
CREATE TABLE payment_records (
    payment_id        UUID           PRIMARY KEY,
    donation_id       UUID           NOT NULL,
    donor_id          UUID           NOT NULL,
    amount            NUMERIC(15,2)  NOT NULL,
    currency          CHAR(3)        NOT NULL,
    status            VARCHAR(15)    NOT NULL DEFAULT 'PENDING'
                      CHECK (status IN ('PENDING','PROCESSING','COMPLETED','FAILED','REFUNDED')),
    gateway_reference VARCHAR(255),
    processed_at      TIMESTAMPTZ,
    version           BIGINT         NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    deleted_at        TIMESTAMPTZ
);

CREATE UNIQUE INDEX idx_payment_records_donation_id ON payment_records (donation_id) WHERE deleted_at IS NULL;
CREATE        INDEX idx_payment_records_donor_id    ON payment_records (donor_id)    WHERE deleted_at IS NULL;
CREATE        INDEX idx_payment_records_status      ON payment_records (status);

-- ------------------------------------------------------------
-- disbursements
-- ------------------------------------------------------------
CREATE TABLE disbursements (
    disbursement_id UUID           PRIMARY KEY,
    allocation_id   UUID           NOT NULL,
    amount          NUMERIC(15,2)  NOT NULL,
    currency        CHAR(3)        NOT NULL,
    status          VARCHAR(15)    NOT NULL DEFAULT 'PENDING'
                    CHECK (status IN ('PENDING','AUTHORIZED','PAID','CANCELLED')),
    authorized_by   VARCHAR(255),
    notes           VARCHAR(500),
    paid_at         TIMESTAMPTZ,
    version         BIGINT         NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ
);

CREATE UNIQUE INDEX idx_disbursements_allocation_id ON disbursements (allocation_id) WHERE deleted_at IS NULL;
CREATE        INDEX idx_disbursements_status        ON disbursements (status);

-- ------------------------------------------------------------
-- ledger_entries
-- ------------------------------------------------------------
CREATE TABLE ledger_entries (
    entry_id     UUID           PRIMARY KEY,
    entry_type   VARCHAR(25)    NOT NULL
                 CHECK (entry_type IN ('DONATION_IN','DISBURSEMENT_OUT','FEE','ADJUSTMENT')),
    amount       NUMERIC(15,2)  NOT NULL,
    currency     CHAR(3)        NOT NULL,
    reference_id UUID           NOT NULL,
    description  VARCHAR(500),
    occurred_at  TIMESTAMPTZ    NOT NULL,
    created_at   TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ledger_entries_entry_type   ON ledger_entries (entry_type);
CREATE INDEX idx_ledger_entries_reference_id ON ledger_entries (reference_id);
CREATE INDEX idx_ledger_entries_occurred_at  ON ledger_entries (occurred_at);

-- ------------------------------------------------------------
-- outbox_events
-- ------------------------------------------------------------
CREATE TABLE outbox_events (
    event_id       UUID         PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id   UUID         NOT NULL,
    event_type     VARCHAR(100) NOT NULL,
    topic          VARCHAR(200) NOT NULL,
    payload        JSONB        NOT NULL,
    published      BOOLEAN      NOT NULL DEFAULT FALSE,
    published_at   TIMESTAMPTZ,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_outbox_published_created_at ON outbox_events (created_at) WHERE published = FALSE;

-- ------------------------------------------------------------
-- processed_events (idempotency guard)
-- ------------------------------------------------------------
CREATE TABLE processed_events (
    event_id     UUID         PRIMARY KEY,
    event_type   VARCHAR(100) NOT NULL,
    processed_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
