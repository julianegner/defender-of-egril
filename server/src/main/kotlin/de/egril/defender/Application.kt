package de.egril.defender

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.slf4j.LoggerFactory
import javax.sql.DataSource
import kotlin.system.exitProcess

private val startupLogger = LoggerFactory.getLogger("Application")

fun main() {
    embeddedServer(Netty, port = SERVER_PORT, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    configurePlugins()
    val dataSource = initializeDatabaseWithRetry()
    configureRouting(dataSource)
}

/**
 * Attempts to initialize the database connection and run migrations, retrying on failure.
 *
 * If the database is configured but every attempt fails (e.g. the DB is not yet ready when
 * the container starts, or a Liquibase migration lock is held by a previous run), the process
 * exits with code 1 so that Docker's `restart: on-failure` policy will restart the container.
 *
 * If no database is configured (host not set or database.enabled=false) the function returns
 * null without retrying, which is the intended "no-persistence" mode used in tests.
 */
private fun Application.initializeDatabaseWithRetry(): DataSource? {
    // Check early whether a database is even configured to avoid unnecessary retries.
    if (environment.config.propertyOrNull("database.enabled")?.getString() == "false") {
        return null
    }
    if (environment.config.propertyOrNull("database.host")?.getString() == null) {
        return null
    }

    val maxAttempts = 10
    val retryDelayMs = 5_000L
    var lastException: Exception? = null

    for (attempt in 1..maxAttempts) {
        try {
            val ds = configureDatabase()
            if (ds != null) return ds
            // configureDatabase() returned null explicitly (disabled/unconfigured), stop retrying.
            return null
        } catch (e: Exception) {
            lastException = e
            if (attempt < maxAttempts) {
                startupLogger.warn(
                    "Database initialization attempt $attempt/$maxAttempts failed: ${e.message}. " +
                        "Retrying in ${retryDelayMs / 1000}s..."
                )
                Thread.sleep(retryDelayMs)
            }
        }
    }

    startupLogger.error(
        "Database initialization failed after $maxAttempts attempts. " +
            "Exiting so the container can be restarted. Last error: ${lastException?.message}",
        lastException
    )
    exitProcess(1)
}

const val SERVER_PORT = 8080
