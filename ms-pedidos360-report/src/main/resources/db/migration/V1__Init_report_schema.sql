CREATE TABLE report_orders (
                               id BIGSERIAL PRIMARY KEY,
                               order_uuid UUID NOT NULL UNIQUE,
                               status VARCHAR(50) NOT NULL,
                               total_amount DECIMAL(19, 2) NOT NULL,
                               created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                               delivered_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE report_order_items (
                                    id BIGSERIAL PRIMARY KEY,
                                    report_order_id BIGINT NOT NULL,
                                    product_uuid UUID NOT NULL,
                                    quantity INTEGER NOT NULL,
                                    CONSTRAINT fk_report_order FOREIGN KEY (report_order_id) REFERENCES report_orders(id) ON DELETE CASCADE
);