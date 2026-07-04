CREATE SCHEMA IF NOT EXISTS inventory;

CREATE TABLE inventory.stock (
                                 id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                 product_id  UUID NOT NULL UNIQUE,
                                 quantity    INTEGER NOT NULL DEFAULT 0,
                                 reserved    INTEGER NOT NULL DEFAULT 0,
                                 version     BIGINT NOT NULL DEFAULT 0,
                                 updated_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE inventory.processed_events (
                                            event_id    UUID PRIMARY KEY,
                                            processed_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_stock_product ON inventory.stock(product_id);