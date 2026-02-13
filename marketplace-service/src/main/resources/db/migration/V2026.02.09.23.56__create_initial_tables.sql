-- Orders Table
CREATE TABLE IF NOT EXISTS orders
(
    id           UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    store_id     VARCHAR(100)   NOT NULL,
    status       VARCHAR(50)    NOT NULL,
    total_amount NUMERIC(10, 2) NOT NULL,
    created_at   TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP      NULL
);

CREATE INDEX IF NOT EXISTS idx_orders_store_id ON orders (store_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders (status);

-- Order Items Table
CREATE TABLE IF NOT EXISTS order_items
(
    id           UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    product_name VARCHAR(255)   NOT NULL,
    quantity     INT            NOT NULL,
    unit_price   NUMERIC(10, 2) NOT NULL,
    discount     NUMERIC(10, 2) NOT NULL DEFAULT 0,
    tax          NUMERIC(10, 2) NOT NULL DEFAULT 0,
    order_id     UUID           NOT NULL,
    CONSTRAINT FK_ORDER_ID FOREIGN KEY (order_id) REFERENCES orders (id)
);

CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON order_items (order_id);

-- Webhooks Table
CREATE TABLE IF NOT EXISTS webhooks
(
    id           UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    store_ids    VARCHAR(100)[] NOT NULL,
    callback_url VARCHAR(500)   NOT NULL,
    active       BOOLEAN        NOT NULL DEFAULT true,
    created_at   TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP      NULL
);

CREATE INDEX IF NOT EXISTS idx_webhooks_store_ids ON webhooks USING GIN (store_ids);

-- Outbox Table (Transactional Outbox Pattern)
CREATE TABLE IF NOT EXISTS outbox_events
(
    id             UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    aggregate_id   VARCHAR(100) NOT NULL,
    aggregate_type VARCHAR(50)  NOT NULL,
    event_type     VARCHAR(100) NOT NULL,
    payload        JSONB        NOT NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    retry_count    INT          NOT NULL DEFAULT 0,
    next_retry_at  TIMESTAMP,
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    processed_at   TIMESTAMP    NULL,
    last_error     TEXT         NULL
);

CREATE INDEX IF NOT EXISTS idx_outbox_status ON outbox_events (status);
CREATE INDEX idx_outbox_next_retry ON outbox_events (next_retry_at) WHERE status = 'PENDING';

COMMENT ON TABLE orders IS 'Marketplace order table';
COMMENT ON TABLE order_items IS 'Items of an order';
COMMENT ON TABLE webhooks IS 'Webhooks registered per store';
COMMENT ON TABLE outbox_events IS 'Events pending publication (Transactional Outbox Pattern)';