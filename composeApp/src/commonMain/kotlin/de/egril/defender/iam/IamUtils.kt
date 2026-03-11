package de.egril.defender.iam

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/** Extracts a simple string field from a flat JSON object without a full parser.
 * Note: does not handle escaped quotes inside string values — sufficient for standard JWT claims. */
internal fun extractJsonStringField(json: String, key: String): String? =
    Regex("\"${Regex.escape(key)}\"\\s*:\\s*\"([^\"]+)\"").find(json)?.groupValues?.get(1)

/**
 * Parses the `preferred_username` (or `sub` as fallback) from a JWT access token.
 * Uses the Kotlin stdlib [Base64] so it works on all platforms (JVM, Android, Native, Wasm).
 */
@OptIn(ExperimentalEncodingApi::class)
internal fun parseJwtUsername(token: String): String? {
    return try {
        val payload = token.split(".").getOrNull(1) ?: return null
        // JWT base64url payloads may omit padding; add it back before decoding.
        val padded = payload + "=".repeat((4 - payload.length % 4) % 4)
        val decoded = Base64.UrlSafe.decode(padded).decodeToString()
        extractJsonStringField(decoded, "preferred_username")
            ?: extractJsonStringField(decoded, "sub")
    } catch (_: Exception) {
        null
    }
}
