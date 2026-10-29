-- -------------------------------------------------------------
-- DisasterReliefNet :: Disaster Service Database Migration V1
-- -------------------------------------------------------------

-- Disasters table
CREATE TABLE disasters (
    disaster_id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    type VARCHAR(20) NOT NULL CHECK (type IN ('FLOOD','EARTHQUAKE','HURRICANE','WILDFIRE','DROUGHT','CONFLICT','OTHER')),
    severity_level VARCHAR(15) NOT NULL CHECK (severity_level IN ('MINOR','MODERATE','SEVERE','CATASTROPHIC')),
    description VARCHAR(2000),
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    radius_km DOUBLE PRECISION,
    region_name VARCHAR(200),
    country CHAR(2),
    estimated_affected_people BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(15) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','MONITORING','CLOSED')),
    occurred_at TIMESTAMPTZ NOT NULL,
    closed_at TIMESTAMPTZ,
    closure_reason VARCHAR(1000),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_disasters_status ON disasters(status);
CREATE INDEX idx_disasters_type ON disasters(type);
CREATE INDEX idx_disasters_occurred_at ON disasters(occurred_at);
CREATE INDEX idx_disasters_country ON disasters(country);

-- Disaster updates table
CREATE TABLE disaster_updates (
    update_id UUID PRIMARY KEY,
    disaster_id UUID NOT NULL REFERENCES disasters(disaster_id),
    title VARCHAR(200) NOT NULL,
    content VARCHAR(5000) NOT NULL,
    source VARCHAR(200),
    posted_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_disaster_updates_disaster_id ON disaster_updates(disaster_id);

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
