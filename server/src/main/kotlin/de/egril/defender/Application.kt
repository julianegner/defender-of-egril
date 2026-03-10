package de.egril.defender

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
    embeddedServer(Netty, port = SERVER_PORT, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    configurePlugins()
    val dataSource = configureDatabase()
    configureRouting(dataSource)
}

const val SERVER_PORT = 8080
