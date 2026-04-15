#!/usr/bin/env bash
# replace-realm-settings.sh
#
# Replaces the Keycloak 'egril' realm settings with the configuration stored in
# servers/keycloak/production/egril-realm.json, while preserving all existing users.
#
# How it works:
#   1. Obtains a short-lived admin token from the Keycloak 'master' realm.
#   2. Downloads the current realm representation and saves it to a timestamped
#      backup file so that you can roll back manually if needed.
#   3. Applies the new realm settings from egril-realm.json using
#      PUT /admin/realms/{realm}.
#
# Why users are preserved:
#   Keycloak stores user accounts independently of the realm representation.
#   The PUT endpoint updates realm settings (token policies, branding, clients,
#   roles, …) but does NOT touch user records.  The source file intentionally
#   contains no "users" array, providing an additional safeguard.
#
# Usage:
#   bash scripts/keycloak/replace-realm-settings.sh
#
# Run from the repository root so that the path to egril-realm.json resolves
# correctly, or set REALM_JSON_FILE to an explicit path.
#
# Environment variables (all optional, shown with defaults):
#   KEYCLOAK_URL            http://localhost:8081
#   KEYCLOAK_REALM          egril
#   KEYCLOAK_ADMIN          admin
#   KEYCLOAK_ADMIN_PASSWORD admin
#   REALM_JSON_FILE         servers/keycloak/production/egril-realm.json
#   BACKUP_DIR              . (current directory)

set -euo pipefail

KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8081}"
REALM="${KEYCLOAK_REALM:-egril}"
ADMIN_USER="${KEYCLOAK_ADMIN:-admin}"
ADMIN_PASSWORD="${KEYCLOAK_ADMIN_PASSWORD:-admin}"
REALM_JSON_FILE="${REALM_JSON_FILE:-servers/keycloak/production/egril-realm.json}"
BACKUP_DIR="${BACKUP_DIR:-.}"

echo "=== Keycloak Realm Settings Replacement ==="
echo "Keycloak   : $KEYCLOAK_URL"
echo "Realm      : $REALM"
echo "Source file: $REALM_JSON_FILE"
echo ""

# ---------------------------------------------------------------------------
# Sanity checks
# ---------------------------------------------------------------------------
if [ ! -f "$REALM_JSON_FILE" ]; then
  echo "ERROR: Realm JSON file not found: $REALM_JSON_FILE"
  echo "  Run this script from the repository root, or set REALM_JSON_FILE."
  exit 1
fi

# ---------------------------------------------------------------------------
# Step 1 – Obtain an admin access token from the master realm
# ---------------------------------------------------------------------------
echo "Step 1: Obtaining admin token from master realm..."
TOKEN_BODY=$(curl -s -X POST \
  "$KEYCLOAK_URL/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  --data-urlencode "grant_type=password" \
  --data-urlencode "client_id=admin-cli" \
  --data-urlencode "username=$ADMIN_USER" \
  --data-urlencode "password=$ADMIN_PASSWORD")

# Extract the access token using standard POSIX tools (grep + cut).
# Keycloak access tokens (JWTs) never contain double-quote characters, so this
# pattern reliably captures the full token value.
ADMIN_TOKEN=$(echo "$TOKEN_BODY" | grep -o '"access_token":"[^"]*"' | cut -d'"' -f4)

if [ -z "$ADMIN_TOKEN" ]; then
  echo "ERROR: Failed to obtain admin token. Response:"
  echo "$TOKEN_BODY"
  exit 1
fi
echo "Admin token obtained."
echo ""

# ---------------------------------------------------------------------------
# Step 2 – Back up the current realm settings
# ---------------------------------------------------------------------------
TIMESTAMP=$(date -u +'%Y%m%d-%H%M%S')
BACKUP_FILE="${BACKUP_DIR}/egril-realm-backup-${TIMESTAMP}.json"

echo "Step 2: Saving current realm settings to backup file..."
HTTP_STATUS=$(curl -s -o "$BACKUP_FILE" -w "%{http_code}" \
  "$KEYCLOAK_URL/admin/realms/$REALM" \
  -H "Authorization: Bearer $ADMIN_TOKEN")

if [ "$HTTP_STATUS" != "200" ]; then
  echo "ERROR: Failed to retrieve current realm settings (HTTP $HTTP_STATUS)."
  echo "  Check that the realm '$REALM' exists and the admin credentials are correct."
  rm -f "$BACKUP_FILE"
  exit 1
fi
echo "Current realm settings saved to: $BACKUP_FILE"
echo ""

# ---------------------------------------------------------------------------
# Step 3 – Apply the new realm settings from the source file
# ---------------------------------------------------------------------------
echo "Step 3: Applying new realm settings from $REALM_JSON_FILE..."
RESPONSE_FILE=$(mktemp)
HTTP_STATUS=$(curl -s -o "$RESPONSE_FILE" -w "%{http_code}" \
  -X PUT \
  "$KEYCLOAK_URL/admin/realms/$REALM" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d "@$REALM_JSON_FILE")

RESPONSE_BODY=$(cat "$RESPONSE_FILE" 2>/dev/null || true)
rm -f "$RESPONSE_FILE"

if [ "$HTTP_STATUS" = "204" ]; then
  echo ""
  echo "SUCCESS: Realm settings have been updated."
  echo "  All user accounts have been preserved."
  echo "  Backup of previous settings: $BACKUP_FILE"
else
  echo "ERROR: Failed to update realm settings (HTTP $HTTP_STATUS)."
  if [ -n "$RESPONSE_BODY" ]; then
    echo "Response:"
    echo "$RESPONSE_BODY"
  fi
  echo ""
  echo "The realm was NOT modified.  Previous settings are still active."
  echo "Backup (may be incomplete): $BACKUP_FILE"
  exit 1
fi
