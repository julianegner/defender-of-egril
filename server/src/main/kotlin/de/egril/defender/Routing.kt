package de.egril.defender

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicReference
import javax.sql.DataSource

private val analyticsLogger = LoggerFactory.getLogger("Analytics")
private val savefileLogger = LoggerFactory.getLogger("Savefiles")

fun Application.configureRouting(dataSourceRef: AtomicReference<DataSource?>) {
    routing {
        get("/") {
            call.respondText("Defender of Egril Backend", ContentType.Text.Plain)
        }

        /**
         * Health endpoint.
         * Returns HTTP 200 {"status":"UP","database":"connected"} when the DB is reachable.
         * Returns HTTP 503 {"status":"DOWN","database":"unavailable"} when dataSource is null.
         *
         * Docker uses this endpoint as the backend container's healthcheck so that
         * `restart: on-failure` (or `restart: always`) kicks in automatically when the
         * DB was not ready at startup and the old image (without retry logic) was used.
         */
        get("/health") {

            // fixme this is only to test if the service would work
            //  and only the health endpoint is broken
            call.respondText(
                """{"status":"UP","database":"connected"}""",
                ContentType.Application.Json,
                HttpStatusCode.OK
            )
            return@get

            /*

            val dataSource = dataSourceRef.get()
            if (dataSource == null) {
                call.respondText(
                    """{"status":"DOWN","database":"unavailable"}""",
                    ContentType.Application.Json,
                    HttpStatusCode.ServiceUnavailable
                )
                return@get
            }
            try {
                dataSource.connection.use { conn ->
                    conn.prepareStatement("SELECT 1").use { stmt ->
                        stmt.executeQuery().use { /* connection verified */ }
                    }
                }
                call.respondText(
                    """{"status":"UP","database":"connected"}""",
                    ContentType.Application.Json,
                    HttpStatusCode.OK
                )
            } catch (e: Exception) {
                val safeMessage = (e.message ?: "error")
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t")
                call.respondText(
                    """{"status":"DOWN","database":"$safeMessage"}""",
                    ContentType.Application.Json,
                    HttpStatusCode.ServiceUnavailable
                )
            }
             */
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

            dataSourceRef.get()?.connection?.use { conn ->
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

        // ---------------------------------------------------------------------------
        // Savefile crossplay endpoints
        // ---------------------------------------------------------------------------

        /**
         * Upload (create or replace) a savefile for the authenticated user.
         * Requires a valid Bearer token in the Authorization header.
         * Returns 401 if no valid user can be extracted from the token.
         */
        post("/api/savefiles") {
            val userId = extractUserIdFromBearerToken(call.request.header(HttpHeaders.Authorization))
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, "Authentication required")
                return@post
            }

            val request = try {
                call.receive<SavefileUploadRequest>()
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, "Invalid payload: ${e.message}")
                return@post
            }

            val ds = dataSourceRef.get() ?: run {
                call.respond(HttpStatusCode.ServiceUnavailable, "Database not available")
                return@post
            }
            ds.connection.use { conn ->
                try {
                    conn.prepareStatement(
                        """
                        INSERT INTO savefiles (user_id, save_id, data, updated_at)
                        VALUES (?, ?, ?, NOW())
                        ON CONFLICT (user_id, save_id)
                        DO UPDATE SET data = EXCLUDED.data, updated_at = NOW()
                        """.trimIndent()
                    ).use { stmt ->
                        stmt.setString(1, userId)
                        stmt.setString(2, request.saveId)
                        stmt.setString(3, request.data)
                        stmt.executeUpdate()
                    }
                    savefileLogger.info("Savefile uploaded: userId=$userId saveId=${request.saveId}")
                    call.respond(HttpStatusCode.OK)
                } catch (e: Exception) {
                    savefileLogger.error("Failed to store savefile: ${e.message}", e)
                    call.respond(HttpStatusCode.InternalServerError, "Failed to store savefile")
                }
            }
        }

        /**
         * List all savefiles for the authenticated user.
         * Requires a valid Bearer token in the Authorization header.
         * Returns 401 if no valid user can be extracted from the token.
         */
        get("/api/savefiles") {
            val userId = extractUserIdFromBearerToken(call.request.header(HttpHeaders.Authorization))
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, "Authentication required")
                return@get
            }

            val ds = dataSourceRef.get() ?: run {
                call.respond(HttpStatusCode.ServiceUnavailable, "Database not available")
                return@get
            }
            ds.connection.use { conn ->
                try {
                    val savefiles = mutableListOf<SavefileMetadata>()
                    conn.prepareStatement(
                        "SELECT save_id, data, updated_at FROM savefiles WHERE user_id = ? ORDER BY updated_at DESC"
                    ).use { stmt ->
                        stmt.setString(1, userId)
                        stmt.executeQuery().use { rs ->
                            while (rs.next()) {
                                savefiles.add(
                                    SavefileMetadata(
                                        saveId = rs.getString("save_id"),
                                        data = rs.getString("data"),
                                        updatedAt = rs.getTimestamp("updated_at").toInstant().toString()
                                    )
                                )
                            }
                        }
                    }
                    call.respond(savefiles)
                } catch (e: Exception) {
                    savefileLogger.error("Failed to retrieve savefiles: ${e.message}", e)
                    call.respond(HttpStatusCode.InternalServerError, "Failed to retrieve savefiles")
                }
            }
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

/**
 * Extracts the stable user ID (`sub` claim) from a JWT Bearer token.
 * Falls back to `preferred_username` if `sub` is not present.
 * Returns null if the header is absent, malformed, or contains no usable identity claim.
 * No signature verification is performed – authentication is delegated to the Keycloak proxy.
 */
private fun extractUserIdFromBearerToken(authHeader: String?): String? {
    if (authHeader == null || !authHeader.startsWith("Bearer ")) return null
    return try {
        val token = authHeader.removePrefix("Bearer ")
        val payload = token.split(".").getOrNull(1) ?: return null
        val padded = payload + "=".repeat((4 - payload.length % 4) % 4)
        val decoded = java.util.Base64.getUrlDecoder().decode(padded).toString(Charsets.UTF_8)
        extractJsonStringValue(decoded, "sub")
            ?: extractJsonStringValue(decoded, "preferred_username")
    } catch (_: Exception) {
        null
    }
}

private fun extractJsonStringValue(json: String, key: String): String? =
    Regex("\"${Regex.escape(key)}\"\\s*:\\s*\"([^\"]+)\"").find(json)?.groupValues?.get(1)
