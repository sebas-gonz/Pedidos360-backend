CREATE TABLE orders (
                        id BIGSERIAL PRIMARY KEY,
                        uuid UUID NOT NULL UNIQUE,
                        customer_id VARCHAR(255) NOT NULL,
                        total_order_amount DECIMAL(19, 2) NOT NULL,
                        price_currency VARCHAR(10) NOT NULL,
                        order_status VARCHAR(50) NOT NULL,
                        version INTEGER NOT NULL DEFAULT 0,
                        created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE order_items (
                             id BIGSERIAL PRIMARY KEY,
                             uuid UUID NOT NULL UNIQUE,
                             product_uuid UUID NOT NULL,
                             order_id BIGINT NOT NULL,
                             quantity INTEGER NOT NULL,
                             total_item_amount DECIMAL(19, 2) NOT NULL,
                             price_currency VARCHAR(10) NOT NULL,
                             version INTEGER NOT NULL DEFAULT 0,
                             created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                             updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                             CONSTRAINT fk_order_id FOREIGN KEY (order_id) REFERENCES orders(id)
);