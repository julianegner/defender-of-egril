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

    /**
     * Client ID used by the desktop app for Device Authorization Grant (RFC 8628).
     * This is a separate Keycloak client that has device auth enabled but does not
     * enforce PKCE, which is incompatible with the device authorization endpoint.
     * An audience mapper on this client adds "defender-of-egril" to the access token
     * so the backend accepts it without modification.
     */
    const val DESKTOP_CLIENT_ID = "defender-of-egril-desktop"

    val baseUrl: String get() = getIamBaseUrl()
    val authUrl: String get() = "$baseUrl/realms/$REALM/protocol/openid-connect/auth"
    val tokenUrl: String get() = "$baseUrl/realms/$REALM/protocol/openid-connect/token"
    val logoutUrl: String get() = "$baseUrl/realms/$REALM/protocol/openid-connect/logout"

    /**
     * Device Authorization endpoint (RFC 8628). Used by the desktop login flow so that
     * the user can complete login on a phone or second device without needing a browser
     * on the machine running the game (e.g. Steam Deck gaming mode).
     */
    val deviceAuthUrl: String get() = "$baseUrl/realms/$REALM/protocol/openid-connect/auth/device"

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
