package de.egril.defender.save

import java.net.HttpURLConnection
import java.net.URL

internal val backendBaseUrl: String
    get() = System.getProperty("defender.backend.url")
        ?: System.getenv("DEFENDER_BACKEND_URL")
        // Fall back to the analytics backend URL for backward compatibility
        ?: System.getProperty("analytics.backend.url")
        ?: System.getenv("ANALYTICS_BACKEND_URL")
        ?: "http://localhost:8080"

/**
 * Synchronously POSTs [body] to [path] on the backend and returns the HTTP status code,
 * or -1 if a network/IO error occurs.
 */
internal fun jvmHttpPost(path: String, body: String, token: String): Int {
    return try {
        val connection = URL("$backendBaseUrl$path").openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Authorization", "Bearer $token")
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        val status = connection.responseCode
        connection.disconnect()
        status
    } catch (_: Exception) {
        -1
    }
}

/**
 * Synchronously GETs [path] from the backend with optional [token] as Bearer auth.
 * Returns the response body on success (2xx), or null on error / non-2xx.
 */
internal fun jvmHttpGet(path: String, token: String?): String? {
    return try {
        val connection = URL("$backendBaseUrl$path").openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        if (token != null) connection.setRequestProperty("Authorization", "Bearer $token")
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        val status = connection.responseCode
        if (status !in 200..299) {
            connection.disconnect()
            return null
        }
        val body = connection.inputStream.bufferedReader().readText()
        connection.disconnect()
        body
    } catch (_: Exception) {
        null
    }
}

/**
 * Synchronously GETs [path] from the backend and returns the raw response bytes,
 * or null on error / non-2xx. Used for downloading binary content (e.g. PNG images).
 */
internal fun jvmHttpGetBytes(path: String, token: String?): ByteArray? {
    return try {
        val connection = URL("$backendBaseUrl$path").openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        if (token != null) connection.setRequestProperty("Authorization", "Bearer $token")
        connection.connectTimeout = 10_000
        connection.readTimeout = 30_000
        val status = connection.responseCode
        if (status !in 200..299) {
            connection.disconnect()
            return null
        }
        val bytes = connection.inputStream.readBytes()
        connection.disconnect()
        bytes
    } catch (_: Exception) {
        null
    }
}
