-- liquibase formatted sql

-- changeset defender:018-install-uuid
ALTER TABLE events
    ADD COLUMN install_uuid UUID;

CREATE INDEX idx_events_install_uuid ON events (install_uuid);

ALTER TABLE player_feedback
    ADD COLUMN install_uuid UUID;

CREATE INDEX idx_player_feedback_install_uuid ON player_feedback (install_uuid);
