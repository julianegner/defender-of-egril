# Production Deployment

This directory contains the Docker Compose files used to deploy the three
production Hetzner vServers.

## Server Overview

| Directory | Server | Service | Public IP | Private IP |
|-----------|--------|---------|-----------|-----------|
| `db/`      | ubuntu-8gb-nbg1-1 | PostgreSQL 16 | 178.104.64.170 | 10.0.0.2 |
| `keycloak/` | ubuntu-8gb-nbg1-2 | Keycloak 24 | 178.104.79.60 | 10.0.0.3 |
| `backend/`  | ubuntu-8gb-nbg1-3 | Backend (Ktor) | 178.104.84.83 | 10.0.0.4 |

Keycloak and the backend both connect to the database over the shared Hetzner
private network using the DB server's private IP **10.0.0.2**.
The backend connects to Keycloak over the private network using **10.0.0.3**.

## Deployment via GitHub Actions

Three `workflow_dispatch` workflows are available in `.github/workflows/`:

| Workflow | File | Deploys |
|----------|------|---------|
| Deploy Database | `deploy-db.yml` | PostgreSQL on ubuntu-8gb-nbg1-1 |
| Deploy Keycloak | `deploy-keycloak.yml` | Keycloak on ubuntu-8gb-nbg1-2 |
| Deploy Backend | `deploy-backend.yml` | Backend on ubuntu-8gb-nbg1-3 |

Run them in order on the first deployment: DB → Keycloak → Backend.

## Required GitHub Secrets

Add the following secrets to the repository
(**Settings → Secrets and variables → Actions**):

| Secret | Description |
|--------|-------------|
| `PROD_SSH_PRIVATE_KEY` | SSH private key for all three production servers. Add the corresponding public key to `~/.ssh/authorized_keys` on each server. |
| `PROD_DB_PASSWORD` | Password for the `defender` PostgreSQL user. Used by all three services. |
| `PROD_KEYCLOAK_ADMIN_PASSWORD` | Password for the Keycloak `admin` account. |

All server IPs and inter-service URLs are hardcoded in the workflows and are
not required as secrets.

## First-Time Setup on Each Server

Before running the workflows for the first time, ensure Docker (with
Compose v2) is installed on each server:

```bash
# On each server (as root):
apt-get update && apt-get install -y docker.io
mkdir -p /opt/defender-of-egril
```

The workflows create the necessary subdirectories under
`/opt/defender-of-egril/` automatically.

## Security Notes

- PostgreSQL (port 5432) is bound to `10.0.0.2` only – not accessible from
  the public internet.  Ensure the Hetzner firewall allows port 5432 only
  from within the private network.
- Keycloak runs with `KC_HTTP_ENABLED=true` and no built-in TLS.  For a
  production-grade setup place a TLS-terminating reverse proxy (e.g. nginx or
  Caddy) in front of Keycloak.
- The backend Docker image is published to the GitHub Container Registry
  (`ghcr.io`).  The image visibility follows the repository visibility.
