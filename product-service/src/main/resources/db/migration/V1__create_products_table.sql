CREATE SCHEMA IF NOT EXISTS products;

CREATE TABLE products.categories (
                                     id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                     name        VARCHAR(100) NOT NULL UNIQUE,
                                     description TEXT,
                                     created_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE products.products (
                                   id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                   name          VARCHAR(255) NOT NULL,
                                   description   TEXT,
                                   price         NUMERIC(19,2) NOT NULL,
                                   category_id   UUID NOT NULL REFERENCES products.categories(id),
                                   seller_id     UUID NOT NULL,
                                   image_url     VARCHAR(500),
                                   status        VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
                                   created_at    TIMESTAMP NOT NULL DEFAULT now(),
                                   updated_at    TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_products_category ON products.products(category_id);
CREATE INDEX idx_products_seller ON products.products(seller_id);
CREATE INDEX idx_products_status ON products.products(status);