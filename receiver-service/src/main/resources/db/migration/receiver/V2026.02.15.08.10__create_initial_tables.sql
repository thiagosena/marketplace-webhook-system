-- Events Table
CREATE TABLE events
(
    id              UUID PRIMARY KEY             DEFAULT gen_random_uuid(),
    idempotency_key VARCHAR(100) UNIQUE NOT NULL,
    event_type      VARCHAR(100)        NOT NULL,
    order_id        VARCHAR(100)        NOT NULL,
    store_id        VARCHAR(100)        NOT NULL,
    snapshot        JSONB               NULL,
    retry_count     INT                 NOT NULL DEFAULT 0,
    next_retry_at   TIMESTAMP,
    status          VARCHAR(100)        NOT NULL DEFAULT 'SNAPSHOT_PENDING',
    received_at     TIMESTAMP           NOT NULL DEFAULT NOW(),
    processed_at    TIMESTAMP           NULL,
    last_error      TEXT                NULL
);

CREATE INDEX idx_events_status ON events (status);
CREATE INDEX idx_events_idempotency_key ON events (idempotency_key);

CREATE INDEX idx_event_snapshot_pending_next_retry ON events (next_retry_at) WHERE status = 'SNAPSHOT_PENDING';

COMMENT ON TABLE events IS 'Receiver event table';