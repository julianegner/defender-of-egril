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

    /**
     * Keycloak "restart login" URL. Opening this URL in the browser with
     * `skip_logout=false` terminates the current SSO session and presents a
     * completely blank login form (no pre-filled username). Use this when
     * switching to a local player that has no linked remote account, so that
     * a subsequent manual login cannot silently re-authenticate as the previous
     * Keycloak user via an active browser session cookie.
     */
    val restartLoginUrl: String get() = "$baseUrl/realms/$REALM/login-actions/restart"
}

/** Returns the Keycloak base URL for the current platform. */
expect fun getIamBaseUrl(): String
