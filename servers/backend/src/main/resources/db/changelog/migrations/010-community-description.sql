-- liquibase formatted sql

-- changeset defender:010-community-description
ALTER TABLE community_files ADD COLUMN description TEXT NOT NULL DEFAULT '';
