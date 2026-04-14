package de.egril.defender.iam

import com.hyperether.resources.LocalizedStrings
import com.hyperether.resources.currentLanguage
import java.net.HttpURLConnection
import java.net.ServerSocket
import java.net.URI
import java.net.URLEncoder
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

actual fun getIamBaseUrl(): String = readIamBaseUrlFromJvmEnv()

// ---------------------------------------------------------------------------
// Token storage – access token + refresh token held in memory only
// ---------------------------------------------------------------------------

/** Refresh token from the last successful token exchange or token refresh. */
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
 * Fixed loopback port for the PKCE redirect URI used in the logout callback flow.
 *
 * The port is registered in local-keycloak/egril-realm.json as
 * http://localhost:10001/callback.
 */
private const val PKCE_CALLBACK_PORT = 10001

/**
 * Monotonically increasing counter that identifies the current login attempt.
 * Incremented by [startPlatformLogin] each time a new login begins. The
 * background login thread captures this value; its `finally` block only clears
 * [IamService.loginInProgress] when the stored generation still matches, so a
 * superseded thread cannot accidentally clear the flag for the new attempt.
 */
private val loginGeneration = AtomicInteger(0)

/** Minimum polling interval in seconds to avoid hammering the server (RFC 8628 §3.5). */
private const val MIN_POLL_INTERVAL_SECONDS = 5L

/**
 * Set to `true` to signal the device-auth polling loop to stop.
 *
 * Set by [startPlatformLogin] at the beginning of each call to cancel any
 * in-progress poll, and by [performPlatformLogoutLocal] / [performPlatformLogoutBackchannel]
 * when the user explicitly cancels or switches players.
 */
private val deviceAuthCancelled = AtomicBoolean(false)

// ---------------------------------------------------------------------------
// Device Authorization Grant (RFC 8628) – login flow
// ---------------------------------------------------------------------------

/** Parsed response from the Keycloak device-authorization endpoint. */
private data class DeviceAuthResponse(
    val deviceCode: String,
    val userCode: String,
    val verificationUri: String,
    val verificationUriComplete: String?,
    val expiresIn: Long,
    val interval: Long
)

/**
 * POSTs to the Keycloak device-authorization endpoint and returns the response,
 * or `null` if the request fails or the server returns a non-200 status.
 */
private fun requestDeviceAuth(): DeviceAuthResponse? {
    return try {
        val body = "client_id=${URLEncoder.encode(IamConfig.DESKTOP_CLIENT_ID, "UTF-8")}&scope=openid"
        val conn = URI.create(IamConfig.deviceAuthUrl).toURL().openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        conn.connectTimeout = TOKEN_HTTP_TIMEOUT_MS
        conn.readTimeout = TOKEN_HTTP_TIMEOUT_MS
        conn.outputStream.use { it.write(body.toByteArray()) }

        if (conn.responseCode != 200) {
            val errorBody = try { conn.errorStream?.bufferedReader()?.readText() } catch (_: Exception) { null }
            conn.disconnect()
            println("IAM: device-auth request failed with HTTP ${conn.responseCode}: $errorBody")
            return null
        }
        val json = conn.inputStream.bufferedReader().readText()
        conn.disconnect()

        DeviceAuthResponse(
            deviceCode = extractJsonStringValue(json, "device_code") ?: return null,
            userCode = extractJsonStringValue(json, "user_code") ?: return null,
            verificationUri = extractJsonStringValue(json, "verification_uri") ?: return null,
            verificationUriComplete = extractJsonStringValue(json, "verification_uri_complete"),
            expiresIn = extractJsonNumberValue(json, "expires_in") ?: 300L,
            interval = extractJsonNumberValue(json, "interval") ?: 5L
        )
    } catch (e: Exception) {
        println("IAM: device-auth request failed: ${e.message}")
        null
    }
}

/**
 * Polls the Keycloak token endpoint using the Device Authorization Grant until
 * the user completes login, the code expires, or [deviceAuthCancelled] is set.
 *
 * Returns the [TokenData] on success or `null` on failure / cancellation.
 */
private fun pollDeviceToken(deviceCode: String, interval: Long, expiresIn: Long): TokenData? {
    val deadline = System.currentTimeMillis() + expiresIn * 1_000L
    // Use the server-specified interval but clamp to at least MIN_POLL_INTERVAL_SECONDS
    // to avoid hammering the server (RFC 8628 §3.5).
    val pollIntervalMs = maxOf(interval, MIN_POLL_INTERVAL_SECONDS) * 1_000L

    while (!deviceAuthCancelled.get() && System.currentTimeMillis() < deadline) {
        Thread.sleep(pollIntervalMs)
        if (deviceAuthCancelled.get()) break

        val result = tryExchangeDeviceCode(deviceCode) ?: continue
        return result
    }
    return null
}

/**
 * Sends a single `urn:ietf:params:oauth:grant-type:device_code` token request (RFC 8628 §3.4).
 * Returns [TokenData] on success, `null` if the code is still pending
 * (`authorization_pending` / `slow_down`) or on network errors.
 * Sets [deviceAuthCancelled] on unrecoverable errors like `expired_token`.
 */
private fun tryExchangeDeviceCode(deviceCode: String): TokenData? {
    return try {
        val body = buildString {
            append("grant_type=${URLEncoder.encode("urn:ietf:params:oauth:grant-type:device_code", "UTF-8")}")
            append("&client_id=${URLEncoder.encode(IamConfig.DESKTOP_CLIENT_ID, "UTF-8")}")
            append("&device_code=${URLEncoder.encode(deviceCode, "UTF-8")}")
        }
        val conn = URI.create(IamConfig.tokenUrl).toURL().openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        conn.connectTimeout = TOKEN_HTTP_TIMEOUT_MS
        conn.readTimeout = TOKEN_HTTP_TIMEOUT_MS
        conn.outputStream.use { it.write(body.toByteArray()) }

        if (conn.responseCode == 200) {
            val json = conn.inputStream.bufferedReader().readText()
            conn.disconnect()
            return parseTokenResponse(json)
        }

        // Non-200: read the error body to decide what to do
        val errorJson = try { conn.errorStream?.bufferedReader()?.readText() } catch (_: Exception) { null } ?: ""
        conn.disconnect()
        val error = extractJsonStringValue(errorJson, "error") ?: ""
        when (error) {
            "authorization_pending", "slow_down" -> null  // still waiting – continue polling
            else -> {
                // expired_token, access_denied, or other unrecoverable error
                println("IAM: device-code exchange failed: $error")
                deviceAuthCancelled.set(true)
                null
            }
        }
    } catch (e: Exception) {
        println("IAM: device-code token poll failed: ${e.message}")
        null
    }
}

internal actual fun startPlatformLogin() {
    // Signal any in-progress polling loop to stop before starting a new login.
    deviceAuthCancelled.set(true)

    val myGeneration = loginGeneration.incrementAndGet()
    // Reset cancellation flag AFTER capturing the new generation so the new thread sees false.
    deviceAuthCancelled.set(false)

    Thread {
        try {
            val deviceResponse = requestDeviceAuth() ?: return@Thread

            IamService.deviceAuthState.value = DeviceAuthState(
                userCode = deviceResponse.userCode,
                verificationUri = deviceResponse.verificationUri,
                verificationUriComplete = deviceResponse.verificationUriComplete
            )

            // Try to open the browser for convenience. On a normal desktop this lets
            // the user complete login in one click (verification_uri_complete pre-fills
            // the code). On Steam Deck gaming mode the browser launch silently fails;
            // the user completes login on their phone using the code shown in the dialog.
            val browserUrl = deviceResponse.verificationUriComplete ?: deviceResponse.verificationUri
            openBrowserNewWindow(browserUrl)

            val tokenData = pollDeviceToken(
                deviceCode = deviceResponse.deviceCode,
                interval = deviceResponse.interval,
                expiresIn = deviceResponse.expiresIn
            ) ?: return@Thread

            IamService.state.value = tokenData.toIamState()
            storedRefreshToken = tokenData.refreshToken
            tokenExpiresAtMs = System.currentTimeMillis() + tokenData.expiresInSeconds * 1_000L

            if (tokenData.refreshToken != null) {
                startBackgroundTokenRefresh()
            }
        } catch (_: Exception) {
            // Login errors must never disrupt gameplay
        } finally {
            IamService.deviceAuthState.value = null
            // Only clear loginInProgress for the login attempt that owns this thread.
            if (loginGeneration.get() == myGeneration) {
                IamService.loginInProgress.value = false
            }
        }
    }.also { it.isDaemon = true }.start()
}

internal actual fun performPlatformLogout() {
    storedRefreshToken = null
    tokenExpiresAtMs = 0L
    refreshThreadRunning = false
    deviceAuthCancelled.set(true)
    IamService.loginInProgress.value = false
    // Open the Keycloak logout endpoint in the browser so the SSO session is terminated.
    // We listen on PKCE_CALLBACK_PORT for the post-logout redirect so that we can serve
    // an auto-closing "Logged out" page. The URI http://localhost:10001/* is registered
    // in the Keycloak client, so http://localhost:10001/logout-callback is accepted.

    // Resolve locale and page texts on the calling thread (UI thread) before spawning
    // the background daemon thread.
    val locale = currentLanguage.value
    val logoutPageHeading = "&#x2713; ${LocalizedStrings.get("iam_logout_successful_heading", locale)}"
    val logoutPageMessage = LocalizedStrings.get("iam_logout_successful_message", locale)

    try {
        val logoutCallbackUri = "http://localhost:$PKCE_CALLBACK_PORT/logout-callback"
        val logoutUrl = "${IamConfig.logoutUrl}?client_id=${IamConfig.CLIENT_ID}" +
                "&post_logout_redirect_uri=${URLEncoder.encode(logoutCallbackUri, "UTF-8")}"
        // Spin up a one-shot server BEFORE opening the browser so we don't miss the redirect.
        Thread {
            serveLogoutCallback(locale.code, logoutPageHeading, logoutPageMessage)
        }.also { it.isDaemon = true }.start()
        openBrowserNewWindow(logoutUrl)
    } catch (_: Exception) {
        // Ignore – local state has already been cleared by IamService.logout()
    }
}

/**
 * Clears only in-memory token state without opening the browser or binding the
 * PKCE callback port. Used when switching players or cancelling a login flow.
 */
internal actual fun performPlatformLogoutLocal() {
    storedRefreshToken = null
    tokenExpiresAtMs = 0L
    refreshThreadRunning = false
    deviceAuthCancelled.set(true)
}

/**
 * Revokes the Keycloak session via an HTTP POST (backchannel logout) and clears
 * in-memory token state. This terminates the server-side session without opening
 * a browser window, so a subsequent login can proceed without any port conflict.
 *
 * If no refresh token is stored (e.g. login never completed) the function behaves
 * identically to [performPlatformLogoutLocal].
 */
internal actual fun performPlatformLogoutBackchannel() {
    val refreshToken = storedRefreshToken
    storedRefreshToken = null
    tokenExpiresAtMs = 0L
    refreshThreadRunning = false
    deviceAuthCancelled.set(true)

    if (refreshToken != null) {
        // Fire-and-forget: revoke the server-side session on a background thread.
        // Local state has already been cleared above so the UI updates immediately.
        Thread {
            try {
                val body = buildString {
                    append("client_id=${URLEncoder.encode(IamConfig.CLIENT_ID, "UTF-8")}")
                    append("&refresh_token=${URLEncoder.encode(refreshToken, "UTF-8")}")
                }
                val conn = URI.create(IamConfig.logoutUrl).toURL().openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                conn.connectTimeout = TOKEN_HTTP_TIMEOUT_MS
                conn.readTimeout = TOKEN_HTTP_TIMEOUT_MS
                conn.outputStream.use { it.write(body.toByteArray()) }
                try { conn.inputStream.use { it.readBytes() } } catch (e: Exception) {
                    println("IAM: backchannel logout – could not read response: ${e.message}")
                }
                conn.disconnect()
            } catch (e: Exception) {
                // Backchannel logout failed – local state was already cleared above.
                println("IAM: backchannel logout failed: ${e.message}")
            }
        }.also { it.isDaemon = true }.start()
    }
}

actual suspend fun initPlatformIam() {
    // Desktop login is triggered manually; nothing to restore on startup.
    // The background refresh thread is started after the initial token exchange succeeds.
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
                // Refresh failed (refresh token expired/revoked) – clear local state silently
                // without opening a browser logout page. The user simply needs to log in again.
                storedRefreshToken = null
                tokenExpiresAtMs = 0L
                IamService.state.value = de.egril.defender.iam.IamState()
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
        val conn = URI.create(IamConfig.tokenUrl).toURL().openConnection() as HttpURLConnection
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
        val claims = parseJwtClaims(accessToken)
        return IamState(
            isAuthenticated = true,
            username = claims.username ?: "unknown",
            token = accessToken,
            email = claims.email,
            firstName = claims.firstName,
            lastName = claims.lastName
        )
    }
}

private fun parseTokenResponse(json: String): TokenData? {
    val accessToken = extractJsonStringValue(json, "access_token") ?: return null
    val refreshToken = extractJsonStringValue(json, "refresh_token")
    val expiresIn = extractJsonNumberValue(json, "expires_in") ?: 300L
    return TokenData(accessToken, refreshToken, expiresIn)
}

private fun serveLogoutCallback(langCode: String, heading: String, message: String) {
    try {
        val server = ServerSocket(PKCE_CALLBACK_PORT)
        server.soTimeout = 30_000
        try {
            val socket = server.accept()
            serveAutoClosePage(socket, langCode = langCode, heading = heading, message = message)
        } finally {
            server.close()
        }
    } catch (_: Exception) {
        // Server timeout or bind failure – silently ignored, local state already cleared.
    }
}

/**
 * Writes a self-contained HTML page to [socket] that displays [heading] and [message],
 * then attempts to close the browser window automatically after 2 seconds via JavaScript.
 * Includes security headers to prevent XSS and clickjacking.
 * The [langCode] is used for the HTML lang attribute (e.g. "en", "de").
 * The socket is closed after writing.
 */
private fun serveAutoClosePage(socket: java.net.Socket, langCode: String, heading: String, message: String) {
    val html = """<!DOCTYPE html>
<html lang="$langCode">
<head><meta charset="UTF-8">
<style>body{font-family:sans-serif;text-align:center;padding:40px;}</style>
</head>
<body>
<h2>$heading</h2>
<p>$message</p>
<script>setTimeout(function(){window.open('','_self');window.close();},2000);</script>
</body>
</html>"""
    socket.getOutputStream().write(buildString {
        append("HTTP/1.1 200 OK\r\n")
        append("Content-Type: text/html; charset=UTF-8\r\n")
        append("Content-Security-Policy: default-src 'none'; style-src 'unsafe-inline'; script-src 'unsafe-inline'\r\n")
        append("X-Frame-Options: DENY\r\n")
        append("X-Content-Type-Options: nosniff\r\n")
        append("\r\n")
        append(html)
    }.toByteArray())
    socket.close()
}

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
 * Silently does nothing if a browser is unavailable (e.g. Steam Deck gaming mode).
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
        // On Steam Deck gaming mode this is expected – the user will use the code shown
        // in the in-app dialog to complete login on their phone.
        println("IAM: Failed to open browser: ${e.message}")
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
