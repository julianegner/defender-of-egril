-- liquibase formatted sql

-- changeset defender:017-events-url
-- Add url column to the events table (web only, APP_STARTED only)
ALTER TABLE events
    ADD COLUMN url VARCHAR(2048);
