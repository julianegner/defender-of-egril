-- liquibase formatted sql

-- changeset defender:001-initial-schema
CREATE TABLE events (
    id         BIGSERIAL                NOT NULL,
    event_type VARCHAR(50)              NOT NULL,
    platform   VARCHAR(50)              NOT NULL,
    level_name VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_events PRIMARY KEY (id)
);
