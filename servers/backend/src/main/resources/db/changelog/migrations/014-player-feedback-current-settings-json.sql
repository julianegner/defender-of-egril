-- liquibase formatted sql

-- changeset defender:014-player-feedback-current-settings-json
ALTER TABLE player_feedback
    ADD COLUMN current_settings_json TEXT;
