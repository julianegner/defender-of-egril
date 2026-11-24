-- liquibase formatted sql

-- changeset defender:019-community-map-zone-images
CREATE TABLE community_map_zone_images (
    community_file_id BIGINT  NOT NULL REFERENCES community_files (id) ON DELETE CASCADE,
    zone_index        INTEGER NOT NULL,
    image             BYTEA   NOT NULL,
    PRIMARY KEY (community_file_id, zone_index)
);
