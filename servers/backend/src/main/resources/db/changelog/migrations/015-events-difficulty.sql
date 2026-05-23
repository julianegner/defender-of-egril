-- liquibase formatted sql

-- changeset defender:015-events-difficulty
-- Add difficulty column to the events table
ALTER TABLE events
    ADD COLUMN difficulty VARCHAR(32);
