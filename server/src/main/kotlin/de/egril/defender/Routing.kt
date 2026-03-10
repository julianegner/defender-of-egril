package de.egril.defender

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory

private val analyticsLogger = LoggerFactory.getLogger("Analytics")

fun Application.configureRouting() {
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
            call.respond(HttpStatusCode.OK)
        }
    }
}
