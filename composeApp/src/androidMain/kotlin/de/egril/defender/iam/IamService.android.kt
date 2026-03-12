package de.egril.defender.iam

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.publicvalue.multiplatform.oidc.OpenIdConnectClient
import org.publicvalue.multiplatform.oidc.types.CodeChallengeMethod

// On Android, environment variables are not accessible to apps.
// The Keycloak URL defaults to the local development value and should be
// overridden by setting the IAM_BASE_URL system property via ADB or build config.
actual fun getIamBaseUrl(): String =
    System.getProperty("iam.base.url") ?: "http://localhost:8081"

// ---------------------------------------------------------------------------
// OIDC redirect URI – must match the oidcRedirectScheme in build.gradle.kts
// and be registered as a valid redirect URI in Keycloak.
// ---------------------------------------------------------------------------
internal const val ANDROID_REDIRECT_URI = "egril://callback"

// ---------------------------------------------------------------------------
// Token storage – held in memory only (no persistence across process restarts)
// ---------------------------------------------------------------------------

/** Refresh token from the last successful OIDC exchange or token refresh. */
@Volatile
private var storedRefreshToken: String? = null

/** Millisecond timestamp at which the current access token expires. */
@Volatile
private var tokenExpiresAtMs: Long = 0L

/** How many seconds before expiry to trigger a proactive refresh. */
private const val TOKEN_REFRESH_BUFFER_SECONDS = 30L

/** Guard against launching multiple concurrent background-refresh coroutines. */
@Volatile
private var refreshCoroutineRunning = false

private const val DEFAULT_TOKEN_EXPIRY_SECONDS = 300L
private const val FALLBACK_USERNAME = "unknown"

/** Coroutine scope for the login flow and background token refresh. */
private val iamScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

// ---------------------------------------------------------------------------
// OIDC client factory
// ---------------------------------------------------------------------------

/**
 * Creates an [OpenIdConnectClient] pointed at the configured Keycloak instance.
 * Uses PKCE (S256) and the custom-scheme redirect URI.
 */
private fun createOidcClient(): OpenIdConnectClient = OpenIdConnectClient {
    endpoints {
        tokenEndpoint = IamConfig.tokenUrl
        authorizationEndpoint = IamConfig.authUrl
        endSessionEndpoint = IamConfig.logoutUrl
    }
    clientId = IamConfig.CLIENT_ID
    scope = "openid profile"
    codeChallengeMethod = CodeChallengeMethod.S256
    redirectUri = ANDROID_REDIRECT_URI
}

// ---------------------------------------------------------------------------
// Platform implementations
// ---------------------------------------------------------------------------

internal actual fun startPlatformLogin() {
    iamScope.launch {
        try {
            val client = createOidcClient()
            val flow = AndroidIamFlowProvider.factory.createAuthFlow(client)
            // startLogin opens Chrome Custom Tabs; continueLogin waits for the redirect.
            flow.startLogin()
            val tokens = flow.continueLogin()

            val accessToken = tokens.access_token ?: return@launch
            IamService.state.value = buildIamState(accessToken)
            storedRefreshToken = tokens.refresh_token
            tokenExpiresAtMs = System.currentTimeMillis() + (tokens.expires_in?.toLong() ?: DEFAULT_TOKEN_EXPIRY_SECONDS) * 1_000L
            if (tokens.refresh_token != null) {
                startBackgroundTokenRefresh(client)
            }
        } catch (_: Exception) {
            // Login errors must never disrupt gameplay
        } finally {
            IamService.loginInProgress.value = false
        }
    }
}

internal actual fun performPlatformLogout() {
    storedRefreshToken = null
    tokenExpiresAtMs = 0L
    refreshCoroutineRunning = false
}

/**
 * Checks for a pending login continuation (handles Activity/process recreation during
 * the OAuth2 redirect flow). If a login was in progress when the process was killed,
 * [continueLogin] will resume it and retrieve the authorization code from the saved state.
 */
actual suspend fun initPlatformIam() {
    try {
        val client = createOidcClient()
        val flow = AndroidIamFlowProvider.factory.createAuthFlow(client)
        if (!flow.canContinueLogin()) return

        val tokens = flow.continueLogin()
        val accessToken = tokens.access_token ?: return
        IamService.state.value = buildIamState(accessToken)
        storedRefreshToken = tokens.refresh_token
        tokenExpiresAtMs = System.currentTimeMillis() + (tokens.expires_in?.toLong() ?: DEFAULT_TOKEN_EXPIRY_SECONDS) * 1_000L

        if (tokens.refresh_token != null) {
            startBackgroundTokenRefresh(client)
        }
    } catch (_: Exception) {
        // If continuation fails, the user can log in manually
    }
}

// ---------------------------------------------------------------------------
// Background token refresh
// ---------------------------------------------------------------------------

/**
 * Launches a background coroutine that proactively refreshes the access token
 * [TOKEN_REFRESH_BUFFER_SECONDS] seconds before it expires.
 *
 * The coroutine stops when:
 * - [performPlatformLogout] clears [refreshCoroutineRunning], or
 * - the refresh token is revoked / the network call fails.
 */
private fun startBackgroundTokenRefresh(client: OpenIdConnectClient) {
    if (refreshCoroutineRunning) return
    refreshCoroutineRunning = true

    iamScope.launch {
        while (refreshCoroutineRunning) {
            val refreshToken = storedRefreshToken ?: break
            val now = System.currentTimeMillis()
            val msUntilRefresh = tokenExpiresAtMs - now - TOKEN_REFRESH_BUFFER_SECONDS * 1_000L
            if (msUntilRefresh > 0) {
                delay(msUntilRefresh)
            }

            if (!refreshCoroutineRunning || storedRefreshToken == null) break

            try {
                val newTokens = client.refreshToken(refreshToken = refreshToken)
                val accessToken = newTokens.access_token ?: break
                IamService.state.value = buildIamState(accessToken, fallbackUsername = IamService.state.value.username)
                storedRefreshToken = newTokens.refresh_token ?: refreshToken
                tokenExpiresAtMs = System.currentTimeMillis() + (newTokens.expires_in?.toLong() ?: DEFAULT_TOKEN_EXPIRY_SECONDS) * 1_000L
            } catch (_: Exception) {
                // Refresh token expired or revoked – log out silently
                IamService.logout()
                break
            }
        }
        refreshCoroutineRunning = false
    }
}
