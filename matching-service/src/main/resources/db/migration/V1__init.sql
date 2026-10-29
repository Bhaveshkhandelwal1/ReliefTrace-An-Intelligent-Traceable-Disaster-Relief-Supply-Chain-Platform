-- -------------------------------------------------------------
-- DisasterReliefNet :: Matching Service Database Migration V1
-- -------------------------------------------------------------

-- Matched Requests table: keeps track of verified requests awaiting matching and prioritization
CREATE TABLE matched_requests (
    request_id UUID PRIMARY KEY,
    receiver_id UUID NOT NULL,
    disaster_id UUID NOT NULL,
    category VARCHAR(30) NOT NULL,
    quantity_requested NUMERIC(12,3) NOT NULL,
    quantity_allocated NUMERIC(12,3) NOT NULL DEFAULT 0.000,
    unit VARCHAR(20) NOT NULL,
    urgency_level VARCHAR(20) NOT NULL,
    household_size INT NOT NULL DEFAULT 1,
    delivery_street VARCHAR(500),
    delivery_city VARCHAR(100),
    delivery_state VARCHAR(100),
    delivery_country CHAR(2),
    delivery_zip VARCHAR(20),
    priority_score DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    verified_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_matched_requests_disaster_id ON matched_requests(disaster_id);
CREATE INDEX idx_matched_requests_category ON matched_requests(category);
CREATE INDEX idx_matched_requests_priority ON matched_requests(priority_score DESC);
CREATE INDEX idx_matched_requests_status ON matched_requests(status);

-- Allocations table
CREATE TABLE allocations (
    allocation_id UUID PRIMARY KEY,
    request_id UUID NOT NULL,
    receiver_id UUID NOT NULL,
    disaster_id UUID NOT NULL,
    item_id UUID NOT NULL,
    warehouse_id UUID NOT NULL,
    quantity NUMERIC(12,3) NOT NULL CHECK (quantity > 0),
    unit VARCHAR(20) NOT NULL DEFAULT 'units',
    status VARCHAR(30) NOT NULL DEFAULT 'SUGGESTED' CHECK (status IN ('SUGGESTED','PENDING_APPROVAL','APPROVED','REJECTED','RESERVATION_FAILED')),
    delivery_street VARCHAR(500),
    delivery_city VARCHAR(100),
    delivery_state VARCHAR(100),
    delivery_country CHAR(2),
    delivery_zip VARCHAR(20),
    priority_score DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    approved_by VARCHAR(255),
    notes VARCHAR(500),
    rejection_reason VARCHAR(500),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_allocations_request_id ON allocations(request_id);
CREATE INDEX idx_allocations_item_id ON allocations(item_id);
CREATE INDEX idx_allocations_status ON allocations(status);
CREATE INDEX idx_allocations_disaster_id ON allocations(disaster_id);

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
