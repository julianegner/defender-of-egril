package de.egril.defender

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory
import javax.sql.DataSource

private val analyticsLogger = LoggerFactory.getLogger("Analytics")

fun Application.configureRouting(dataSource: DataSource? = null) {
    routing {
        get("/") {
            call.respondText("Defender of Egril Backend", ContentType.Text.Plain)
        }

        post("/api/events") {
            val event = try {
                call.receive<GameEvent>()
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, "Invalid event payload: ${e.message}")
                return@post
            }

            // Optionally extract the authenticated username from the Bearer token.
            // Authentication is not required – the token is only used for audit logging.
            val authUser = extractUsernameFromBearerToken(call.request.header(HttpHeaders.Authorization))

            val message = buildString {
                append("[${event.event}] platform=${event.platform}")
                if (event.levelName != null) append(" levelName=${event.levelName}")
                if (authUser != null) append(" user=$authUser")
            }
            analyticsLogger.info(message)

            dataSource?.connection?.use { conn ->
                try {
                    conn.prepareStatement(
                        "INSERT INTO events (event_type, platform, level_name) VALUES (?, ?, ?)"
                    ).use { stmt ->
                        stmt.setString(1, event.event)
                        stmt.setString(2, event.platform)
                        stmt.setString(3, event.levelName)
                        stmt.executeUpdate()
                    }
                } catch (e: Exception) {
                    analyticsLogger.error("Failed to persist event to database: ${e.message}", e)
                }
            }

            call.respond(HttpStatusCode.OK)
        }
    }
}

/**
 * Extracts the `preferred_username` (or `sub`) from a JWT Bearer token without
 * requiring a full JOSE library. Returns null if the header is absent or malformed.
 * This is for audit-logging only – no signature verification is performed.
 */
private fun extractUsernameFromBearerToken(authHeader: String?): String? {
    if (authHeader == null || !authHeader.startsWith("Bearer ")) return null
    return try {
        val token = authHeader.removePrefix("Bearer ")
        val payload = token.split(".").getOrNull(1) ?: return null
        val padded = payload + "=".repeat((4 - payload.length % 4) % 4)
        val decoded = java.util.Base64.getUrlDecoder().decode(padded).toString(Charsets.UTF_8)
        extractJsonStringValue(decoded, "preferred_username")
            ?: extractJsonStringValue(decoded, "sub")
    } catch (_: Exception) {
        null
    }
}

private fun extractJsonStringValue(json: String, key: String): String? =
    Regex("\"${Regex.escape(key)}\"\\s*:\\s*\"([^\"]+)\"").find(json)?.groupValues?.get(1)
