-- -------------------------------------------------------------
-- DisasterReliefNet :: Verification Service Database Migration V1
-- -------------------------------------------------------------

-- Verification Cases table
CREATE TABLE verification_cases (
    case_id UUID PRIMARY KEY,
    request_id UUID NOT NULL UNIQUE,
    receiver_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN','UNDER_REVIEW','APPROVED','REJECTED')),
    reviewed_by VARCHAR(255),
    review_notes VARCHAR(1000),
    opened_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    resolved_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_verification_cases_request_id ON verification_cases(request_id);
CREATE INDEX idx_verification_cases_receiver_id ON verification_cases(receiver_id);
CREATE INDEX idx_verification_cases_status ON verification_cases(status);

-- Verification Documents table
CREATE TABLE verification_documents (
    document_id UUID PRIMARY KEY,
    case_id UUID NOT NULL REFERENCES verification_cases(case_id) ON DELETE CASCADE,
    document_type VARCHAR(30) NOT NULL CHECK (document_type IN ('NATIONAL_ID','PROOF_OF_ADDRESS','MEDICAL_CERT','OTHER')),
    file_url VARCHAR(500) NOT NULL,
    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_verification_documents_case_id ON verification_documents(case_id);

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
