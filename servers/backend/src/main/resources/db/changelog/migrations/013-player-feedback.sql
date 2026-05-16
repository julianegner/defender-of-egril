-- liquibase formatted sql

-- changeset defender:013-player-feedback
CREATE TABLE player_feedback (
    id                BIGSERIAL                 NOT NULL,
    feedback_uuid     UUID                      NOT NULL,
    feedback_type     VARCHAR(64)               NOT NULL,
    bug_types         TEXT[]                    NOT NULL DEFAULT '{}',
    message           TEXT                      NOT NULL,
    contact_email     VARCHAR(320),
    source_context    VARCHAR(64),
    platform          VARCHAR(64)               NOT NULL,
    platform_long     TEXT,
    platform_extended TEXT,
    os_name           VARCHAR(255),
    version_name      VARCHAR(64),
    commit_hash       VARCHAR(64),
    user_id           VARCHAR(255),
    user_name         VARCHAR(255),
    game_level_name   VARCHAR(255),
    game_turn_number  INT,
    game_state_json   TEXT,
    game_log          TEXT,
    screenshot_png    BYTEA,
    created_at        TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_player_feedback PRIMARY KEY (id),
    CONSTRAINT uk_player_feedback_uuid UNIQUE (feedback_uuid)
);

CREATE INDEX idx_player_feedback_created_at ON player_feedback (created_at);
CREATE INDEX idx_player_feedback_type ON player_feedback (feedback_type);
