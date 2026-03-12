#!/usr/bin/env kotlin
/**
 * Defender of Egril – Backend API Client Script
 *
 * A self-contained Kotlin script that:
 *   1. Obtains an access token from Keycloak via the Resource Owner Password
 *      Credentials (ROPC) flow (username + password).
 *   2. Demonstrates every backend API endpoint with concrete examples.
 *
 * Requirements:
 *   - Kotlin 1.9+ (kotlinc / Kotlin scripting support)
 *   - A running Keycloak instance (default: http://localhost:8081)
 *   - A running backend server (default: http://localhost:8080)
 *   - A valid Keycloak user account
 *
 * Usage:
 *   ./backend-api-client.main.kts [username] [password]
 *
 * Or set environment variables:
 *   export KEYCLOAK_USER=myuser
 *   export KEYCLOAK_PASSWORD=mypassword
 *   ./backend-api-client.main.kts
 *
 * Environment variables (all optional, shown with defaults):
 *   KEYCLOAK_URL      http://localhost:8081
 *   KEYCLOAK_REALM    egril
 *   KEYCLOAK_CLIENT   defender-of-egril-cli   (dedicated CLI client with direct access grants)
 *   KEYCLOAK_USER     (required – or pass as first argument)
 *   KEYCLOAK_PASSWORD (required – or pass as second argument)
 *   BACKEND_URL       http://localhost:8080
 */

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

// ---------------------------------------------------------------------------
// Configuration
// ---------------------------------------------------------------------------

val keycloakUrl  = System.getenv("KEYCLOAK_URL")     ?: "http://localhost:8081"
val realm        = System.getenv("KEYCLOAK_REALM")    ?: "egril"
val clientId     = System.getenv("KEYCLOAK_CLIENT")   ?: "defender-of-egril-cli"
val backendUrl   = System.getenv("BACKEND_URL")       ?: "http://localhost:8080"

val username = args.getOrNull(0) ?: System.getenv("KEYCLOAK_USER")
    ?: error("Username required: pass as first argument or set KEYCLOAK_USER env variable")
val password = args.getOrNull(1) ?: System.getenv("KEYCLOAK_PASSWORD")
    ?: error("Password required: pass as second argument or set KEYCLOAK_PASSWORD env variable")

val tokenUrl = "$keycloakUrl/realms/$realm/protocol/openid-connect/token"

// ---------------------------------------------------------------------------
// Step 1 – Obtain access token from Keycloak
// ---------------------------------------------------------------------------

println("=== Step 1: Obtain Keycloak access token ===")
println("Token URL : $tokenUrl")
println("Username  : $username")

val tokenResponse = httpPost(
    url = tokenUrl,
    body = buildString {
        append("grant_type=password")
        append("&client_id=${URLEncoder.encode(clientId, "UTF-8")}")
        append("&username=${URLEncoder.encode(username, "UTF-8")}")
        append("&password=${URLEncoder.encode(password, "UTF-8")}")
        append("&scope=openid")
    },
    contentType = "application/x-www-form-urlencoded",
    token = null
)

if (tokenResponse.status !in 200..299) {
    System.err.println("ERROR: Failed to obtain token (HTTP ${tokenResponse.status})")
    System.err.println(tokenResponse.body)
    System.exit(1)
}

val accessToken = extractJsonField(tokenResponse.body, "access_token")
    ?: error("No access_token in token response")

println("Token obtained successfully (${accessToken.length} chars)")
println()

// Decode and print key JWT claims for convenience
val claims = decodeJwtPayload(accessToken)
println("JWT claims:")
println("  sub               : ${extractJsonField(claims, "sub") ?: "(not present)"}")
println("  preferred_username: ${extractJsonField(claims, "preferred_username") ?: "(not present)"}")
println("  email             : ${extractJsonField(claims, "email") ?: "(not present)"}")
println("  given_name        : ${extractJsonField(claims, "given_name") ?: "(not present)"}")
println("  family_name       : ${extractJsonField(claims, "family_name") ?: "(not present)"}")
println()

// ---------------------------------------------------------------------------
// Step 2 – GET /  (health-check / root endpoint)
// ---------------------------------------------------------------------------

println("=== Step 2: GET / (root health-check) ===")
val rootResponse = httpGet("$backendUrl/", accessToken)
println("Status : ${rootResponse.status}")
// println("Body   : ${rootResponse.body}")

// Verify this is really the Ktor backend and not, e.g., the webpack dev server
// (which also runs on port 8080 and returns Express-style 404s for /api/* routes).
if (rootResponse.status != 200 || !rootResponse.body.contains("Defender of Egril Backend")) {
    System.err.println()
    System.err.println("ERROR: Backend server check failed.")
    System.err.println("Expected: HTTP 200 with body containing 'Defender of Egril Backend'")
    System.err.println("Got     : HTTP ${rootResponse.status} – '${rootResponse.body.take(120)}'")
    System.err.println()
    System.err.println("Possible causes:")
    System.err.println("  1. The backend server is not running. Start it with:")
    System.err.println("       docker compose up -d --build backend   (from the repo root)")
    System.err.println("     or: ./gradlew :server:run")
    System.err.println("  2. Another service (e.g. the WASM dev server) is occupying port 8080.")
    System.err.println("     Stop it, or point this script at a different URL:")
    System.err.println("       BACKEND_URL=http://localhost:8090 kotlinc -script scripts/api-client/backend-api-client.main.kts")
    System.err.println("  3. The backend built from an older image. Rebuild with:")
    System.err.println("       docker compose up -d --build backend")
    System.exit(1)
}
println()

// ---------------------------------------------------------------------------
// Step 3 – POST /api/events  (analytics event)
// ---------------------------------------------------------------------------

println("=== Step 3: POST /api/events (analytics event) ===")
val eventBody = """{"event":"script_test","platform":"desktop","levelName":"test_level"}"""
val eventResponse = httpPost(
    url = "$backendUrl/api/events",
    body = eventBody,
    contentType = "application/json",
    token = accessToken
)
println("Status : ${eventResponse.status}")
println("Body   : ${eventResponse.body}")
println()

// ---------------------------------------------------------------------------
// Step 4 – POST /api/savefiles  (upload a savefile)
// ---------------------------------------------------------------------------

println("=== Step 4: POST /api/savefiles (upload savefile) ===")

// Example minimal savefile JSON – in production this is the full game state
val exampleSaveJson = """{"levelId":"test_level","comment":"Script test save","turnNumber":5,"coins":150,"healthPoints":10}"""
val uploadBody = """{"saveId":"script_test_save","data":${escapeJsonString(exampleSaveJson)}}"""

val uploadResponse = httpPost(
    url = "$backendUrl/api/savefiles",
    body = uploadBody,
    contentType = "application/json",
    token = accessToken
)
println("Status : ${uploadResponse.status}")
println("Body   : ${uploadResponse.body}")
println()

// ---------------------------------------------------------------------------
// Step 5 – GET /api/savefiles  (list all savefiles for this user)
// ---------------------------------------------------------------------------

println("=== Step 5: GET /api/savefiles (list savefiles) ===")
val listResponse = httpGet("$backendUrl/api/savefiles", accessToken)
println("Status : ${listResponse.status}")
if (listResponse.status in 200..299) {
    val body = listResponse.body
    // Pretty-print: count saves and show save IDs
    val saveCount = body.split("\"saveId\"").size - 1
    println("Savefiles returned: $saveCount")
    println("Raw JSON (first 500 chars): ${body.take(500)}${if (body.length > 500) "…" else ""}")
} else {
    println("Body   : ${listResponse.body}")
}
println()

// ---------------------------------------------------------------------------
// Step 6 – POST /api/savefiles  (upsert / overwrite the same save)
// ---------------------------------------------------------------------------

println("=== Step 6: POST /api/savefiles (upsert – overwrite existing save) ===")
val updatedSaveJson = """{"levelId":"test_level","comment":"Updated by script","turnNumber":10,"coins":300,"healthPoints":8}"""
val upsertBody = """{"saveId":"script_test_save","data":${escapeJsonString(updatedSaveJson)}}"""

val upsertResponse = httpPost(
    url = "$backendUrl/api/savefiles",
    body = upsertBody,
    contentType = "application/json",
    token = accessToken
)
println("Status : ${upsertResponse.status}")
println("Body   : ${upsertResponse.body}")
println()

// Verify update was applied
println("=== Verify: GET /api/savefiles after upsert ===")
val verifyResponse = httpGet("$backendUrl/api/savefiles", accessToken)
println("Status : ${verifyResponse.status}")
val saveCount2 = verifyResponse.body.split("\"saveId\"").size - 1
println("Savefiles returned: $saveCount2")
println()

println("=== All examples completed successfully ===")

// ---------------------------------------------------------------------------
// Helper functions
// ---------------------------------------------------------------------------

data class HttpResponse(val status: Int, val body: String)

fun httpGet(url: String, token: String?): HttpResponse {
    val conn = URL(url).openConnection() as HttpURLConnection
    conn.requestMethod = "GET"
    if (token != null) conn.setRequestProperty("Authorization", "Bearer $token")
    conn.connectTimeout = 15_000
    conn.readTimeout    = 15_000
    return readResponse(conn)
}

fun httpPost(url: String, body: String, contentType: String, token: String?): HttpResponse {
    val conn = URL(url).openConnection() as HttpURLConnection
    conn.requestMethod = "POST"
    conn.doOutput = true
    conn.setRequestProperty("Content-Type", contentType)
    if (token != null) conn.setRequestProperty("Authorization", "Bearer $token")
    conn.connectTimeout = 15_000
    conn.readTimeout    = 15_000
    conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
    return readResponse(conn)
}

fun readResponse(conn: HttpURLConnection): HttpResponse {
    val status = conn.responseCode
    val body = try {
        (if (status in 200..299) conn.inputStream else conn.errorStream)
            ?.bufferedReader()?.readText() ?: ""
    } catch (_: Exception) { "" }
    conn.disconnect()
    return HttpResponse(status, body)
}

/** Extracts a simple string field from a flat JSON object without a full parser. */
fun extractJsonField(json: String, key: String): String? =
    Regex(""""${Regex.escape(key)}"\s*:\s*"([^"]+)"""").find(json)?.groupValues?.get(1)

/** Decodes the payload section of a JWT (base64url) and returns the JSON string. */
fun decodeJwtPayload(token: String): String {
    val payload = token.split(".").getOrNull(1) ?: return "{}"
    val padded = payload + "=".repeat((4 - payload.length % 4) % 4)
    return java.util.Base64.getUrlDecoder().decode(padded).toString(Charsets.UTF_8)
}

/** Escapes a string for safe embedding as a JSON string value. */
fun escapeJsonString(s: String): String = buildString {
    append('"')
    for (c in s) {
        when (c) {
            '"'  -> append("\\\"")
            '\\' -> append("\\\\")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> append(c)
        }
    }
    append('"')
}
