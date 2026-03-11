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

    /** Logs out and clears the local auth state. */
    fun logout() {
        performPlatformLogout()
        state.value = IamState()
    }

    /** Returns the current Bearer access token, or null if not authenticated. */
    fun getToken(): String? = state.value.token

    /** Returns true if the user is currently authenticated. */
    fun isAuthenticated(): Boolean = state.value.isAuthenticated
}

/** Starts the platform-specific OAuth2/OIDC login flow. */
internal expect fun startPlatformLogin()

/** Performs the platform-specific logout. */
internal expect fun performPlatformLogout()

/**
 * Performs platform-specific IAM initialisation, e.g. restoring an existing
 * Keycloak session on the web. Called once when the app starts.
 * Implementations may suspend to wait for an asynchronous SDK.
 */
expect suspend fun initPlatformIam()
