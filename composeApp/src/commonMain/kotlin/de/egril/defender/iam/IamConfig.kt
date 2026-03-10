package de.egril.defender.iam

/**
 * Configuration for the IAM (Keycloak) integration.
 *
 * The base URL is read from a platform-specific source:
 * - Web: `window.keycloakConfig.url` (set in index.html, overridable per deployment)
 * - Desktop: `iam.base.url` system property or `IAM_BASE_URL` environment variable
 * - Android/iOS: `IAM_BASE_URL` environment variable (default: localhost)
 */
object IamConfig {
    const val REALM = "egril"
    const val CLIENT_ID = "defender-of-egril"

    val baseUrl: String get() = getIamBaseUrl()
    val authUrl: String get() = "$baseUrl/realms/$REALM/protocol/openid-connect/auth"
    val tokenUrl: String get() = "$baseUrl/realms/$REALM/protocol/openid-connect/token"
    val logoutUrl: String get() = "$baseUrl/realms/$REALM/protocol/openid-connect/logout"
}

/** Returns the Keycloak base URL for the current platform. */
expect fun getIamBaseUrl(): String
