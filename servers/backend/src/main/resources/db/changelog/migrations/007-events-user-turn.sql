-- liquibase formatted sql

-- changeset defender:007-events-user-turn
-- Add user_name (Keycloak username of the authenticated player) and turn_number to the events table
ALTER TABLE events
    ADD COLUMN user_name   VARCHAR(255),
    ADD COLUMN turn_number INTEGER;
