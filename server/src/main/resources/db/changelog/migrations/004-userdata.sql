-- liquibase formatted sql

-- changeset defender:004-userdata
CREATE TABLE userdata (
    id         BIGSERIAL                NOT NULL,
    user_id    VARCHAR(255)             NOT NULL,
    data       TEXT                     NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_userdata PRIMARY KEY (id),
    CONSTRAINT uq_userdata_user_id UNIQUE (user_id)
);
