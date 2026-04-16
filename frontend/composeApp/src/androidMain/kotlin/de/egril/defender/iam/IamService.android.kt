package de.egril.defender.iam

import de.egril.defender.BuildConfig
import de.egril.defender.utils.getPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.publicvalue.multiplatform.oidc.OpenIdConnectClient
import org.publicvalue.multiplatform.oidc.types.CodeChallengeMethod
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder

// On Android, JVM system properties for custom keys are not accessible at runtime.
// The Keycloak URL is therefore baked into the APK at build time via BuildConfig,
// which is populated from profiles/local.properties or profiles/production.properties
// depending on the selected product flavor.
actual fun getIamBaseUrl(): String = BuildConfig.IAM_BASE_URL

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
private const val ANDROID_TOKEN_HTTP_TIMEOUT_MS = 10_000
private const val MIN_DEVICE_POLL_INTERVAL_SECONDS = 5L

/** Tracks the current login job so it can be cancelled when a new login starts. */
@Volatile
private var currentLoginJob: Job? = null

/** Set to `true` to abort an in-progress Device Authorization Grant polling loop. */
@Volatile
private var deviceAuthCancelledAndroid = false

/** Coroutine scope for the login flow and background token refresh. */
private val iamScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

// ---------------------------------------------------------------------------
// Device Authorization Grant – data types (Android TV)
// ---------------------------------------------------------------------------

private data class AndroidDeviceAuthResponse(
    val deviceCode: String,
    val userCode: String,
    val verificationUri: String,
    val verificationUriComplete: String?,
    val expiresIn: Long,
    val interval: Long
)

private data class AndroidTokenData(
    val accessToken: String,
    val refreshToken: String?,
    val expiresInSeconds: Long
)

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

/**
 * Creates an [OpenIdConnectClient] using [IamConfig.DESKTOP_CLIENT_ID] for
 * background token refresh after a Device Authorization Grant login on Android TV.
 */
private fun createDeviceAuthOidcClient(): OpenIdConnectClient = OpenIdConnectClient {
    endpoints {
        tokenEndpoint = IamConfig.tokenUrl
        authorizationEndpoint = IamConfig.authUrl
        endSessionEndpoint = IamConfig.logoutUrl
    }
    clientId = IamConfig.DESKTOP_CLIENT_ID
    scope = "openid"
}

// ---------------------------------------------------------------------------
// Platform implementations
// ---------------------------------------------------------------------------

internal actual fun startPlatformLogin() {
    // Cancel any in-progress Device Auth polling loop or browser-based flow.
    deviceAuthCancelledAndroid = true
    currentLoginJob?.cancel()
    deviceAuthCancelledAndroid = false

    currentLoginJob = iamScope.launch {
        try {
            if (getPlatform().isAndroidTV) {
                // Android TV: no accessible browser, use Device Authorization Grant.
                startDeviceAuthLogin()
            } else {
                // Regular Android: use standard PKCE Authorization Code Grant via browser.
                startPkceLogin()
            }
        } catch (_: Exception) {
            // Login errors must never disrupt gameplay
        } finally {
            IamService.deviceAuthState.value = null
            IamService.loginInProgress.value = false
        }
    }
}

/**
 * PKCE Authorization Code Grant login flow for regular Android devices.
 * Opens Chrome Custom Tabs for the Keycloak login page.
 */
private suspend fun startPkceLogin() {
    val client = createOidcClient()
    val flow = AndroidIamFlowProvider.factory.createAuthFlow(client)
    // startLogin opens Chrome Custom Tabs; continueLogin waits for the redirect.
    flow.startLogin()
    val tokens = flow.continueLogin()

    val accessToken = tokens.access_token ?: return
    IamService.state.value = buildIamState(accessToken)
    storedRefreshToken = tokens.refresh_token
    tokenExpiresAtMs = System.currentTimeMillis() + (tokens.expires_in?.toLong() ?: DEFAULT_TOKEN_EXPIRY_SECONDS) * 1_000L
    if (tokens.refresh_token != null) {
        startBackgroundTokenRefresh(client)
    }
}

/**
 * Device Authorization Grant (RFC 8628) login flow for Android TV devices.
 * No browser is needed: the user code is displayed in the game and the user
 * completes authentication on a phone or second device.
 */
private suspend fun startDeviceAuthLogin() {
    val deviceResponse = requestDeviceAuthAndroid() ?: return

    IamService.deviceAuthState.value = DeviceAuthState(
        userCode = deviceResponse.userCode,
        verificationUri = deviceResponse.verificationUri,
        verificationUriComplete = deviceResponse.verificationUriComplete
    )

    val tokenData = pollDeviceTokenAndroid(
        deviceCode = deviceResponse.deviceCode,
        interval = deviceResponse.interval,
        expiresIn = deviceResponse.expiresIn
    ) ?: return

    IamService.state.value = buildIamState(tokenData.accessToken)
    storedRefreshToken = tokenData.refreshToken
    tokenExpiresAtMs = System.currentTimeMillis() + tokenData.expiresInSeconds * 1_000L

    if (tokenData.refreshToken != null) {
        startBackgroundTokenRefresh(createDeviceAuthOidcClient())
    }
}

internal actual fun performPlatformLogout() {
    deviceAuthCancelledAndroid = true
    currentLoginJob?.cancel()
    storedRefreshToken = null
    tokenExpiresAtMs = 0L
    refreshCoroutineRunning = false
}

internal actual fun performPlatformLogoutLocal() {
    deviceAuthCancelledAndroid = true
    currentLoginJob?.cancel()
    storedRefreshToken = null
    tokenExpiresAtMs = 0L
    refreshCoroutineRunning = false
}

internal actual fun performPlatformLogoutBackchannel() {
    // Android uses a system browser / Chrome Custom Tabs for login; there is no
    // long-lived server socket (port) concern. Clearing in-memory state is sufficient.
    deviceAuthCancelledAndroid = true
    currentLoginJob?.cancel()
    storedRefreshToken = null
    tokenExpiresAtMs = 0L
    refreshCoroutineRunning = false
}

/**
 * Checks for a pending login continuation (handles Activity/process recreation during
 * the OAuth2 redirect flow). If a login was in progress when the process was killed,
 * [continueLogin] will resume it and retrieve the authorization code from the saved state.
 *
 * On Android TV the Device Authorization Grant is used instead of PKCE, so there is
 * no redirect flow to continue.
 */
actual suspend fun initPlatformIam() {
    if (getPlatform().isAndroidTV) return // Device Auth has no redirect continuation

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

// ---------------------------------------------------------------------------
// Device Authorization Grant HTTP helpers (Android TV)
// ---------------------------------------------------------------------------

/**
 * POSTs to the Keycloak device-authorization endpoint and returns the parsed response,
 * or `null` if the request fails or the server returns a non-200 status.
 */
private suspend fun requestDeviceAuthAndroid(): AndroidDeviceAuthResponse? = withContext(Dispatchers.IO) {
    try {
        val body = "client_id=${URLEncoder.encode(IamConfig.DESKTOP_CLIENT_ID, "UTF-8")}&scope=openid"
        val conn = URI.create(IamConfig.deviceAuthUrl).toURL().openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        conn.connectTimeout = ANDROID_TOKEN_HTTP_TIMEOUT_MS
        conn.readTimeout = ANDROID_TOKEN_HTTP_TIMEOUT_MS
        conn.outputStream.use { it.write(body.toByteArray()) }

        if (conn.responseCode != 200) {
            conn.disconnect()
            return@withContext null
        }
        val json = conn.inputStream.bufferedReader().readText()
        conn.disconnect()

        AndroidDeviceAuthResponse(
            deviceCode = extractJsonStringValue(json, "device_code") ?: return@withContext null,
            userCode = extractJsonStringValue(json, "user_code") ?: return@withContext null,
            verificationUri = extractJsonStringValue(json, "verification_uri") ?: return@withContext null,
            verificationUriComplete = extractJsonStringValue(json, "verification_uri_complete"),
            expiresIn = extractJsonLongValue(json, "expires_in") ?: 300L,
            interval = extractJsonLongValue(json, "interval") ?: 5L
        )
    } catch (_: Exception) {
        null
    }
}

/**
 * Polls the Keycloak token endpoint using the Device Authorization Grant until
 * the user completes login, the code expires, or [deviceAuthCancelledAndroid] is set.
 * Returns [AndroidTokenData] on success, or `null` on failure or cancellation.
 */
private suspend fun pollDeviceTokenAndroid(
    deviceCode: String,
    interval: Long,
    expiresIn: Long
): AndroidTokenData? {
    val deadline = System.currentTimeMillis() + expiresIn * 1_000L
    val pollIntervalMs = maxOf(interval, MIN_DEVICE_POLL_INTERVAL_SECONDS) * 1_000L

    while (!deviceAuthCancelledAndroid && System.currentTimeMillis() < deadline) {
        // delay() is a cancellable suspension point: if the coroutine is cancelled
        // (e.g. via currentLoginJob.cancel()) a CancellationException is thrown here.
        delay(pollIntervalMs)
        if (deviceAuthCancelledAndroid) break

        val result = tryExchangeDeviceCodeAndroid(deviceCode)
        if (result != null) return result
    }
    return null
}

/**
 * Sends a single `urn:ietf:params:oauth:grant-type:device_code` token request (RFC 8628 §3.4).
 * Returns [AndroidTokenData] on success, `null` if authorization is still pending, the
 * request should be slowed down, or a network error occurs. Sets [deviceAuthCancelledAndroid]
 * on unrecoverable errors such as `expired_token`.
 */
private suspend fun tryExchangeDeviceCodeAndroid(deviceCode: String): AndroidTokenData? = withContext(Dispatchers.IO) {
    try {
        val body = buildString {
            append("grant_type=${URLEncoder.encode("urn:ietf:params:oauth:grant-type:device_code", "UTF-8")}")
            append("&client_id=${URLEncoder.encode(IamConfig.DESKTOP_CLIENT_ID, "UTF-8")}")
            append("&device_code=${URLEncoder.encode(deviceCode, "UTF-8")}")
        }
        val conn = URI.create(IamConfig.tokenUrl).toURL().openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        conn.connectTimeout = ANDROID_TOKEN_HTTP_TIMEOUT_MS
        conn.readTimeout = ANDROID_TOKEN_HTTP_TIMEOUT_MS
        conn.outputStream.use { it.write(body.toByteArray()) }

        if (conn.responseCode == 200) {
            val json = conn.inputStream.bufferedReader().readText()
            conn.disconnect()
            val accessToken = extractJsonStringValue(json, "access_token") ?: return@withContext null
            val refreshToken = extractJsonStringValue(json, "refresh_token")
            val expiresInSecs = extractJsonLongValue(json, "expires_in") ?: DEFAULT_TOKEN_EXPIRY_SECONDS
            return@withContext AndroidTokenData(accessToken, refreshToken, expiresInSecs)
        }

        val errorJson = try { conn.errorStream?.bufferedReader()?.readText() } catch (_: Exception) { null } ?: ""
        conn.disconnect()
        val error = extractJsonStringValue(errorJson, "error") ?: ""
        when (error) {
            "authorization_pending", "slow_down" -> null // still waiting – continue polling
            else -> {
                // expired_token, access_denied, or other unrecoverable error
                deviceAuthCancelledAndroid = true
                null
            }
        }
    } catch (_: Exception) {
        null
    }
}
