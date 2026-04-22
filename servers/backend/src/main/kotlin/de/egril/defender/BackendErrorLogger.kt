package de.egril.defender

import io.ktor.server.application.*
import io.ktor.server.request.*
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicReference
import javax.sql.DataSource

private val errorLogger = LoggerFactory.getLogger("BackendErrors")

/**
 * Persists a backend error to the `backend_errors` database table and logs it via SLF4J.
 * If the database is not available the error is only written to the application log.
 */
fun logBackendError(
    dataSourceRef: AtomicReference<DataSource?>,
    endpoint: String,
    httpMethod: String,
    statusCode: Int,
    message: String
) {
    errorLogger.error("[$statusCode] $httpMethod $endpoint — $message")
    val ds = dataSourceRef.get() ?: return
    try {
        ds.connection.use { conn ->
            conn.prepareStatement(
                "INSERT INTO backend_errors (endpoint, http_method, status_code, message) VALUES (?, ?, ?, ?)"
            ).use { stmt ->
                stmt.setString(1, endpoint)
                stmt.setString(2, httpMethod)
                stmt.setInt(3, statusCode)
                stmt.setString(4, message)
                stmt.executeUpdate()
            }
        }
    } catch (e: Exception) {
        errorLogger.error("Failed to persist backend error to database: ${e.message}", e)
    }
}

/**
 * Installs a pipeline interceptor that automatically logs every HTTP 5xx response
 * (both explicit responds and unhandled exceptions) to the `backend_errors` table.
 */
fun Application.configureErrorLogging(dataSourceRef: AtomicReference<DataSource?>) {
    intercept(ApplicationCallPipeline.Monitoring) {
        try {
            proceed()
        } catch (e: Throwable) {
            logBackendError(
                dataSourceRef,
                call.request.path(),
                call.request.httpMethod.value,
                500,
                e.message ?: "Unhandled exception"
            )
            throw e
        }
        val status = call.response.status() ?: return@intercept
        if (status.value >= 500) {
            logBackendError(
                dataSourceRef,
                call.request.path(),
                call.request.httpMethod.value,
                status.value,
                status.description
            )
        }
    }
}
