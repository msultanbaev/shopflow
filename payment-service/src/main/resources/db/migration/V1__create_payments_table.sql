CREATE SCHEMA IF NOT EXISTS payments;

CREATE TABLE payments.payments (
                                   id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                   order_id    UUID NOT NULL UNIQUE,
                                   user_id     UUID NOT NULL,
                                   amount      NUMERIC(19,2) NOT NULL,
                                   status      VARCHAR(50) NOT NULL DEFAULT 'PENDING',
                                   created_at  TIMESTAMP NOT NULL DEFAULT now(),
                                   updated_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE payments.outbox (
                                 id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                 aggregate_id    UUID NOT NULL,
                                 event_type      VARCHAR(100) NOT NULL,
                                 payload         TEXT NOT NULL,
                                 sent            BOOLEAN NOT NULL DEFAULT false,
                                 created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE payments.processed_events (
                                           event_id        UUID PRIMARY KEY,
                                           processed_at    TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_payments_order ON payments.payments(order_id);
CREATE INDEX idx_payments_status ON payments.payments(status);
CREATE INDEX idx_outbox_sent ON payments.outbox(sent) WHERE sent = false;