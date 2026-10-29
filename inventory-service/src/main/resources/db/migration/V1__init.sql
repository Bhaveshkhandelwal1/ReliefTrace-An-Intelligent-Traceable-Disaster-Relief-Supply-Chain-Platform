-- -------------------------------------------------------------
-- DisasterReliefNet :: Inventory Service Database Migration V1
-- -------------------------------------------------------------

-- Warehouses table
CREATE TABLE warehouses (
    warehouse_id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    street VARCHAR(500),
    city VARCHAR(100),
    state VARCHAR(100),
    country CHAR(2),
    zip VARCHAR(20),
    capacity_cbm NUMERIC(10,2),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_warehouses_active ON warehouses(active);

-- Items table
CREATE TABLE items (
    item_id UUID PRIMARY KEY,
    warehouse_id UUID NOT NULL REFERENCES warehouses(warehouse_id),
    name VARCHAR(200) NOT NULL,
    category VARCHAR(20) NOT NULL,
    unit VARCHAR(20) NOT NULL,
    quantity_on_hand NUMERIC(12,3) NOT NULL DEFAULT 0 CHECK (quantity_on_hand >= 0),
    quantity_reserved NUMERIC(12,3) NOT NULL DEFAULT 0 CHECK (quantity_reserved >= 0),
    expires_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT chk_items_reserved_lte_on_hand CHECK (quantity_reserved <= quantity_on_hand)
);

CREATE INDEX idx_items_warehouse_id ON items(warehouse_id);
CREATE INDEX idx_items_category ON items(category);
CREATE INDEX idx_items_expires_at ON items(expires_at);

-- Reservations table
CREATE TABLE reservations (
    reservation_id UUID PRIMARY KEY,
    item_id UUID NOT NULL REFERENCES items(item_id),
    allocation_id UUID NOT NULL UNIQUE,
    quantity NUMERIC(12,3) NOT NULL CHECK (quantity > 0),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','CONFIRMED','RELEASED','CONSUMED')),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_reservations_item_id ON reservations(item_id);
CREATE INDEX idx_reservations_allocation_id ON reservations(allocation_id);
CREATE INDEX idx_reservations_status ON reservations(status);

-- Stock adjustments table (audit log for manual adjustments and donations)
CREATE TABLE stock_adjustments (
    adjustment_id UUID PRIMARY KEY,
    item_id UUID NOT NULL REFERENCES items(item_id),
    adjustment NUMERIC(12,3) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    adjusted_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_stock_adjustments_item_id ON stock_adjustments(item_id);

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
