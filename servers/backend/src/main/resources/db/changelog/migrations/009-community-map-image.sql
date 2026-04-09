-- liquibase formatted sql

-- changeset defender:009-community-map-image
ALTER TABLE community_files ADD COLUMN map_image BYTEA;
