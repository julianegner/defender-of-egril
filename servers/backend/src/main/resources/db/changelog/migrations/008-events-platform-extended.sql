-- liquibase formatted sql

-- changeset defender:008-events-platform-extended
-- Add platform_extended column to the events table for detailed OS/browser information
ALTER TABLE events
    ADD COLUMN platform_extended VARCHAR(512);
