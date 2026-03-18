#!/usr/bin/env bash
# Builds the Keycloak theme JAR (if not already present) then starts the local
# Keycloak instance via docker compose.
#
# Usage:
#   ./keycloak/local/start.sh           # start in the foreground
#   ./keycloak/local/start.sh -d        # start in detached (background) mode
#
# Prerequisites: Node.js 18+, npm 9+, Docker with Compose v2

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
THEME_DIR="$REPO_ROOT/keycloak/keycloak-theme"
JAR_DIR="$THEME_DIR/dist_keycloak"

# Build the theme JAR if it is missing or the source is newer.
if [ ! -d "$JAR_DIR" ] || [ -z "$(ls "$JAR_DIR"/*.jar 2>/dev/null)" ]; then
  echo ">>> Building Keycloak theme..."
  cd "$THEME_DIR"
  npm install
  npm run build
  cd "$REPO_ROOT"
  echo ">>> Theme built."
else
  echo ">>> Theme JARs already present, skipping build."
  echo "    (Delete $JAR_DIR to force a rebuild.)"
fi

echo ">>> Starting Keycloak..."
exec docker compose -f "$SCRIPT_DIR/docker-compose.yml" up "$@"
