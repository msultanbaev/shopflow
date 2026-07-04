CREATE SCHEMA IF NOT EXISTS orders;

CREATE TABLE orders.orders (
                               id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               user_id         UUID NOT NULL,
                               status          VARCHAR(50) NOT NULL DEFAULT 'PENDING',
                               total_amount    NUMERIC(19,2) NOT NULL,
                               created_at      TIMESTAMP NOT NULL DEFAULT now(),
                               updated_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE orders.order_items (
                                    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                    order_id    UUID NOT NULL REFERENCES orders.orders(id),
                                    product_id  UUID NOT NULL,
                                    quantity    INTEGER NOT NULL,
                                    price       NUMERIC(19,2) NOT NULL
);

CREATE TABLE orders.outbox (
                               id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               aggregate_id    UUID NOT NULL,
                               event_type      VARCHAR(100) NOT NULL,
                               payload         TEXT NOT NULL,
                               sent            BOOLEAN NOT NULL DEFAULT false,
                               created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_outbox_sent ON orders.outbox(sent);
CREATE INDEX idx_orders_user ON orders.orders(user_id);
CREATE INDEX idx_orders_status ON orders.orders(status);