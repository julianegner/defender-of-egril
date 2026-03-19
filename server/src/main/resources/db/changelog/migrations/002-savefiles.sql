-- liquibase formatted sql

-- changeset defender:002-savefiles
CREATE TABLE savefiles (
    id         BIGSERIAL                NOT NULL,
    user_id    VARCHAR(255)             NOT NULL,
    save_id    VARCHAR(255)             NOT NULL,
    data       TEXT                     NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_savefiles PRIMARY KEY (id),
    CONSTRAINT uq_savefiles_user_save UNIQUE (user_id, save_id)
);

CREATE INDEX idx_savefiles_user_id ON savefiles (user_id);
