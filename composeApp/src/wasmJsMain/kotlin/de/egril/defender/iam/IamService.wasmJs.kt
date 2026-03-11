@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package de.egril.defender.iam

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
// JS interop – all interaction with the Keycloak JS adapter goes through here
// ---------------------------------------------------------------------------

@JsFun("() => { return (window.keycloakConfig && window.keycloakConfig.url) || 'http://localhost:8081'; }")
private external fun jsGetKeycloakUrl(): String

/** Returns true once the Keycloak JS adapter has finished its init() call. */
@JsFun("() => { return window._kcReady === true; }")
private external fun jsIsKcReady(): Boolean

@JsFun("() => { return window._kcAuthenticated === true; }")
private external fun jsIsKcAuthenticated(): Boolean

@JsFun("() => { return window._kcUsername || null; }")
private external fun jsGetKcUsername(): String?

@JsFun("() => { return window._kcToken || null; }")
private external fun jsGetKcToken(): String?

@JsFun("() => { if (window._keycloak) { window._keycloak.login(); } }")
private external fun jsKcLogin()

@JsFun("() => { if (window._keycloak) { window._keycloak.logout({ redirectUri: window.location.origin }); } }")
private external fun jsKcLogout()

// ---------------------------------------------------------------------------
// Platform implementations
// ---------------------------------------------------------------------------

actual fun getIamBaseUrl(): String = jsGetKeycloakUrl()

internal actual fun startPlatformLogin() {
    jsKcLogin()
}

internal actual fun performPlatformLogout() {
    jsKcLogout()
}

// Background scope for the token-sync loop. Lives for the duration of the app.
private val iamScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

/** How often (ms) to check whether Keycloak.js has silently refreshed the token. */
private const val KC_TOKEN_SYNC_INTERVAL_MS = 30_000L

private const val KC_INIT_POLL_INTERVAL_MS = 100L
private const val KC_INIT_TIMEOUT_STEPS = 100 // 100 × 100ms = 10 s

/**
 * Waits (up to ~10 s) for the Keycloak JS adapter to complete its async init,
 * then updates [IamService.state] if the user has an active session.
 *
 * After that, a background coroutine polls [window._kcToken] every
 * [KC_TOKEN_SYNC_INTERVAL_MS] ms so that silent token refreshes done by
 * Keycloak.js (via `onTokenExpired` → `updateToken`) are reflected in
 * [IamService.state].
 */
actual suspend fun initPlatformIam() {
    // Poll until Keycloak.js has finished its init() Promise
    repeat(KC_INIT_TIMEOUT_STEPS) {
        if (jsIsKcReady()) return@repeat
        delay(KC_INIT_POLL_INTERVAL_MS)
    }
    syncKcState()

    // Start a background loop to pick up silent token refreshes.
    iamScope.launch {
        while (true) {
            delay(KC_TOKEN_SYNC_INTERVAL_MS)
            syncKcState()
        }
    }
}

/**
 * Reads the current Keycloak JS state and updates [IamService.state] if
 * the token has changed or the session has ended.
 */
private fun syncKcState() {
    val authenticated = jsIsKcAuthenticated()
    val currentState = IamService.state.value
    if (authenticated) {
        val token = jsGetKcToken()
        // Only update when the token value has actually changed to avoid
        // unnecessary Compose recompositions.
        if (token != null && token != currentState.token) {
            IamService.state.value = IamState(
                isAuthenticated = true,
                username = jsGetKcUsername() ?: currentState.username,
                token = token
            )
        }
    } else if (currentState.isAuthenticated) {
        // Session ended (e.g. refresh token expired) – clear local auth state.
        IamService.state.value = IamState()
    }
}
