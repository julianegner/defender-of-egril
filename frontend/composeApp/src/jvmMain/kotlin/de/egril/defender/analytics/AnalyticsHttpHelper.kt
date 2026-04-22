package de.egril.defender.analytics

import de.egril.defender.iam.IamService
import java.net.HttpURLConnection
import java.net.URI

/**
 * Fire-and-forget HTTP POST to the analytics backend.
 * Runs on a daemon thread so it never blocks the game.
 * Errors are silently swallowed so analytics never disrupts gameplay.
 * If the user is authenticated via IAM, the Bearer token is attached as an optional header.
 */
internal fun postEventJson(eventType: GameEventType, levelName: String?, platform: String, turnNumber: Int? = null) {
    val json = buildEventJson(eventType, levelName, platform, turnNumber)
    val targetUrl = "$backendUrl/api/events"
    val token = IamService.getToken()
    Thread {
        try {
            val connection = URI.create(targetUrl).toURL().openConnection() as HttpURLConnection
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
