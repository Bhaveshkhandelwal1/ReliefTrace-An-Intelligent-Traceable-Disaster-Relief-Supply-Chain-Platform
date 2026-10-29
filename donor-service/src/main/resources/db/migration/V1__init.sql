-- -------------------------------------------------------------
-- DisasterReliefNet :: Donor Service Database Migration V1
-- -------------------------------------------------------------

-- Donors table
CREATE TABLE donors (
    donor_id UUID PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(20),
    donor_type VARCHAR(20) NOT NULL DEFAULT 'INDIVIDUAL' CHECK (donor_type IN ('INDIVIDUAL','CORPORATE','PHARMACY','NGO')),
    street VARCHAR(500),
    city VARCHAR(100),
    state VARCHAR(100),
    country CHAR(2),
    zip VARCHAR(20),
    tax_id VARCHAR(50),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','SUSPENDED','DELETED')),
    total_donated_amount NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    impact_deliveries_count BIGINT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_donors_email ON donors(email);
CREATE INDEX idx_donors_status ON donors(status);
CREATE INDEX idx_donors_type ON donors(donor_type);

-- Donations table
CREATE TABLE donations (
    donation_id UUID PRIMARY KEY,
    donor_id UUID NOT NULL REFERENCES donors(donor_id),
    donation_type VARCHAR(20) NOT NULL CHECK (donation_type IN ('MONETARY','IN_KIND')),
    amount NUMERIC(15,2),
    currency CHAR(3),
    item_description VARCHAR(500),
    quantity_kg NUMERIC(10,3),
    disaster_id UUID,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','PLEDGED','CONFIRMED','RECEIVED','REJECTED','CANCELLED','PAID')),
    notes VARCHAR(1000),
    receipt_number VARCHAR(100),
    receipt_issued_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_donations_donor_id ON donations(donor_id);
CREATE INDEX idx_donations_disaster_id ON donations(disaster_id);
CREATE INDEX idx_donations_status ON donations(status);
CREATE INDEX idx_donations_receipt_number ON donations(receipt_number);

-- Donation Items line table (for itemized in-kind donations and pledges)
CREATE TABLE donation_items (
    item_line_id UUID PRIMARY KEY,
    donation_id UUID NOT NULL REFERENCES donations(donation_id),
    item_name VARCHAR(200) NOT NULL,
    category VARCHAR(30) NOT NULL,
    quantity NUMERIC(12,3) NOT NULL CHECK (quantity > 0),
    unit VARCHAR(30) NOT NULL,
    estimated_value NUMERIC(15,2),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_donation_items_donation_id ON donation_items(donation_id);

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
