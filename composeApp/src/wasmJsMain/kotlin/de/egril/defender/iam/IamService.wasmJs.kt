@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package de.egril.defender.iam

import kotlinx.coroutines.delay

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

/**
 * Waits (up to ~10 s) for the Keycloak JS adapter to complete its async init,
 * then updates [IamService.state] if the user has an active session.
 */
actual suspend fun initPlatformIam() {
    // Poll until Keycloak.js has finished its init() Promise (max ~10 s)
    repeat(100) {
        if (jsIsKcReady()) return@repeat
        delay(100)
    }
    if (jsIsKcAuthenticated()) {
        val username = jsGetKcUsername()
        val token = jsGetKcToken()
        if (username != null && token != null) {
            IamService.state.value = IamState(
                isAuthenticated = true,
                username = username,
                token = token
            )
        }
    }
}
