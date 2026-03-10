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
            val message = if (event.levelName != null) {
                "[${event.event}] platform=${event.platform} levelName=${event.levelName}"
            } else {
                "[${event.event}] platform=${event.platform}"
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
