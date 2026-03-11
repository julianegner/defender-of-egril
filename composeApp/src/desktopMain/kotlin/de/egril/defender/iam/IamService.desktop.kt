package de.egril.defender.iam

import java.net.HttpURLConnection
import java.net.ServerSocket
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.Base64
import kotlin.random.Random

actual fun getIamBaseUrl(): String = readIamBaseUrlFromJvmEnv()

// ---------------------------------------------------------------------------
// Token storage – access token + refresh token held in memory only
// ---------------------------------------------------------------------------

/** Refresh token from the last successful PKCE exchange or token refresh. */
@Volatile
private var storedRefreshToken: String? = null

/** Millisecond timestamp at which the access token expires. */
@Volatile
private var tokenExpiresAtMs: Long = 0L

/** How many seconds before expiry to trigger a refresh. */
private const val TOKEN_REFRESH_BUFFER_SECONDS = 30L

/** HTTP connection / read timeout for Keycloak token endpoint calls (ms). */
private const val TOKEN_HTTP_TIMEOUT_MS = 10_000

/** Guard against launching multiple concurrent refresh threads. */
@Volatile
private var refreshThreadRunning = false

internal actual fun startPlatformLogin() {
    Thread {
        try {
            val port = findFreePort()
            val redirectUri = "http://localhost:$port/callback"
            val codeVerifier = generateCodeVerifier()
            val codeChallenge = generateCodeChallenge(codeVerifier)
            val state = generateRandomBase64(16)

            val authUrl = buildString {
                append(IamConfig.authUrl)
                append("?client_id=").append(IamConfig.CLIENT_ID)
                append("&response_type=code")
                append("&scope=openid")
                append("&redirect_uri=").append(URLEncoder.encode(redirectUri, "UTF-8"))
                append("&state=").append(state)
                append("&code_challenge=").append(codeChallenge)
                append("&code_challenge_method=S256")
            }

            java.awt.Desktop.getDesktop().browse(URI(authUrl))

            val code = waitForAuthCode(port) ?: return@Thread
            val tokenData = exchangeCodeForToken(code, codeVerifier, redirectUri) ?: return@Thread

            IamService.state.value = tokenData.toIamState()
            storedRefreshToken = tokenData.refreshToken
            tokenExpiresAtMs = System.currentTimeMillis() + tokenData.expiresInSeconds * 1_000L

            if (tokenData.refreshToken != null) {
                startBackgroundTokenRefresh()
            }
        } catch (_: Exception) {
            // Login errors must never disrupt gameplay
        }
    }.also { it.isDaemon = true }.start()
}

internal actual fun performPlatformLogout() {
    storedRefreshToken = null
    tokenExpiresAtMs = 0L
    refreshThreadRunning = false
    // Open the Keycloak logout endpoint in the browser so the SSO session is terminated
    try {
        val logoutUrl = "${IamConfig.logoutUrl}?client_id=${IamConfig.CLIENT_ID}" +
                "&post_logout_redirect_uri=${URLEncoder.encode("http://localhost", "UTF-8")}"
        java.awt.Desktop.getDesktop().browse(URI(logoutUrl))
    } catch (_: Exception) {
        // Ignore – local state has already been cleared by IamService.logout()
    }
}

actual suspend fun initPlatformIam() {
    // Desktop login is triggered manually; nothing to restore on startup.
    // The background refresh thread is started in startPlatformLogin() after
    // the initial PKCE exchange succeeds.
}

// ---------------------------------------------------------------------------
// Background token refresh
// ---------------------------------------------------------------------------

/**
 * Starts a daemon thread that refreshes the access token ~[TOKEN_REFRESH_BUFFER_SECONDS]
 * seconds before it expires. The thread runs until the user logs out or the
 * refresh fails (e.g. refresh token expired or revoked).
 *
 * A [refreshThreadRunning] guard prevents multiple concurrent refresh threads if
 * [startPlatformLogin] is called more than once.
 */
private fun startBackgroundTokenRefresh() {
    if (refreshThreadRunning) return
    refreshThreadRunning = true
    Thread {
        while (refreshThreadRunning) {
            val refreshToken = storedRefreshToken ?: break
            val now = System.currentTimeMillis()
            val expiresAt = tokenExpiresAtMs
            val msUntilRefresh = expiresAt - now - TOKEN_REFRESH_BUFFER_SECONDS * 1_000L
            if (msUntilRefresh > 0) {
                Thread.sleep(msUntilRefresh)
            }
            // Re-check after sleeping – logout may have cleared storedRefreshToken
            if (!refreshThreadRunning || storedRefreshToken == null) break

            val tokenData = refreshAccessToken(refreshToken)
            if (tokenData == null) {
                // Refresh failed (refresh token expired/revoked) – log out silently
                IamService.logout()
                break
            }

            IamService.state.value = tokenData.toIamState()
            storedRefreshToken = tokenData.refreshToken ?: refreshToken
            tokenExpiresAtMs = System.currentTimeMillis() + tokenData.expiresInSeconds * 1_000L
        }
        refreshThreadRunning = false
    }.also { it.isDaemon = true }.start()
}

/**
 * Exchanges the stored refresh token for a new access token.
 * Returns null if the refresh fails (token expired, revoked, or network error).
 */
private fun refreshAccessToken(refreshToken: String): TokenData? {
    return try {
        val body = buildString {
            append("grant_type=refresh_token")
            append("&client_id=${IamConfig.CLIENT_ID}")
            append("&refresh_token=${URLEncoder.encode(refreshToken, "UTF-8")}")
        }
        val conn = URL(IamConfig.tokenUrl).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        conn.connectTimeout = TOKEN_HTTP_TIMEOUT_MS
        conn.readTimeout = TOKEN_HTTP_TIMEOUT_MS
        conn.outputStream.use { it.write(body.toByteArray()) }

        if (conn.responseCode != 200) {
            conn.disconnect()
            return null
        }
        val response = conn.inputStream.bufferedReader().readText()
        conn.disconnect()
        parseTokenResponse(response)
    } catch (_: Exception) {
        null
    }
}

// ---------------------------------------------------------------------------
// Private helpers
// ---------------------------------------------------------------------------

/** Parsed token response from the Keycloak token endpoint. */
private data class TokenData(
    val accessToken: String,
    val refreshToken: String?,
    val expiresInSeconds: Long
) {
    fun toIamState(): IamState {
        val username = extractUsernameFromJwt(accessToken) ?: "unknown"
        return IamState(isAuthenticated = true, username = username, token = accessToken)
    }
}

private fun parseTokenResponse(json: String): TokenData? {
    val accessToken = extractJsonStringValue(json, "access_token") ?: return null
    val refreshToken = extractJsonStringValue(json, "refresh_token")
    val expiresIn = extractJsonNumberValue(json, "expires_in") ?: 300L
    return TokenData(accessToken, refreshToken, expiresIn)
}

private fun waitForAuthCode(port: Int): String? {
    val server = ServerSocket(port)
    server.soTimeout = 60_000 // 1-minute timeout
    return try {
        val socket = server.accept()
        val requestLine = socket.getInputStream().bufferedReader().readLine() ?: return null
        // e.g. "GET /callback?code=abc&state=xyz HTTP/1.1"
        val pathAndQuery = requestLine.split(" ").getOrNull(1) ?: return null
        val queryString = pathAndQuery.substringAfter("?", "")
        val params = queryString.split("&")
            .mapNotNull { it.split("=", limit = 2).takeIf { p -> p.size == 2 } }
            .associate { (k, v) -> k to v }

        val html = "<html><body><h2>Login successful! You may close this window.</h2></body></html>"
        socket.getOutputStream().write(
            "HTTP/1.1 200 OK\r\nContent-Type: text/html\r\n\r\n$html".toByteArray()
        )
        socket.close()
        params["code"]
    } catch (_: Exception) {
        null
    } finally {
        server.close()
    }
}

private fun exchangeCodeForToken(code: String, codeVerifier: String, redirectUri: String): TokenData? {
    return try {
        val body = buildString {
            append("grant_type=authorization_code")
            append("&client_id=${IamConfig.CLIENT_ID}")
            append("&code=${URLEncoder.encode(code, "UTF-8")}")
            append("&redirect_uri=${URLEncoder.encode(redirectUri, "UTF-8")}")
            append("&code_verifier=${URLEncoder.encode(codeVerifier, "UTF-8")}")
        }

        val conn = URL(IamConfig.tokenUrl).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        conn.connectTimeout = TOKEN_HTTP_TIMEOUT_MS
        conn.readTimeout = TOKEN_HTTP_TIMEOUT_MS
        conn.outputStream.use { it.write(body.toByteArray()) }

        val response = conn.inputStream.bufferedReader().readText()
        conn.disconnect()

        parseTokenResponse(response)
    } catch (_: Exception) {
        null
    }
}

private fun generateCodeVerifier(): String =
    Base64.getUrlEncoder().withoutPadding()
        .encodeToString(ByteArray(32).also { Random.nextBytes(it) })

private fun generateCodeChallenge(verifier: String): String {
    val hash = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
    return Base64.getUrlEncoder().withoutPadding().encodeToString(hash)
}

private fun generateRandomBase64(bytes: Int): String =
    Base64.getUrlEncoder().withoutPadding()
        .encodeToString(ByteArray(bytes).also { Random.nextBytes(it) })

private fun findFreePort(): Int = ServerSocket(0).use { it.localPort }

/** Extracts a numeric field from a flat JSON object without a full parser. */
private fun extractJsonNumberValue(json: String, key: String): Long? =
    Regex("\"${Regex.escape(key)}\"\\s*:\\s*(\\d+)").find(json)?.groupValues?.get(1)?.toLongOrNull()
