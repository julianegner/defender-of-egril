package de.egril.defender

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.slf4j.LoggerFactory

private val startupLogger = LoggerFactory.getLogger("Application")

fun main() {
    embeddedServer(Netty, port = SERVER_PORT, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    configurePlugins()
    val dataSource = try {
        configureDatabase()
    } catch (e: Exception) {
        startupLogger.error("Database initialization failed, starting without persistence: ${e.message}", e)
        null
    }
    configureRouting(dataSource)
}

const val SERVER_PORT = 8080
