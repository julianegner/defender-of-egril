package de.egril.defender.iam

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.publicvalue.multiplatform.oidc.OpenIdConnectClient
import org.publicvalue.multiplatform.oidc.types.CodeChallengeMethod
import org.publicvalue.multiplatform.oidc.appsupport.IosCodeAuthFlowFactory
import platform.Foundation.NSDate

actual fun getIamBaseUrl(): String = "http://localhost:8081"

// ---------------------------------------------------------------------------
// OIDC redirect URI – must be a valid redirect URI registered in Keycloak.
// ASWebAuthenticationSession handles the custom scheme automatically.
// ---------------------------------------------------------------------------
private const val IOS_REDIRECT_URI = "egril://callback"

// ---------------------------------------------------------------------------
// Token storage – held in memory only
// ---------------------------------------------------------------------------

@Volatile
private var storedRefreshToken: String? = null

/** Epoch-millisecond timestamp at which the current access token expires. */
@Volatile
private var tokenExpiresAtMs: Long = 0L

private const val TOKEN_REFRESH_BUFFER_SECONDS = 30L

@Volatile
private var refreshCoroutineRunning = false

private const val DEFAULT_TOKEN_EXPIRY_SECONDS = 300L
private const val FALLBACK_USERNAME = "unknown"

/** Single factory instance – ASWebAuthenticationSession, no additional setup required. */
private val iosAuthFlowFactory = IosCodeAuthFlowFactory()

/** Coroutine scope for login and background refresh. */
private val iamScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

// ---------------------------------------------------------------------------
// OIDC client factory
// ---------------------------------------------------------------------------

private fun createOidcClient(): OpenIdConnectClient = OpenIdConnectClient {
    endpoints {
        tokenEndpoint = IamConfig.tokenUrl
        authorizationEndpoint = IamConfig.authUrl
        endSessionEndpoint = IamConfig.logoutUrl
    }
    clientId = IamConfig.CLIENT_ID
    scope = "openid profile"
    codeChallengeMethod = CodeChallengeMethod.S256
    redirectUri = IOS_REDIRECT_URI
}

/** Current time as epoch milliseconds using Foundation's NSDate. */
private fun nowMs(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()

// ---------------------------------------------------------------------------
// Platform implementations
// ---------------------------------------------------------------------------

internal actual fun startPlatformLogin() {
    iamScope.launch {
        try {
            val client = createOidcClient()
            val flow = iosAuthFlowFactory.createAuthFlow(client)
            // startLogin presents ASWebAuthenticationSession; continueLogin waits for the redirect.
            flow.startLogin()
            val tokens = flow.continueLogin()

            val accessToken = tokens.access_token ?: return@launch
            IamService.state.value = buildIamState(accessToken)
            storedRefreshToken = tokens.refresh_token
            tokenExpiresAtMs = nowMs() + (tokens.expires_in?.toLong() ?: DEFAULT_TOKEN_EXPIRY_SECONDS) * 1_000L

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

internal actual fun performPlatformLogoutLocal() {
    storedRefreshToken = null
    tokenExpiresAtMs = 0L
    refreshCoroutineRunning = false
}

internal actual fun performPlatformLogoutBackchannel() {
    // iOS uses ASWebAuthenticationSession; there is no long-lived server socket concern.
    // Clearing in-memory state is sufficient.
    storedRefreshToken = null
    tokenExpiresAtMs = 0L
    refreshCoroutineRunning = false
}

actual suspend fun initPlatformIam() {
    // iOS login is triggered manually via ASWebAuthenticationSession.
    // No stored session is restored on startup (no TokenStore used).
}

/**
 * Opens the Keycloak user account console via UIApplication so the user can
 * manage their credentials, update their username, or delete their account.
 */
internal actual fun openPlatformAccountConsole() {
    val url = platform.Foundation.NSURL.URLWithString(IamConfig.accountUrl) ?: return
    platform.UIKit.UIApplication.sharedApplication.openURL(url)
}

// ---------------------------------------------------------------------------
// Background token refresh
// ---------------------------------------------------------------------------

private fun startBackgroundTokenRefresh(client: OpenIdConnectClient) {
    if (refreshCoroutineRunning) return
    refreshCoroutineRunning = true

    iamScope.launch {
        while (refreshCoroutineRunning) {
            val refreshToken = storedRefreshToken ?: break
            val msUntilRefresh = tokenExpiresAtMs - nowMs() - TOKEN_REFRESH_BUFFER_SECONDS * 1_000L
            if (msUntilRefresh > 0) {
                delay(msUntilRefresh)
            }

            if (!refreshCoroutineRunning || storedRefreshToken == null) break

            try {
                val newTokens = client.refreshToken(refreshToken = refreshToken)
                val accessToken = newTokens.access_token ?: break
                IamService.state.value = buildIamState(accessToken, fallbackUsername = IamService.state.value.username)
                storedRefreshToken = newTokens.refresh_token ?: refreshToken
                tokenExpiresAtMs = nowMs() + (newTokens.expires_in?.toLong() ?: DEFAULT_TOKEN_EXPIRY_SECONDS) * 1_000L
            } catch (_: Exception) {
                // Refresh token expired or revoked – log out silently
                IamService.logout()
                break
            }
        }
        refreshCoroutineRunning = false
    }
}
