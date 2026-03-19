-- liquibase formatted sql

-- changeset defender:005-settings
CREATE TABLE player_settings (
    id         BIGSERIAL                NOT NULL,
    user_id    VARCHAR(255)             NOT NULL,
    data       TEXT                     NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_player_settings PRIMARY KEY (id),
    CONSTRAINT uq_player_settings_user_id UNIQUE (user_id)
);
