package de.egril.defender.iam

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

/**
 * Singleton service for IAM (Keycloak) authentication.
 *
 * Login is entirely optional. The game can be played without being logged in.
 * When logged in, the Keycloak username is displayed and the access token is
 * attached to API calls as an optional Bearer token.
 */
object IamService {

    /** Reactive authentication state – observe this in Compose via `by IamService.state`. */
    val state: MutableState<IamState> = mutableStateOf(IamState())

    /**
     * Set to `true` while the PKCE browser redirect is in progress (desktop) so that
     * the UI can display a "Waiting for browser login…" indicator.
     */
    val loginInProgress: MutableState<Boolean> = mutableStateOf(false)

    /** Initiates the login flow (platform-specific). */
    fun login() {
        loginInProgress.value = true
        startPlatformLogin()
    }

    /** Logs out and clears the local auth state. Also performs a full SSO logout in the browser. */
    fun logout() {
        performPlatformLogout()
        state.value = IamState()
    }

    /**
     * Clears local auth state only, without performing a browser-based SSO logout.
     * Use when switching players so that the Keycloak callback port is not occupied
     * by a logout listener when the subsequent login flow starts.
     */
    fun logoutLocal() {
        performPlatformLogoutLocal()
        state.value = IamState()
        loginInProgress.value = false
    }

    /**
     * Terminates the Keycloak session server-side via an HTTP POST (backchannel logout)
     * and clears local auth state.
     *
     * Unlike [logout], this does **not** open a browser window or bind the PKCE callback
     * port, so there is no risk of a port conflict with a subsequent login flow.
     *
     * - **Desktop**: HTTP POST to the Keycloak logout endpoint with the stored refresh token.
     *   The server-side session is revoked immediately. Falls back to local-only cleanup if
     *   no refresh token is available or the request fails.
     * - **Android / iOS**: clears in-memory state only (no server socket / port concerns).
     * - **WASM**: delegates to the Keycloak.js SSO logout (browser redirect back to origin).
     *
     * Use this when switching to a player with no linked remote account, so that a
     * subsequent manual login does not silently re-authenticate as the previous user.
     */
    fun logoutBackchannel() {
        performPlatformLogoutBackchannel()
        state.value = IamState()
        loginInProgress.value = false
    }

    /** Returns the current Bearer access token, or null if not authenticated. */
    fun getToken(): String? = state.value.token

    /** Returns true if the user is currently authenticated. */
    fun isAuthenticated(): Boolean = state.value.isAuthenticated
}

/** Starts the platform-specific OAuth2/OIDC login flow. */
internal expect fun startPlatformLogin()

/** Performs the platform-specific logout (including SSO browser redirect). */
internal expect fun performPlatformLogout()

/**
 * Clears only the in-memory token state without performing a browser-based SSO logout.
 * This avoids occupying the PKCE callback port, which would block a subsequent login.
 */
internal expect fun performPlatformLogoutLocal()

/**
 * Terminates the Keycloak session server-side and clears in-memory token state.
 * Does NOT open a browser window or bind the PKCE callback port.
 *
 * - **Desktop**: HTTP POST to the Keycloak logout endpoint with the stored refresh token.
 * - **Android / iOS**: clears in-memory state only (same as [performPlatformLogoutLocal]).
 * - **WASM**: delegates to the Keycloak.js SSO logout (browser redirect back to origin).
 */
internal expect fun performPlatformLogoutBackchannel()

/**
 * Performs platform-specific IAM initialisation, e.g. restoring an existing
 * Keycloak session on the web. Called once when the app starts.
 * Implementations may suspend to wait for an asynchronous SDK.
 */
expect suspend fun initPlatformIam()
