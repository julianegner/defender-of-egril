-- liquibase formatted sql

-- changeset defender:011-backend-errors
CREATE TABLE backend_errors (
    id          BIGSERIAL                NOT NULL,
    endpoint    VARCHAR(255)             NOT NULL,
    http_method VARCHAR(10)              NOT NULL,
    status_code INT                      NOT NULL,
    message     TEXT                     NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_backend_errors PRIMARY KEY (id)
);

CREATE INDEX idx_backend_errors_created_at ON backend_errors (created_at);
