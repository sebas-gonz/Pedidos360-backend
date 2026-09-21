CREATE TABLE audit_events (
                              id BIGSERIAL PRIMARY KEY,
                              order_uuid UUID NOT NULL,
                              event_type VARCHAR(100) NOT NULL,
                              user_email VARCHAR(255),
                              user_role VARCHAR(50),
                              event_date TIMESTAMP WITH TIME ZONE NOT NULL,
                              details TEXT
);