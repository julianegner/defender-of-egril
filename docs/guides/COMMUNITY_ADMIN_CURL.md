# Community Map/Level Admin – curl / bash Guide

This guide explains how to **list** and **delete** community maps and levels stored on
the server using `curl` and standard shell tools.

No UI is provided for admin operations. All management is done via the backend REST API.

---

## Prerequisites

| Requirement | Notes |
|---|---|
| `curl` | Available on Linux, macOS, Windows (WSL or Git Bash) |
| `jq` | JSON processor – install with `apt install jq`, `brew install jq`, etc. |
| Running Keycloak | Default: `http://localhost:8081` (or your production URL) |
| Running backend | Default: `http://localhost:8080` (or your production URL) |
| Account with `community_admin` role | See [Granting the Role](#granting-the-community_admin-role) below |

---

## Granting the `community_admin` Role

The `community_admin` role is a **client role** on the `defender-of-egril` Keycloak client.
Only users explicitly granted this role can delete community files.

### Via the Keycloak Admin Console (recommended)

1. Open the Keycloak admin console:
   - Local: `http://localhost:8081` → log in as `admin` / `admin`
   - Production: `https://<your-keycloak-host>`
2. Navigate to **Realm: egril** → **Users** → select the user to promote.
3. Open the **Role mappings** tab.
4. Under **Client roles**, select `defender-of-egril` from the dropdown.
5. Assign the `community_admin` role.
6. Save.

> **Note for new deployments**: The `community_admin` role is automatically created
> when Keycloak imports `egril-realm.json` on first start.
> If Keycloak was already running before the role was added, recreate the container:
> ```bash
> docker compose down -v && docker compose up -d
> ```
> Or use the Keycloak Admin REST API to create the role manually (see below).

### Create the role via Admin REST API (if missing)

```bash
KEYCLOAK_URL="http://localhost:8081"
ADMIN_TOKEN=$(curl -s -X POST "$KEYCLOAK_URL/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password&client_id=admin-cli&username=admin&password=admin" \
  | jq -r '.access_token')

# Get the internal ID of the defender-of-egril client
CLIENT_UUID=$(curl -s "$KEYCLOAK_URL/admin/realms/egril/clients?clientId=defender-of-egril" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  | jq -r '.[0].id')

# Create the community_admin role on that client
curl -s -X POST "$KEYCLOAK_URL/admin/realms/egril/clients/$CLIENT_UUID/roles" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"community_admin","description":"Allows listing and deleting community maps and levels via the admin API"}'
echo "Role created (HTTP 201 = success)"
```

---

## Step 1 – Obtain an Access Token

Use the `defender-of-egril-cli` Keycloak client (Resource Owner Password Credentials flow).
Replace `ADMIN_USER` and `ADMIN_PASSWORD` with the credentials of a user who has
the `community_admin` role.

```bash
KEYCLOAK_URL="http://localhost:8081"     # Change for production
BACKEND_URL="http://localhost:8080"      # Change for production
REALM="egril"
CLIENT_ID="defender-of-egril-cli"
ADMIN_USER="your-admin-user@example.com"
ADMIN_PASSWORD="your-password"

TOKEN=$(curl -s -X POST \
  "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password&client_id=$CLIENT_ID&username=$ADMIN_USER&password=$ADMIN_PASSWORD&scope=openid" \
  | jq -r '.access_token')

echo "Token obtained: ${TOKEN:0:40}..."
```

> **Security note**: Avoid passing passwords in shell history. Use environment variables or
> a `.env` file instead: `source .env && TOKEN=$(curl ...)`.

---

## Step 2 – List Community Files

The list endpoint is **public** (no token required) but you can optionally pass the token.

### List all community files (maps and levels)

```bash
curl -s "$BACKEND_URL/api/community/files" | jq .
```

### List only maps

```bash
curl -s "$BACKEND_URL/api/community/files?fileType=MAP" | jq .
```

### List only levels

```bash
curl -s "$BACKEND_URL/api/community/files?fileType=LEVEL" | jq .
```

**Example response:**

```json
[
  {
    "fileType": "MAP",
    "fileId": "my-cool-map",
    "authorUsername": "mapmaker@example.com",
    "authorId": "8f3e2a1b-...",
    "updatedAt": "2024-06-01T12:00:00Z",
    "uploadedAt": "2024-05-20T09:30:00Z"
  },
  {
    "fileType": "LEVEL",
    "fileId": "hard-challenge",
    "authorUsername": "leveldesigner@example.com",
    "authorId": "c7d4e9f0-...",
    "updatedAt": "2024-06-02T08:15:00Z",
    "uploadedAt": "2024-06-02T08:15:00Z"
  }
]
```

Use the `fileType` and `fileId` fields to identify files for deletion.

---

## Step 3 – Delete a Community File

The delete endpoint requires a Bearer token with the `community_admin` role.

```bash
# Delete a map
curl -s -X DELETE "$BACKEND_URL/api/community/files/MAP/my-cool-map" \
  -H "Authorization: Bearer $TOKEN"

# Delete a level
curl -s -X DELETE "$BACKEND_URL/api/community/files/LEVEL/hard-challenge" \
  -H "Authorization: Bearer $TOKEN"
```

**Response codes:**

| Code | Meaning |
|---|---|
| `200 OK` | File deleted successfully |
| `401 Unauthorized` | No Bearer token provided |
| `403 Forbidden` | Token valid but user lacks the `community_admin` role |
| `404 Not Found` | No file with the given `fileType` + `fileId` combination exists |
| `400 Bad Request` | `fileType` is not `MAP` or `LEVEL` |
| `503 Service Unavailable` | Database not available |

---

## Complete Example Script

The following script lists all community files and then deletes a specific one:

```bash
#!/usr/bin/env bash
set -euo pipefail

KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8081}"
BACKEND_URL="${BACKEND_URL:-http://localhost:8080}"
REALM="${KEYCLOAK_REALM:-egril}"
CLIENT_ID="${KEYCLOAK_CLIENT:-defender-of-egril-cli}"
ADMIN_USER="${KEYCLOAK_USER:?Set KEYCLOAK_USER}"
ADMIN_PASSWORD="${KEYCLOAK_PASSWORD:?Set KEYCLOAK_PASSWORD}"

# 1. Get token
TOKEN=$(curl -s -X POST \
  "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password&client_id=$CLIENT_ID&username=$ADMIN_USER&password=$ADMIN_PASSWORD&scope=openid" \
  | jq -r '.access_token')

echo "=== All community files ==="
curl -s "$BACKEND_URL/api/community/files" | jq .

echo ""
echo "=== Deleting MAP 'my-cool-map' ==="
HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE \
  "$BACKEND_URL/api/community/files/MAP/my-cool-map" \
  -H "Authorization: Bearer $TOKEN")

if [ "$HTTP_STATUS" = "200" ]; then
  echo "Deleted successfully."
elif [ "$HTTP_STATUS" = "404" ]; then
  echo "File not found (already deleted?)."
else
  echo "Unexpected status: $HTTP_STATUS"
  exit 1
fi
```

Save as e.g. `delete-community-map.sh`, make executable with `chmod +x`, then run:

```bash
export KEYCLOAK_USER=admin@example.com
export KEYCLOAK_PASSWORD=secret
bash delete-community-map.sh
```

---

## Download a File Before Deleting

To retrieve the raw JSON data of a file before deletion (e.g. for backup):

```bash
# Download map data
curl -s "$BACKEND_URL/api/community/files/MAP/my-cool-map" \
  | jq -r '.data' \
  > my-cool-map-backup.json

# Download level data
curl -s "$BACKEND_URL/api/community/files/LEVEL/hard-challenge" \
  | jq -r '.data' \
  > hard-challenge-backup.json
```

---

## Notes

- **Token expiry**: Keycloak access tokens expire after ~5 minutes. Re-run the token
  acquisition step if you see `401 Unauthorized` after a pause.
- **The `defender-of-egril-cli` client** must have Direct Access Grants enabled (it does
  by default). See `scripts/api-client/setup-keycloak-cli-client.sh` if the client is missing.
- **Deletion is permanent**: There is no recycle bin. Download the file data first if
  you need a backup (see section above).
- **Anyone can list** community files without authentication. Only deletion requires the
  `community_admin` role.
