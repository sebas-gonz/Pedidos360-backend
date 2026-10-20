CREATE TABLE audit_events
(
    id         BIGSERIAL PRIMARY KEY,
    order_uuid UUID         NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    user_email VARCHAR(255),
    user_role  VARCHAR(50),
    details    TEXT,
    uuid       UUID         NOT NULL UNIQUE,
    version    INTEGER      NOT NULL        DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE     DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE     DEFAULT CURRENT_TIMESTAMP
);