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

## SSH Key Setup (Required Before First Run)

The workflows authenticate to the production servers using an SSH key pair.
You must set this up once from your **local machine** before running any workflow.

### 1. Generate a dedicated deploy key pair (on your local machine)

```bash
ssh-keygen -t ed25519 -C "github-actions-deploy" -f ~/.ssh/defender_deploy_key -N ""
```

This creates two files **on your local machine**:
- `~/.ssh/defender_deploy_key` — **private key** (goes into GitHub Secrets)
- `~/.ssh/defender_deploy_key.pub` — **public key** (goes onto each server)

### 2. Add the private key to GitHub Secrets (on your local machine)

**Important:** Private SSH keys contain newlines that are required for the key to be valid.
Copying by visually selecting text in a terminal can silently strip those newlines, making
the key unusable. Use a clipboard command instead:

```bash
# macOS – copies the key to your clipboard:
pbcopy < ~/.ssh/defender_deploy_key

# Linux (requires xclip):
xclip -sel clip < ~/.ssh/defender_deploy_key

# If neither tool is available, print the key and copy from the terminal
# (select from the BEGIN line to the END line, inclusive):
cat ~/.ssh/defender_deploy_key
```

Open **Settings → Secrets and variables → Actions → New repository secret**, set the
name to `PROD_SSH_PRIVATE_KEY`, paste the clipboard contents into the value field,
and save.

The stored value must look exactly like this (with all internal newlines preserved):

```
-----BEGIN OPENSSH PRIVATE KEY-----
b3BlbnNzaC1rZXktdjEAAAAA...
...AAAA
-----END OPENSSH PRIVATE KEY-----
```

> **Tip:** After saving the secret, run the "Deploy Database" workflow — the
> `🔍 Validate SSH key` step will print `✅ Key is valid` if the newlines are
> intact, or give a specific error message if they are not.

### 3. Add the public key to each server

Log in to each of the three production servers (using existing root access,
e.g. via the Hetzner Console) and append the public key to the root user's
`authorized_keys`. Paste the output of `cat ~/.ssh/defender_deploy_key.pub`
from your local machine directly into each server:

```bash
# On each server (as root), paste the full public key line:
mkdir -p ~/.ssh
chmod 700 ~/.ssh
echo "ssh-ed25519 AAAA... github-actions-deploy" >> ~/.ssh/authorized_keys
chmod 600 ~/.ssh/authorized_keys
```

Repeat this on all three servers:
- `178.104.64.170` (DB)
- `178.104.79.60` (Keycloak)
- `178.104.84.83` (Backend)

## First-Time Setup on Each Server

Before running the workflows for the first time, ensure Docker (with
Compose v2) is installed on each server and the SSH key is configured
(see [SSH Key Setup](#ssh-key-setup-required-before-first-run) above):

```bash
# On each server (as root):
apt-get update && apt-get install -y docker.io
```

**DB server only** (`/opt/postgres` is created automatically by the workflow):
```bash
mkdir -p /opt/postgres
```

**Keycloak server only** — obtain a TLS certificate before the first deploy:
```bash
apt-get install -y certbot
mkdir -p /opt/keycloak/providers
certbot certonly --standalone -d sso.julianegner.de
```

**Backend server** (created automatically by the workflow):
```bash
mkdir -p /opt/defender-of-egril/backend
```

## Security Notes

- PostgreSQL (port 5432) is bound to `10.0.0.2` only – not accessible from
  the public internet.  Ensure the Hetzner firewall allows port 5432 only
  from within the private network.
- Keycloak is not exposed directly; nginx (in the same Docker Compose) terminates
  TLS on ports 80/443 and proxies to Keycloak's internal HTTP port.  The TLS
  certificate for `sso.julianegner.de` is obtained via certbot on the host and
  mounted read-only into the nginx container.
- The backend Docker image is published to the GitHub Container Registry
  (`ghcr.io`).  The image visibility follows the repository visibility.

## Troubleshooting

### `ssh: unable to authenticate, attempted methods [none publickey]`

The SSH handshake reached the server but the server rejected the key.
Work through this checklist in order:

**1. Verify the public key is on every server**

The most common cause: the public key that matches `PROD_SSH_PRIVATE_KEY` has
never been added to `~/.ssh/authorized_keys` on the server.

Derive the public key from the private key you put in the secret (run this
on your local machine where the key file exists):

```bash
ssh-keygen -y -f ~/.ssh/defender_deploy_key
# → prints something like: ssh-ed25519 AAAA... github-actions-deploy
```

Then check what is in the server's `authorized_keys`:

```bash
# Log into the server (e.g. via Hetzner Console) and run:
cat ~/.ssh/authorized_keys
```

If the line printed by `ssh-keygen -y` is not in `authorized_keys`, add it:

```bash
echo "ssh-ed25519 AAAA... github-actions-deploy" >> ~/.ssh/authorized_keys
chmod 600 ~/.ssh/authorized_keys
```

**2. Confirm the key fingerprint shown by the workflow matches the server**

The "Deploy Database" workflow prints the fingerprint of `PROD_SSH_PRIVATE_KEY`
during the `🔍 Validate SSH key` step.  To see what fingerprint the server
trusts, run on the server:

```bash
ssh-keygen -l -f ~/.ssh/authorized_keys
```

If the fingerprints don't match, the wrong private key was stored in the secret.

**3. Check that `PROD_SSH_PRIVATE_KEY` has the correct format**

The secret must be the **complete** private key file, including the header and
footer lines **and all internal newlines**.

If the `🔍 Validate SSH key` step prints `⚠️  PROD_SSH_PRIVATE_KEY is NOT a valid private key`,
the most likely cause is that newlines inside the key were lost when the key was
copied manually (e.g. by selecting text in a terminal window).

**Fix:** Use a clipboard command to copy the key — this preserves all newlines:

```bash
# macOS:
pbcopy < ~/.ssh/defender_deploy_key

# Linux (requires xclip):
xclip -sel clip < ~/.ssh/defender_deploy_key
```

Then open **Settings → Secrets and variables → Actions**, delete the existing
`PROD_SSH_PRIVATE_KEY` secret, create a new one with the same name, and paste
the clipboard contents.  A valid key looks like this in the secret value field:

```
-----BEGIN OPENSSH PRIVATE KEY-----
b3BlbnNzaC1rZXktdjEAAAAA...
...AAAA
-----END OPENSSH PRIVATE KEY-----
```

The `🔍 Validate SSH key` step also prints `Line count` — a valid ed25519 key
typically has around 8–10 lines.  If the line count is 1, newlines were lost.

**4. Check server-side SSH settings**

If everything above looks correct, log in to the server and check:

```bash
# Ensure authorized_keys has strict permissions (must be 600 or 640):
stat ~/.ssh/authorized_keys
chmod 600 ~/.ssh/authorized_keys

# Check SSH daemon logs for the exact rejection reason:
journalctl -u ssh --since "10 minutes ago" | tail -30
```

