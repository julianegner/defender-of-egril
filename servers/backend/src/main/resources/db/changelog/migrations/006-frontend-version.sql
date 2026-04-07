-- liquibase formatted sql

-- changeset defender:006-frontend-version
-- Add frontend version and commit hash to the events table (platform already exists there)
ALTER TABLE events
    ADD COLUMN version_name   VARCHAR(100),
    ADD COLUMN commit_hash    VARCHAR(100),
    ADD COLUMN platform_long  TEXT;

-- Add platform, platform_long, version name, and commit hash to the userdata table
ALTER TABLE userdata
    ADD COLUMN platform       VARCHAR(50),
    ADD COLUMN platform_long  TEXT,
    ADD COLUMN version_name   VARCHAR(100),
    ADD COLUMN commit_hash    VARCHAR(100);

-- Add platform, platform_long, version name, and commit hash to the player_settings table
ALTER TABLE player_settings
    ADD COLUMN platform       VARCHAR(50),
    ADD COLUMN platform_long  TEXT,
    ADD COLUMN version_name   VARCHAR(100),
    ADD COLUMN commit_hash    VARCHAR(100);

-- Add platform, platform_long, version name, and commit hash to the savefiles table
ALTER TABLE savefiles
    ADD COLUMN platform       VARCHAR(50),
    ADD COLUMN platform_long  TEXT,
    ADD COLUMN version_name   VARCHAR(100),
    ADD COLUMN commit_hash    VARCHAR(100);
