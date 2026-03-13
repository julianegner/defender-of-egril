package de.egril.defender

import io.ktor.server.application.*
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import javax.sql.DataSource

private val startupLogger = LoggerFactory.getLogger("Application")

/** Holds the live DataSource. Updated by the background reconnect scheduler. */
val dataSourceRef = AtomicReference<DataSource?>(null)

fun main() {
    embeddedServer(Netty, port = SERVER_PORT, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    startupLogger.info("Starting application")
    startupLogger.info("database enabled: ${environment.config.propertyOrNull("database.enabled")?.getString() }")
    startupLogger.info("database host: ${environment.config.propertyOrNull("database.host")?.getString() }")

    configurePlugins()
    val initialDs = tryConnectDatabase(attempt = 1)
    dataSourceRef.set(initialDs)
    logDataSourceStatus(initialDs, source = "startup")

    if (initialDs == null && isDatabaseConfigured()) {
        startDatabaseReconnectScheduler()
    }

    configureRouting(dataSourceRef)
}

/**
 * Single attempt to connect to the database and run Liquibase migrations.
 * Returns the DataSource on success, or null if not configured.
 * Swallows exceptions and returns null so the backend can start without a DB.
 */
private fun Application.tryConnectDatabase(attempt: Int): DataSource? {
    if (!isDatabaseConfigured()) return null
    return try {
        val ds = configureDatabase()
        if (ds != null) startupLogger.info("Database connected on attempt $attempt.")
        ds
    } catch (e: Exception) {
        startupLogger.warn("Database connection attempt $attempt failed: ${e.message}")
        null
    }
}

/**
 * Returns true when a database host is configured and the feature is not explicitly disabled.
 */
private fun Application.isDatabaseConfigured(): Boolean {
    if (environment.config.propertyOrNull("database.enabled")?.getString() == "false") return false
    return environment.config.propertyOrNull("database.host")?.getString() != null
}

/**
 * Starts a single-thread scheduler that retries the database connection every 60 s
 * while [dataSourceRef] is null. Once a connection is established the scheduler stops.
 * Registers an application lifecycle hook to shut the executor down gracefully on stop.
 */
private fun Application.startDatabaseReconnectScheduler() {
    startupLogger.info("Database not available at startup. Starting background reconnect scheduler (every 60 s).")
    val scheduler = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "db-reconnect").also { it.isDaemon = true }
    }
    var attemptCount = 1
    scheduler.scheduleWithFixedDelay(
        {
            if (dataSourceRef.get() != null) {
                startupLogger.info("Database reconnect scheduler: datasource is available, stopping scheduler.")
                scheduler.shutdown()
                return@scheduleWithFixedDelay
            }
            attemptCount++
            startupLogger.info("Database reconnect scheduler: attempt $attemptCount...")
            val ds = tryConnectDatabase(attempt = attemptCount)
            if (ds != null) {
                dataSourceRef.set(ds)
                logDataSourceStatus(ds, source = "reconnect scheduler (attempt $attemptCount)")
                scheduler.shutdown()
            } else {
                startupLogger.warn(
                    "Database reconnect scheduler: attempt $attemptCount failed. " +
                        "Next retry in 60 s. Current datasource status: UNAVAILABLE"
                )
            }
        },
        60L, 60L, TimeUnit.SECONDS
    )
    // Shut down the scheduler when the application stops (e.g. during tests or graceful shutdown).
    monitor.subscribe(ApplicationStopped) {
        if (!scheduler.isShutdown) {
            startupLogger.info("Application stopping — shutting down database reconnect scheduler.")
            scheduler.shutdown()
        }
    }
}

/** Logs a one-line summary of the current datasource state. */
private fun logDataSourceStatus(dataSource: DataSource?, source: String) {
    if (dataSource != null) {
        startupLogger.info("Datasource status [$source]: CONNECTED")
    } else {
        startupLogger.warn("Datasource status [$source]: UNAVAILABLE — savefile endpoints will return 503 until DB is reachable")
    }
}

const val SERVER_PORT = 8080
