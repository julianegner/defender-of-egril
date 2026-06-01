-- liquibase formatted sql

-- changeset defender:016-crash-reports
CREATE TABLE crash_reports (
    id                BIGSERIAL                NOT NULL,
    crash_uuid        UUID                     NOT NULL,
    error_type        VARCHAR(512)             NOT NULL,
    error_message     TEXT,
    stack_trace       TEXT,
    game_log          TEXT,
    settings_json     TEXT,
    platform          VARCHAR(64)              NOT NULL,
    platform_long     TEXT,
    platform_extended TEXT,
    os_name           VARCHAR(255),
    version_name      VARCHAR(64),
    commit_hash       VARCHAR(64),
    user_id           VARCHAR(255),
    user_name         VARCHAR(255),
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_crash_reports PRIMARY KEY (id),
    CONSTRAINT uk_crash_reports_uuid UNIQUE (crash_uuid)
);

CREATE INDEX idx_crash_reports_created_at ON crash_reports (created_at);
CREATE INDEX idx_crash_reports_error_type ON crash_reports (error_type);
