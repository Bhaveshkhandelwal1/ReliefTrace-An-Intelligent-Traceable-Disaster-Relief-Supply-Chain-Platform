-- ============================================================
-- Notification Service DB Schema — V1
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ------------------------------------------------------------
-- notification_logs
-- ------------------------------------------------------------
CREATE TABLE notification_logs (
    log_id      UUID         PRIMARY KEY,
    user_id     VARCHAR(255) NOT NULL,
    channel     VARCHAR(10)  NOT NULL CHECK (channel IN ('EMAIL','SMS','PUSH')),
    subject     VARCHAR(500),
    body        TEXT         NOT NULL,
    status      VARCHAR(10)  NOT NULL DEFAULT 'PENDING' CHECK (status IN ('SENT','FAILED','PENDING')),
    event_id    UUID,
    event_type  VARCHAR(100),
    sent_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notification_logs_user_id  ON notification_logs (user_id);
CREATE INDEX idx_notification_logs_status   ON notification_logs (status);
CREATE INDEX idx_notification_logs_event_id ON notification_logs (event_id);

-- ------------------------------------------------------------
-- notification_preferences
-- ------------------------------------------------------------
CREATE TABLE notification_preferences (
    user_id       VARCHAR(255) PRIMARY KEY,
    email_enabled BOOLEAN      NOT NULL DEFAULT TRUE,
    sms_enabled   BOOLEAN      NOT NULL DEFAULT FALSE,
    push_enabled  BOOLEAN      NOT NULL DEFAULT FALSE,
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ------------------------------------------------------------
-- processed_events (idempotency guard)
-- ------------------------------------------------------------
CREATE TABLE processed_events (
    event_id     UUID         PRIMARY KEY,
    event_type   VARCHAR(100) NOT NULL,
    processed_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
