-- ============================================================
-- Transport Service DB Schema — V1
-- ============================================================

-- Extension for UUID generation (Flyway runs as superuser in dev)
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ------------------------------------------------------------
-- drivers
-- ------------------------------------------------------------
CREATE TABLE drivers (
    driver_id          UUID         PRIMARY KEY,
    user_id            VARCHAR(255) NOT NULL,
    first_name         VARCHAR(100) NOT NULL,
    last_name          VARCHAR(100) NOT NULL,
    email              VARCHAR(255) NOT NULL,
    phone              VARCHAR(20)  NOT NULL,
    license_number     VARCHAR(50)  NOT NULL,
    available          BOOLEAN      NOT NULL DEFAULT TRUE,
    active_shipment_id UUID,
    version            BIGINT       NOT NULL DEFAULT 0,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted_at         TIMESTAMPTZ
);

CREATE UNIQUE INDEX idx_drivers_user_id  ON drivers (user_id)  WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX idx_drivers_email    ON drivers (email)     WHERE deleted_at IS NULL;
CREATE        INDEX idx_drivers_available ON drivers (available) WHERE deleted_at IS NULL;

-- ------------------------------------------------------------
-- vehicles
-- ------------------------------------------------------------
CREATE TABLE vehicles (
    vehicle_id   UUID        PRIMARY KEY,
    driver_id    UUID        NOT NULL REFERENCES drivers (driver_id),
    plate_number VARCHAR(20) NOT NULL,
    type         VARCHAR(20) NOT NULL CHECK (type IN ('MOTORCYCLE','VAN','TRUCK','BOAT','HELICOPTER')),
    capacity_kg  NUMERIC(10,2),
    capacity_cbm NUMERIC(10,2),
    active       BOOLEAN     NOT NULL DEFAULT TRUE,
    version      BIGINT      NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at   TIMESTAMPTZ
);

CREATE UNIQUE INDEX idx_vehicles_plate_number ON vehicles (plate_number) WHERE deleted_at IS NULL;
CREATE        INDEX idx_vehicles_driver_id    ON vehicles (driver_id);
CREATE        INDEX idx_vehicles_active       ON vehicles (active) WHERE deleted_at IS NULL;

-- ------------------------------------------------------------
-- shipments
-- ------------------------------------------------------------
CREATE TABLE shipments (
    shipment_id            UUID         PRIMARY KEY,
    allocation_id          UUID         NOT NULL,
    reservation_id         UUID         NOT NULL,
    driver_id              UUID         REFERENCES drivers (driver_id),
    vehicle_id             UUID         REFERENCES vehicles (vehicle_id),
    receiver_id            UUID         NOT NULL,
    pickup_street          VARCHAR(500),
    pickup_city            VARCHAR(100),
    pickup_state           VARCHAR(100),
    pickup_country         CHAR(2),
    pickup_zip             VARCHAR(20),
    delivery_street        VARCHAR(500),
    delivery_city          VARCHAR(100),
    delivery_state         VARCHAR(100),
    delivery_country       CHAR(2),
    delivery_zip           VARCHAR(20),
    scheduled_pickup_at    TIMESTAMPTZ,
    picked_up_at           TIMESTAMPTZ,
    delivered_at           TIMESTAMPTZ,
    status                 VARCHAR(20)  NOT NULL DEFAULT 'CREATED'
                               CHECK (status IN ('CREATED','ASSIGNED','PICKED_UP','IN_TRANSIT','DELIVERED','DISPUTED','FAILED','CANCELLED')),
    proof_of_delivery_url  VARCHAR(500),
    recipient_signature    VARCHAR(500),
    dispute_reason         VARCHAR(1000),
    dispute_evidence_url   VARCHAR(500),
    version                BIGINT       NOT NULL DEFAULT 0,
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted_at             TIMESTAMPTZ
);

CREATE UNIQUE INDEX idx_shipments_allocation_id ON shipments (allocation_id) WHERE deleted_at IS NULL;
CREATE        INDEX idx_shipments_driver_id     ON shipments (driver_id);
CREATE        INDEX idx_shipments_receiver_id   ON shipments (receiver_id);
CREATE        INDEX idx_shipments_status        ON shipments (status);

-- ------------------------------------------------------------
-- shipment_tracking_events
-- ------------------------------------------------------------
CREATE TABLE shipment_tracking_events (
    tracking_event_id UUID         PRIMARY KEY,
    shipment_id       UUID         NOT NULL REFERENCES shipments (shipment_id),
    event_type        VARCHAR(50)  NOT NULL,
    description       VARCHAR(500),
    occurred_at       TIMESTAMPTZ  NOT NULL,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_tracking_events_shipment_id ON shipment_tracking_events (shipment_id);

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
-- processed_events  (idempotency guard)
-- ------------------------------------------------------------
CREATE TABLE processed_events (
    event_id     UUID         PRIMARY KEY,
    event_type   VARCHAR(100) NOT NULL,
    processed_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
