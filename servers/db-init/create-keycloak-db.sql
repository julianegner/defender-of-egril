-- Creates a dedicated database for Keycloak IAM.
-- This script is executed by the postgres container on first startup
-- (via /docker-entrypoint-initdb.d) after the primary "defenderofegril"
-- database has been created by the POSTGRES_DB environment variable.
CREATE DATABASE keycloak;
GRANT ALL PRIVILEGES ON DATABASE keycloak TO defender;
