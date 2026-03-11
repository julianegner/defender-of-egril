package de.egril.defender.iam

/**
 * Reads the Keycloak base URL from the JVM environment.
 * Resolution order:
 * 1. `iam.base.url` Java system property  (e.g. -Diam.base.url=https://…)
 * 2. `IAM_BASE_URL` environment variable
 * 3. `http://localhost:8081` (local development default)
 */
internal fun readIamBaseUrlFromJvmEnv(): String =
    System.getProperty("iam.base.url")
        ?: System.getenv("IAM_BASE_URL")
        ?: "http://localhost:8081"

/**
 * Parses the `preferred_username` (or `sub` as fallback) from a JWT access token
 * without requiring any third-party library.
 */
internal fun extractUsernameFromJwt(token: String): String? {
    return try {
        val payload = token.split(".").getOrNull(1) ?: return null
        val padded = payload + "=".repeat((4 - payload.length % 4) % 4)
        val decoded = java.util.Base64.getUrlDecoder().decode(padded).toString(Charsets.UTF_8)
        extractJsonStringValue(decoded, "preferred_username")
            ?: extractJsonStringValue(decoded, "sub")
    } catch (_: Exception) {
        null
    }
}

/** Extracts a simple string field from a flat JSON object without a full parser. */
internal fun extractJsonStringValue(json: String, key: String): String? =
    Regex("\"${Regex.escape(key)}\"\\s*:\\s*\"([^\"]+)\"").find(json)?.groupValues?.get(1)
