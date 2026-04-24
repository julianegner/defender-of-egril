-- liquibase formatted sql

-- changeset defender:012-events-os
-- Add os_name column to the events table for operating system information
ALTER TABLE events
    ADD COLUMN os_name VARCHAR(255);
