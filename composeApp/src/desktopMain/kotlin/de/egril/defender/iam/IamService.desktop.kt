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
            val iamState = exchangeCodeForToken(code, codeVerifier, redirectUri) ?: return@Thread
            IamService.state.value = iamState
        } catch (_: Exception) {
            // Login errors must never disrupt gameplay
        }
    }.also { it.isDaemon = true }.start()
}

internal actual fun performPlatformLogout() {
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
    // Desktop login is triggered manually; nothing to restore on startup
}

// ---------------------------------------------------------------------------
// Private helpers
// ---------------------------------------------------------------------------

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

private fun exchangeCodeForToken(code: String, codeVerifier: String, redirectUri: String): IamState? {
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
        conn.connectTimeout = 10_000
        conn.readTimeout = 10_000
        conn.outputStream.use { it.write(body.toByteArray()) }

        val response = conn.inputStream.bufferedReader().readText()
        conn.disconnect()

        val accessToken = extractJsonStringValue(response, "access_token") ?: return null
        val username = extractUsernameFromJwt(accessToken) ?: "unknown"
        IamState(isAuthenticated = true, username = username, token = accessToken)
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
