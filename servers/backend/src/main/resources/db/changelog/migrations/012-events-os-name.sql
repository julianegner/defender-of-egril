-- liquibase formatted sql

-- changeset defender:012-events-os-name
-- Add os_name column to the events table
ALTER TABLE events
    ADD COLUMN os_name VARCHAR(255);
