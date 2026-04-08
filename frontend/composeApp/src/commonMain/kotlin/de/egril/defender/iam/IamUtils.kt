package de.egril.defender.iam

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/** Extracts a simple string field from a flat JSON object without a full parser.
 * Note: does not handle escaped quotes inside string values — sufficient for standard JWT claims. */
internal fun extractJsonStringField(json: String, key: String): String? =
    Regex("\"${Regex.escape(key)}\"\\s*:\\s*\"([^\"]+)\"").find(json)?.groupValues?.get(1)

/**
 * Claims extracted from a JWT access token that are relevant to display in the UI.
 */
internal data class JwtUserClaims(
    val username: String?,
    val email: String?,
    val firstName: String?,
    val lastName: String?
) {
    companion object {
        val empty = JwtUserClaims(null, null, null, null)
    }
}

/**
 * Parses user-facing claims from a JWT access token.
 * Uses the Kotlin stdlib [Base64] so it works on all platforms (JVM, Android, Native, Wasm).
 */
@OptIn(ExperimentalEncodingApi::class)
internal fun parseJwtClaims(token: String): JwtUserClaims {
    return try {
        val payload = token.split(".").getOrNull(1) ?: return JwtUserClaims.empty
        // JWT base64url payloads may omit padding; add it back before decoding.
        val padded = payload + "=".repeat((4 - payload.length % 4) % 4)
        val decoded = Base64.UrlSafe.decode(padded).decodeToString()
        JwtUserClaims(
            username = extractJsonStringField(decoded, "preferred_username")
                ?: extractJsonStringField(decoded, "sub"),
            email = extractJsonStringField(decoded, "email"),
            firstName = extractJsonStringField(decoded, "given_name"),
            lastName = extractJsonStringField(decoded, "family_name")
        )
    } catch (_: Exception) {
        JwtUserClaims.empty
    }
}

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

/**
 * Builds an authenticated [IamState] from a JWT [accessToken], parsing all available user
 * claims (username, email, first/last name).
 *
 * @param accessToken The JWT access token.
 * @param fallbackUsername Used as [IamState.username] when the token contains no usable identity claim.
 */
internal fun buildIamState(accessToken: String, fallbackUsername: String? = null): IamState {
    val claims = parseJwtClaims(accessToken)
    return IamState(
        isAuthenticated = true,
        username = claims.username ?: fallbackUsername ?: "unknown",
        token = accessToken,
        email = claims.email,
        firstName = claims.firstName,
        lastName = claims.lastName
    )
}
