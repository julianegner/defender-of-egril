-- liquibase formatted sql

-- changeset defender:003-community-files
CREATE TABLE community_files (
    id           BIGSERIAL                NOT NULL,
    user_id      VARCHAR(255)             NOT NULL,
    username     VARCHAR(255)             NOT NULL DEFAULT '',
    file_type    VARCHAR(10)              NOT NULL,
    file_id      VARCHAR(255)             NOT NULL,
    data         TEXT                     NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_community_files PRIMARY KEY (id),
    CONSTRAINT uq_community_files_type_id UNIQUE (file_type, file_id)
);

CREATE INDEX idx_community_files_user_id ON community_files (user_id);
CREATE INDEX idx_community_files_file_type ON community_files (file_type);
