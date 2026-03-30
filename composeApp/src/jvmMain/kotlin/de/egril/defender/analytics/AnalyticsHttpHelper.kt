package de.egril.defender.analytics

import de.egril.defender.iam.IamService
import java.net.HttpURLConnection
import java.net.URL

private val backendUrl: String = "https://defender-backend.egril.de"

/**
 * Fire-and-forget HTTP POST to the analytics backend.
 * Runs on a daemon thread so it never blocks the game.
 * Errors are silently swallowed so analytics never disrupts gameplay.
 * If the user is authenticated via IAM, the Bearer token is attached as an optional header.
 */
internal fun postEventJson(eventType: String, levelName: String?, platform: String) {
    val json = buildEventJson(eventType, levelName, platform)
    val targetUrl = "$backendUrl/api/events"
    val token = IamService.getToken()
    Thread {
        try {
            val connection = URL(targetUrl).openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            if (token != null) {
                connection.setRequestProperty("Authorization", "Bearer $token")
            }
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.outputStream.use { it.write(json.toByteArray(Charsets.UTF_8)) }
            connection.responseCode // triggers the request and consumes the response
            connection.disconnect()
        } catch (_: Exception) {
            // Analytics errors must never affect gameplay
        }
    }.also { it.isDaemon = true }.start()
}
