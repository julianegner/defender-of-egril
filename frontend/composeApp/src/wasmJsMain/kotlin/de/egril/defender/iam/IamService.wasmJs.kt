@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package de.egril.defender.iam

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.hyperether.resources.currentLanguage

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

@JsFun("() => { return window._kcEmail || null; }")
private external fun jsGetKcEmail(): String?

@JsFun("() => { return window._kcFirstName || null; }")
private external fun jsGetKcFirstName(): String?

@JsFun("() => { return window._kcLastName || null; }")
private external fun jsGetKcLastName(): String?

@JsFun("() => { return window._kcToken || null; }")
private external fun jsGetKcToken(): String?

/**
 * Reads the 'dark_mode' key from localStorage.
 * The multiplatform-settings library stores boolean values as "true"/"false" strings in
 * localStorage on WASM/JS, using the key name directly.
 *
 * NOTE: The key 'dark_mode' must match [AppSettings.KEY_DARK_MODE] in AppSettings.kt.
 * If that constant is renamed, this function must be updated accordingly.
 */
@JsFun("() => { return localStorage.getItem('dark_mode') === 'true'; }")
private external fun jsGetIsDarkMode(): Boolean

/**
 * Sets the KEYCLOAKIFY_DARK_MODE cookie so that the Keycloakify theme on the
 * Keycloak login page can read the app's dark-mode preference.
 *
 * Cookies are scoped to the domain (not the port), so a cookie set on
 * localhost:8080 is readable by Keycloak on localhost:8081.
 *
 * NOTE: The cookie is set without the `Secure` flag so that it works on
 * plain-HTTP localhost during development. In production, where both the app
 * and Keycloak are served over HTTPS, browsers enforce Secure automatically
 * for cross-origin cookies, so this is safe in practice.
 *
 * @param isDark true for dark mode, false for light mode.
 */
@JsFun("(isDark) => { document.cookie = 'KEYCLOAKIFY_DARK_MODE=' + (isDark ? 'dark' : 'light') + '; path=/; SameSite=Strict'; }")
private external fun jsSetDarkModeCookie(isDark: Boolean)

/**
 * Initiates a Keycloak login, forwarding the app's selected [locale] so the
 * login page is shown in the same language as the game.
 *
 * Returns `true` if the Keycloak JS adapter was available and the login
 * redirect was initiated, `false` if the adapter was not yet initialised
 * (e.g. because the Keycloak server was unreachable at page load time).
 *
 * Stale `kc-callback-*` entries are cleared from localStorage before the
 * redirect so that a full localStorage does not cause a QuotaExceededError
 * when Keycloak tries to store the new callback state.
 */
@JsFun("""(locale) => {
    if (window._keycloak) {
        try {
            Object.keys(localStorage)
                .filter(function(k) { return k.startsWith('kc-callback-'); })
                .forEach(function(k) { localStorage.removeItem(k); });
        } catch (e) { /* ignore cleanup errors */ }
        window._keycloak.login({ prompt: 'login', locale: locale });
        return true;
    }
    return false;
}""")
private external fun jsKcLoginWithLocale(locale: String): Boolean

@JsFun("() => { if (window._keycloak) { window._keycloak.logout({ redirectUri: window.location.origin }); } }")
private external fun jsKcLogout()

@JsFun("(url) => { window.open(url, '_blank', 'noopener,noreferrer'); }")
private external fun jsOpenUrl(url: String)

// ---------------------------------------------------------------------------
// Platform implementations
// ---------------------------------------------------------------------------

actual fun getIamBaseUrl(): String = jsGetKeycloakUrl()

internal actual fun startPlatformLogin() {
    // Sync the app's dark-mode preference to a cookie so the Keycloakify theme
    // on the Keycloak login page can apply the correct color scheme.
    jsSetDarkModeCookie(jsGetIsDarkMode())

    // Forward the app's selected language to the Keycloak login page.
    val locale = currentLanguage.value
    val loginStarted = jsKcLoginWithLocale(locale.code)

    // If the Keycloak JS adapter was not available (e.g. the Keycloak server
    // was unreachable at page load time), the browser redirect never happens.
    // Reset the in-progress flag so the UI is not stuck showing "Waiting for
    // login…" indefinitely.
    if (!loginStarted) {
        IamService.loginInProgress.value = false
    }
}

internal actual fun performPlatformLogout() {
    jsKcLogout()
}

/**
 * On WASM the Keycloak JS adapter manages SSO state. When switching players we only
 * need to clear the local [IamService.state]; the SSO session in the browser can
 * remain active (the next login will reuse it silently if still valid).
 */
internal actual fun performPlatformLogoutLocal() {
    // No-op: IamService.logoutLocal() clears IamService.state directly.
}

/**
 * On WASM, performs a full Keycloak.js SSO logout (browser redirect back to the
 * origin). This terminates the SSO session so that a subsequent login does not
 * silently re-authenticate as the previous user.
 */
internal actual fun performPlatformLogoutBackchannel() {
    jsKcLogout()
}

/**
 * Opens the Keycloak user account console in a new browser tab so the user can
 * manage their credentials, update their username, or delete their account.
 */
internal actual fun openPlatformAccountConsole() {
    jsOpenUrl(IamConfig.accountUrl)
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
                token = token,
                email = jsGetKcEmail() ?: currentState.email,
                firstName = jsGetKcFirstName() ?: currentState.firstName,
                lastName = jsGetKcLastName() ?: currentState.lastName
            )
        }
    } else if (currentState.isAuthenticated) {
        // Session ended (e.g. refresh token expired) – clear local auth state.
        IamService.state.value = IamState()
    }
}
