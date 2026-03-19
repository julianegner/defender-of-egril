#!/usr/bin/env bash
# setup-keycloak-cli-client.sh
#
# Creates the 'defender-of-egril-cli' Keycloak client via the Admin REST API.
#
# When to run this:
#   - After pulling a change that adds the client to egril-realm.json (Keycloak
#     only imports realm JSON on first start, so the client won't appear in an
#     already-running instance automatically).
#   - You do NOT need this if you start Keycloak fresh:
#       docker compose down -v && docker compose up -d
#
# Usage:
#   bash scripts/api-client/setup-keycloak-cli-client.sh
#
# Environment variables (all optional, shown with defaults):
#   KEYCLOAK_URL            http://localhost:8081
#   KEYCLOAK_REALM          egril
#   KEYCLOAK_ADMIN          admin
#   KEYCLOAK_ADMIN_PASSWORD admin

set -euo pipefail

KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8081}"
REALM="${KEYCLOAK_REALM:-egril}"
ADMIN_USER="${KEYCLOAK_ADMIN:-admin}"
ADMIN_PASSWORD="${KEYCLOAK_ADMIN_PASSWORD:-admin}"

echo "=== Setup: defender-of-egril-cli Keycloak client ==="
echo "Keycloak : $KEYCLOAK_URL"
echo "Realm    : $REALM"
echo ""

# ---------------------------------------------------------------------------
# Step 1 – Obtain an admin access token from the master realm
# Use --data-urlencode so that special characters in credentials are safe.
# ---------------------------------------------------------------------------
echo "Obtaining admin token..."
TOKEN_BODY=$(curl -s -X POST \
  "$KEYCLOAK_URL/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  --data-urlencode "grant_type=password" \
  --data-urlencode "client_id=admin-cli" \
  --data-urlencode "username=$ADMIN_USER" \
  --data-urlencode "password=$ADMIN_PASSWORD")

# Extract the access token – try python3 first, fall back to grep
ADMIN_TOKEN=$(echo "$TOKEN_BODY" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('access_token',''))" 2>/dev/null || true)
if [ -z "$ADMIN_TOKEN" ]; then
  ADMIN_TOKEN=$(echo "$TOKEN_BODY" | grep -o '"access_token":"[^"]*"' | cut -d'"' -f4 || true)
fi

if [ -z "$ADMIN_TOKEN" ]; then
  echo "ERROR: Failed to obtain admin token. Response:"
  echo "$TOKEN_BODY"
  exit 1
fi
echo "Admin token obtained."
echo ""

# ---------------------------------------------------------------------------
# Step 2 – Check whether the client already exists
# ---------------------------------------------------------------------------
echo "Checking if 'defender-of-egril-cli' already exists..."
EXISTING=$(curl -s \
  "$KEYCLOAK_URL/admin/realms/$REALM/clients?clientId=defender-of-egril-cli" \
  -H "Authorization: Bearer $ADMIN_TOKEN")

# An empty JSON array [] means the client does not exist yet
if echo "$EXISTING" | python3 -c "import sys,json; data=json.load(sys.stdin); sys.exit(0 if data else 1)" 2>/dev/null; then
  echo "Client 'defender-of-egril-cli' already exists. Nothing to do."
  exit 0
fi

# ---------------------------------------------------------------------------
# Step 3 – Create the client
# ---------------------------------------------------------------------------
echo "Creating 'defender-of-egril-cli' client..."
HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST \
  "$KEYCLOAK_URL/admin/realms/$REALM/clients" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "defender-of-egril-cli",
    "name": "Defender of Egril CLI",
    "description": "Public client for developer scripts and API testing. Allows Resource Owner Password Credentials flow.",
    "enabled": true,
    "publicClient": true,
    "standardFlowEnabled": false,
    "directAccessGrantsEnabled": true,
    "protocol": "openid-connect"
  }')

if [ "$HTTP_STATUS" = "201" ]; then
  echo "SUCCESS: Client 'defender-of-egril-cli' created."
  echo ""
  echo "You can now run the API client script:"
  echo "  kotlinc -script scripts/api-client/backend-api-client.main.kts"
else
  echo "ERROR: Failed to create client (HTTP $HTTP_STATUS)"
  exit 1
fi
