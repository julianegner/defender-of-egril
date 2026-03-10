package de.egril.defender

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor
import org.slf4j.LoggerFactory
import javax.sql.DataSource

private val dbLogger = LoggerFactory.getLogger("Database")

fun Application.configureDatabase(): DataSource? {
    // Explicitly disabled via config
    if (environment.config.propertyOrNull("database.enabled")?.getString() == "false") {
        dbLogger.info("Database connection disabled via configuration.")
        return null
    }

    // No host configured → skip (handles environments where no application.conf is loaded)
    val host = environment.config.propertyOrNull("database.host")?.getString()
        ?: return null.also { dbLogger.info("No database host configured, skipping database connection.") }

    val port = environment.config.propertyOrNull("database.port")?.getString() ?: "5432"
    val name = environment.config.propertyOrNull("database.name")?.getString() ?: "defenderofegril"
    val user = environment.config.propertyOrNull("database.user")?.getString() ?: "defender"
    val password = environment.config.propertyOrNull("database.password")?.getString() ?: "defender"

    val jdbcUrl = "jdbc:postgresql://$host:$port/$name"
    dbLogger.info("Connecting to database at $jdbcUrl")

    val hikariConfig = HikariConfig().apply {
        this.jdbcUrl = jdbcUrl
        this.username = user
        this.password = password
        driverClassName = "org.postgresql.Driver"
        maximumPoolSize = 10
        minimumIdle = 2
        idleTimeout = 600_000
        connectionTimeout = 30_000
    }

    val dataSource = HikariDataSource(hikariConfig)
    runLiquibaseMigrations(dataSource)
    dbLogger.info("Database connection established.")
    return dataSource
}

private fun runLiquibaseMigrations(dataSource: DataSource) {
    dbLogger.info("Running Liquibase migrations...")
    // Save and restore the thread context classloader to prevent Liquibase from
    // corrupting it, which would cause NoClassDefFoundError for application classes
    // on subsequent requests handled by Netty I/O threads.
    val thread = Thread.currentThread()
    val appClassLoader = thread.contextClassLoader
    try {
        dataSource.connection.use { connection ->
            val database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(JdbcConnection(connection))
            val liquibase = Liquibase(
                "db/changelog/db.changelog-master.xml",
                ClassLoaderResourceAccessor(appClassLoader),
                database
            )
            liquibase.update("") // empty string means apply all pending changesets up to the latest
        }
    } finally {
        thread.contextClassLoader = appClassLoader
    }
    dbLogger.info("Liquibase migrations completed.")
}
