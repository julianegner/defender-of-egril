#!/usr/bin/env bash
# setup-keycloak-cli-client.sh
#
# Creates the 'defender-of-egril-cli' Keycloak client via the Admin REST API.
#
# When to run this:
#   - After the first time you pull this change (the realm JSON is only imported
#     on first Keycloak start, so the new client won't appear automatically in an
#     already-running Keycloak instance).
#   - You do NOT need to run this if you start Keycloak fresh (docker compose down -v
#     followed by docker compose up -d), because then the realm JSON is fully imported.
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
# ---------------------------------------------------------------------------
echo "Obtaining admin token..."
TOKEN_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST \
  "$KEYCLOAK_URL/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password&client_id=admin-cli&username=$(python3 -c "import urllib.parse,sys; print(urllib.parse.quote('$ADMIN_USER'))")&password=$(python3 -c "import urllib.parse,sys; print(urllib.parse.quote('$ADMIN_PASSWORD'))")")

HTTP_STATUS=$(echo "$TOKEN_RESPONSE" | tail -n1)
TOKEN_BODY=$(echo "$TOKEN_RESPONSE" | head -n-1)

if [ "$HTTP_STATUS" != "200" ]; then
  echo "ERROR: Failed to obtain admin token (HTTP $HTTP_STATUS)"
  echo "$TOKEN_BODY"
  exit 1
fi

ADMIN_TOKEN=$(python3 -c "import sys, json; print(json.loads('''$TOKEN_BODY''')['access_token'])" 2>/dev/null || \
  echo "$TOKEN_BODY" | grep -o '"access_token":"[^"]*"' | cut -d'"' -f4)

if [ -z "$ADMIN_TOKEN" ]; then
  echo "ERROR: Could not extract access_token from response"
  echo "$TOKEN_BODY"
  exit 1
fi
echo "Admin token obtained."
echo ""

# ---------------------------------------------------------------------------
# Step 2 – Check if the client already exists
# ---------------------------------------------------------------------------
echo "Checking if 'defender-of-egril-cli' already exists..."
CHECK_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" \
  "$KEYCLOAK_URL/admin/realms/$REALM/clients?clientId=defender-of-egril-cli" \
  -H "Authorization: Bearer $ADMIN_TOKEN")

EXISTING=$(curl -s \
  "$KEYCLOAK_URL/admin/realms/$REALM/clients?clientId=defender-of-egril-cli" \
  -H "Authorization: Bearer $ADMIN_TOKEN")

# If the JSON array is non-empty, the client exists
if echo "$EXISTING" | python3 -c "import sys,json; data=json.load(sys.stdin); sys.exit(0 if data else 1)" 2>/dev/null; then
  echo "Client 'defender-of-egril-cli' already exists. Nothing to do."
  exit 0
fi

# ---------------------------------------------------------------------------
# Step 3 – Create the client
# ---------------------------------------------------------------------------
echo "Creating 'defender-of-egril-cli' client..."
CREATE_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST \
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

HTTP_STATUS=$(echo "$CREATE_RESPONSE" | tail -n1)

if [ "$HTTP_STATUS" = "201" ]; then
  echo "SUCCESS: Client 'defender-of-egril-cli' created."
  echo ""
  echo "You can now run the API client script:"
  echo "  kotlinc -script scripts/api-client/backend-api-client.main.kts"
else
  echo "ERROR: Failed to create client (HTTP $HTTP_STATUS)"
  echo "$CREATE_RESPONSE" | head -n-1
  exit 1
fi
