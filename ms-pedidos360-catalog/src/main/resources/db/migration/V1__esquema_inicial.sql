CREATE TABLE products
(
    id             BIGSERIAL PRIMARY KEY,
    uuid           UUID           NOT NULL UNIQUE,
    product_name   VARCHAR(255)   NOT NULL,
    description    TEXT,
    price          DECIMAL(19, 2) NOT NULL,
    stock          BIGINT         NOT NULL,
    price_currency VARCHAR(10)    NOT NULL,
    tags           TEXT[],
    images_url     TEXT[],
    version        INTEGER        NOT NULL  DEFAULT 0,
    created_at     TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);