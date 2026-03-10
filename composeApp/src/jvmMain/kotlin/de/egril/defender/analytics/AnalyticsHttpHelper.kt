package de.egril.defender.analytics

import java.net.HttpURLConnection
import java.net.URL

private val backendUrl: String
    get() = System.getProperty("analytics.backend.url")
        ?: System.getenv("ANALYTICS_BACKEND_URL")
        ?: "http://localhost:8080"

/**
 * Fire-and-forget HTTP POST to the analytics backend.
 * Runs on a daemon thread so it never blocks the game.
 * Errors are silently swallowed so analytics never disrupts gameplay.
 */
internal fun postEventJson(eventType: String, levelName: String?, platform: String) {
    val json = buildEventJson(eventType, levelName, platform)
    val targetUrl = "$backendUrl/api/events"
    Thread {
        try {
            val connection = URL(targetUrl).openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
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
