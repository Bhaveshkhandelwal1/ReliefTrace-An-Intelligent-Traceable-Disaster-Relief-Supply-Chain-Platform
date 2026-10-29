-- ============================================================
-- Analytics Service DB Schema — V1
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ------------------------------------------------------------
-- donation_facts
-- ------------------------------------------------------------
CREATE TABLE donation_facts (
    fact_id       UUID          PRIMARY KEY,
    donation_id   UUID          NOT NULL,
    donor_id      UUID          NOT NULL,
    disaster_id   UUID,
    donation_type VARCHAR(20)   NOT NULL,
    amount        NUMERIC(15,2),
    currency      CHAR(3),
    quantity_kg   NUMERIC(10,3),
    registered_at TIMESTAMPTZ   NOT NULL,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_donation_facts_donation_id   ON donation_facts (donation_id);
CREATE        INDEX idx_donation_facts_disaster_id   ON donation_facts (disaster_id);
CREATE        INDEX idx_donation_facts_registered_at ON donation_facts (registered_at);
CREATE        INDEX idx_donation_facts_donor_id      ON donation_facts (donor_id);

-- ------------------------------------------------------------
-- request_facts
-- ------------------------------------------------------------
CREATE TABLE request_facts (
    fact_id            UUID          PRIMARY KEY,
    request_id         UUID          NOT NULL,
    receiver_id        UUID          NOT NULL,
    disaster_id        UUID          NOT NULL,
    category           VARCHAR(20)   NOT NULL,
    urgency_level      VARCHAR(10)   NOT NULL,
    status             VARCHAR(20)   NOT NULL,
    quantity_requested NUMERIC(10,3),
    unit               VARCHAR(20),
    created_at_source  TIMESTAMPTZ   NOT NULL,
    verified_at        TIMESTAMPTZ,
    delivered_at       TIMESTAMPTZ,
    created_at         TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_request_facts_request_id  ON request_facts (request_id);
CREATE        INDEX idx_request_facts_disaster_id ON request_facts (disaster_id);
CREATE        INDEX idx_request_facts_status      ON request_facts (status);
CREATE        INDEX idx_request_facts_category    ON request_facts (category);

-- ------------------------------------------------------------
-- shipment_facts
-- ------------------------------------------------------------
CREATE TABLE shipment_facts (
    fact_id        UUID             PRIMARY KEY,
    shipment_id    UUID             NOT NULL,
    allocation_id  UUID             NOT NULL,
    driver_id      UUID,
    receiver_id    UUID             NOT NULL,
    disaster_id    UUID,
    status         VARCHAR(20)      NOT NULL,
    assigned_at    TIMESTAMPTZ,
    picked_up_at   TIMESTAMPTZ,
    delivered_at   TIMESTAMPTZ,
    delivery_hours DOUBLE PRECISION,
    created_at     TIMESTAMPTZ      NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ      NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_shipment_facts_shipment_id  ON shipment_facts (shipment_id);
CREATE        INDEX idx_shipment_facts_disaster_id  ON shipment_facts (disaster_id);
CREATE        INDEX idx_shipment_facts_status       ON shipment_facts (status);
CREATE        INDEX idx_shipment_facts_delivered_at ON shipment_facts (delivered_at);

-- ------------------------------------------------------------
-- disaster_summaries
-- ------------------------------------------------------------
CREATE TABLE disaster_summaries (
    disaster_id              UUID          PRIMARY KEY,
    disaster_name            VARCHAR(200)  NOT NULL,
    type                     VARCHAR(20)   NOT NULL,
    severity_level           VARCHAR(15)   NOT NULL,
    status                   VARCHAR(15)   NOT NULL,
    total_requests_created   BIGINT        NOT NULL DEFAULT 0,
    total_requests_delivered BIGINT        NOT NULL DEFAULT 0,
    total_donations_linked   BIGINT        NOT NULL DEFAULT 0,
    total_monetary_donated   NUMERIC(20,2) NOT NULL DEFAULT 0,
    declared_at              TIMESTAMPTZ   NOT NULL,
    closed_at                TIMESTAMPTZ,
    created_at               TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

-- ------------------------------------------------------------
-- processed_events (idempotency guard)
-- ------------------------------------------------------------
CREATE TABLE processed_events (
    event_id     UUID         PRIMARY KEY,
    event_type   VARCHAR(100) NOT NULL,
    processed_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
