-- -------------------------------------------------------------
-- DisasterReliefNet :: Receiver Service Database Migration V1
-- -------------------------------------------------------------

-- Receivers table
CREATE TABLE receivers (
    receiver_id UUID PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(20),
    disaster_id UUID NOT NULL,
    street VARCHAR(500),
    city VARCHAR(100),
    state VARCHAR(100),
    country CHAR(2),
    zip VARCHAR(20),
    household_size INT NOT NULL CHECK (household_size > 0),
    vulnerability_tags VARCHAR(500),
    verification_status VARCHAR(20) NOT NULL DEFAULT 'UNVERIFIED' CHECK (verification_status IN ('UNVERIFIED','PENDING','VERIFIED','REJECTED')),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_receivers_email ON receivers(email);
CREATE INDEX idx_receivers_disaster_id ON receivers(disaster_id);
CREATE INDEX idx_receivers_verification_status ON receivers(verification_status);

-- Aid Requests table
CREATE TABLE aid_requests (
    request_id UUID PRIMARY KEY,
    receiver_id UUID NOT NULL REFERENCES receivers(receiver_id),
    disaster_id UUID NOT NULL,
    category VARCHAR(30) NOT NULL CHECK (category IN ('FOOD','WATER','MEDICINE','SHELTER','CLOTHING','OTHER')),
    description VARCHAR(1000) NOT NULL,
    quantity_requested NUMERIC(12,3) NOT NULL CHECK (quantity_requested > 0),
    unit VARCHAR(20) NOT NULL,
    urgency_level VARCHAR(20) NOT NULL CHECK (urgency_level IN ('LOW','MEDIUM','HIGH','CRITICAL')),
    delivery_street VARCHAR(500),
    delivery_city VARCHAR(100),
    delivery_state VARCHAR(100),
    delivery_country CHAR(2),
    delivery_zip VARCHAR(20),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','VERIFIED','REJECTED','ALLOCATED','IN_TRANSIT','DELIVERED','CANCELLED')),
    allocation_id UUID,
    dedup_key VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_aid_requests_receiver_id ON aid_requests(receiver_id);
CREATE INDEX idx_aid_requests_disaster_id ON aid_requests(disaster_id);
CREATE INDEX idx_aid_requests_status ON aid_requests(status);
CREATE INDEX idx_aid_requests_urgency ON aid_requests(urgency_level);
CREATE INDEX idx_aid_requests_dedup_key ON aid_requests(dedup_key);

-- Outbox Events table for Transactional Outbox Pattern
CREATE TABLE outbox_events (
    event_id UUID PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    topic VARCHAR(200) NOT NULL,
    payload TEXT NOT NULL,
    published BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    published_at TIMESTAMPTZ
);

CREATE INDEX idx_outbox_events_unpublished ON outbox_events(published, created_at) WHERE published = FALSE;

-- Processed Events table for Idempotent Consumer Pattern
CREATE TABLE processed_events (
    event_id UUID PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
