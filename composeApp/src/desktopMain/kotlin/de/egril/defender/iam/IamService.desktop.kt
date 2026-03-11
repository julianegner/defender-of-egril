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

/**
 * Fixed loopback port for the PKCE redirect URI.
 *
 * Keycloak 24 does NOT support wildcards in the port position of a redirect URI
 * (e.g. http://localhost:*\/callback is rejected). A dedicated, well-known port
 * registered in the Keycloak client avoids this restriction.
 *
 * The port is registered in local-keycloak/egril-realm.json as
 * http://localhost:10001/callback.
 */
private const val PKCE_CALLBACK_PORT = 10001

internal actual fun startPlatformLogin() {
    Thread {
        try {
            val port = acquireCallbackPort()
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

            // Open the Keycloak login page in a new browser window in the foreground.
            // IamService.loginInProgress is set to true by IamService.login() before
            // this call; the UI shows a "Waiting for browser login" dialog while
            // this thread is blocked waiting for the callback.
            openBrowserNewWindow(authUrl)

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
        } finally {
            // Always clear the in-progress flag, regardless of success or failure.
            IamService.loginInProgress.value = false
        }
    }.also { it.isDaemon = true }.start()
}

internal actual fun performPlatformLogout() {
    storedRefreshToken = null
    tokenExpiresAtMs = 0L
    refreshThreadRunning = false
    IamService.loginInProgress.value = false
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
 * Starts a daemon thread that refreshes the access token ~TOKEN_REFRESH_BUFFER_SECONDS
 * seconds before it expires. The thread runs until the user logs out or the
 * refresh fails (e.g. refresh token expired or revoked).
 *
 * A refreshThreadRunning guard prevents multiple concurrent refresh threads if
 * startPlatformLogin is called more than once.
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
    server.soTimeout = 120_000 // 2-minute timeout to give the user time to log in
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

/**
 * Returns PKCE_CALLBACK_PORT if that port is available, otherwise finds any free port.
 *
 * Note: if a fallback port is returned it will NOT be registered in Keycloak, so the
 * authorization code exchange will fail. In practice port PKCE_CALLBACK_PORT should
 * always be available on a development machine.
 */
private fun acquireCallbackPort(): Int {
    return try {
        ServerSocket(PKCE_CALLBACK_PORT).use { PKCE_CALLBACK_PORT }
    } catch (_: Exception) {
        ServerSocket(0).use { it.localPort }
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

/** Extracts a numeric field from a flat JSON object without a full parser. */
private fun extractJsonNumberValue(json: String, key: String): Long? =
    Regex("\"${Regex.escape(key)}\"\\s*:\\s*(\\d+)").find(json)?.groupValues?.get(1)?.toLongOrNull()

// ---------------------------------------------------------------------------
// Browser launcher – opens the URL in a new foreground window
// ---------------------------------------------------------------------------

/**
 * Opens [url] in a new browser window rather than a background tab.
 *
 * Strategy per OS:
 * - **macOS**: `open -n <url>` forces a new instance/window of the default browser.
 * - **Windows**: `cmd /c start "" <url>` opens in the default browser (new window or focused tab).
 * - **Linux / other**: tries common browsers with `--new-window`; falls back to `xdg-open`.
 *
 * Falls back to [java.awt.Desktop.browse] if all process-based attempts fail.
 */
private fun openBrowserNewWindow(url: String) {
    val os = System.getProperty("os.name").lowercase()
    try {
        when {
            os.contains("mac") -> {
                // -n opens a new instance (new window) of whichever app handles the URL
                ProcessBuilder("open", "-n", url).inheritIO().start()
                return
            }
            os.contains("win") -> {
                // 'start' opens the URL in the system default browser
                ProcessBuilder("cmd.exe", "/c", "start", "", url).inheritIO().start()
                return
            }
            else -> {
                // Linux / BSD: attempt browser-specific --new-window flags
                if (openBrowserLinuxNewWindow(url)) return
            }
        }
    } catch (_: Exception) {
        // Fall through to AWT fallback
    }
    // AWT fallback – may open a tab rather than a window but is always available
    try {
        java.awt.Desktop.getDesktop().browse(URI(url))
    } catch (e: Exception) {
        // Both the process-based launch and AWT have failed.
        // The loginInProgress flag is cleared by the thread's finally block, so the
        // spinner in the UI will disappear and the user can try again.
        println("IAM: Failed to open browser for login: ${e.message}")
    }
}

/**
 * On Linux, detects the default browser via `xdg-settings` and tries to launch it
 * with `--new-window`. Returns `true` if the browser process was started successfully.
 */
private fun openBrowserLinuxNewWindow(url: String): Boolean {
    // Detect the default browser .desktop file (e.g. "google-chrome.desktop")
    val defaultDesktop = try {
        val proc = ProcessBuilder("xdg-settings", "get", "default-web-browser").start()
        val result = proc.inputStream.bufferedReader().readLine()?.trim()?.lowercase() ?: ""
        proc.waitFor()
        result
    } catch (_: Exception) {
        ""
    }

    // Map recognised .desktop names to their CLI binary + --new-window flag
    val command: List<String>? = when {
        "chromium" in defaultDesktop -> listOf("chromium", "--new-window", url)
        "chrome" in defaultDesktop  -> listOf("google-chrome", "--new-window", url)
        "brave" in defaultDesktop   -> listOf("brave-browser", "--new-window", url)
        "firefox" in defaultDesktop -> listOf("firefox", "--new-window", url)
        "epiphany" in defaultDesktop -> listOf("epiphany", "--new-window", url)
        else -> null
    }

    if (command != null) {
        try {
            ProcessBuilder(command).inheritIO().start()
            return true
        } catch (_: Exception) {}
    }

    // Generic Linux fallback: xdg-open (may open a tab in an existing window)
    return try {
        ProcessBuilder("xdg-open", url).inheritIO().start()
        true
    } catch (_: Exception) {
        false
    }
}
