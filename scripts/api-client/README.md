# Backend API Client – Developer Script

A self-contained **Kotlin script** for interacting with the Defender of Egril backend API.
It authenticates with Keycloak via username/password, then demonstrates every backend endpoint with concrete examples.

## Prerequisites

| Requirement | Notes |
|---|---|
| **Kotlin 1.9+** | Must be on your `PATH`. Install via [SDKMAN](https://sdkman.io/): `sdk install kotlin` |
| **Keycloak** | Default: `http://localhost:8081`. Start with `docker compose up keycloak` from the repo root. |
| **Backend server** | Default: `http://localhost:8080`. Start with `./gradlew :server:run` or `docker compose up backend`. |
| **User account** | A Keycloak account in the `egril` realm. Create one in the Keycloak admin console. |

> **First time setup**: The `defender-of-egril-cli` client is defined in `local-keycloak/egril-realm.json`.
> If Keycloak was already running before this client was added, restart it so the realm is re-imported:
> `docker compose restart keycloak`

## Quick Start

> **Security note**: Passing passwords as command-line arguments may expose them in shell history
> and process listings. Prefer using environment variables (`KEYCLOAK_USER` / `KEYCLOAK_PASSWORD`)
> or a `.env` file loaded into your shell before running the script.

```bash
# Run with username/password as arguments
kotlinc -script scripts/api-client/backend-api-client.main.kts myuser mypassword

# Or using environment variables
export KEYCLOAK_USER=myuser
export KEYCLOAK_PASSWORD=mypassword
kotlinc -script scripts/api-client/backend-api-client.main.kts
```

## Configuration

All settings have sensible defaults for local development.
Override them with environment variables:

| Variable | Default | Description |
|---|---|---|
| `KEYCLOAK_URL` | `http://localhost:8081` | Keycloak base URL |
| `KEYCLOAK_REALM` | `egril` | Keycloak realm name |
| `KEYCLOAK_CLIENT` | `defender-of-egril-cli` | OAuth2 client ID (CLI client with direct access grants) |
| `KEYCLOAK_USER` | *(required)* | Keycloak username |
| `KEYCLOAK_PASSWORD` | *(required)* | Keycloak password |
| `BACKEND_URL` | `http://localhost:8080` | Backend server base URL |

## What the Script Does

The script walks through every backend endpoint in order:

### Step 1 – Obtain a Keycloak access token

Uses the **Resource Owner Password Credentials (ROPC)** flow via the dedicated
`defender-of-egril-cli` client (a public Keycloak client with Direct Access Grants
enabled, separate from the main `defender-of-egril` app client which intentionally
restricts this flow).

```
POST {KEYCLOAK_URL}/realms/{realm}/protocol/openid-connect/token
Content-Type: application/x-www-form-urlencoded

grant_type=password&client_id=defender-of-egril-cli&username=...&password=...&scope=openid
```

### Step 2 – `GET /` (root health-check)

Verifies the backend is reachable. Returns `200 OK` with a plain-text greeting.

```
GET http://localhost:8080/
```

### Step 3 – `POST /api/events` (analytics event)

Logs a game analytics event. Authentication is optional for this endpoint.

```
POST http://localhost:8080/api/events
Authorization: Bearer <token>
Content-Type: application/json

{"event":"script_test","platform":"desktop","levelName":"test_level"}
```

### Step 4 – `POST /api/savefiles` (upload a savefile)

Uploads (or upserts) a savefile for the authenticated user. The `data` field
contains the raw JSON of the save game as a **string value** (JSON-escaped).

```
POST http://localhost:8080/api/savefiles
Authorization: Bearer <token>
Content-Type: application/json

{
  "saveId": "script_test_save",
  "data": "{\"levelId\":\"test_level\",\"comment\":\"Script test save\",\"turnNumber\":5}"
}
```

- Returns `200 OK` on success.
- Returns `401 Unauthorized` if no valid Bearer token is provided.
- If a save with the same `saveId` already exists for this user, it is overwritten (upsert semantics).

### Step 5 – `GET /api/savefiles` (list savefiles)

Returns all savefiles for the authenticated user as a JSON array.

```
GET http://localhost:8080/api/savefiles
Authorization: Bearer <token>
```

**Response format:**

```json
[
  {
    "saveId": "script_test_save",
    "data": "{\"levelId\":\"test_level\",...}",
    "updatedAt": "2024-01-15T10:30:00Z"
  },
  {
    "saveId": "autosave_game",
    "data": "{...}",
    "updatedAt": "2024-01-15T11:45:00Z"
  }
]
```

### Step 6 – `POST /api/savefiles` (upsert – update existing save)

Same as Step 4 but with updated content, demonstrating the upsert behaviour.

---

## Manual cURL Examples

If you prefer `curl` over the Kotlin script:

### Get a token

```bash
TOKEN=$(curl -s -X POST \
  "http://localhost:8081/realms/egril/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password&client_id=defender-of-egril-cli&username=myuser&password=mypassword&scope=openid" \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])")

echo "Token: ${TOKEN:0:40}..."
```

### Upload a savefile

```bash
curl -s -X POST "http://localhost:8080/api/savefiles" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"saveId":"my_save","data":"{\"levelId\":\"level1\",\"turnNumber\":3}"}'
```

### List savefiles

```bash
curl -s -X GET "http://localhost:8080/api/savefiles" \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
```

### Post an analytics event

```bash
curl -s -X POST "http://localhost:8080/api/events" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"event":"level_started","platform":"desktop","levelName":"welcome_to_defender_of_egril"}'
```

---

## Notes

- **Token expiry**: Keycloak access tokens expire after ~5 minutes by default. Re-run the script to obtain a fresh token.
- **ROPC flow**: The Resource Owner Password flow used here requires the `defender-of-egril-cli` client (Direct Access Grants enabled). The main `defender-of-egril` app client intentionally has this disabled. This separation keeps the app client secure while allowing scripts to authenticate with username/password.
- **No signature verification**: The script decodes JWT claims for display only. No signature check is performed.
- **JSON parsing**: The script uses simple regex-based JSON field extraction. This is sufficient for the flat Keycloak JWT payload but will not correctly handle field values that contain escaped quotes. Use a proper JSON library if you need to parse complex savefile content.
