package de.egril.defender

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor
import org.junit.AfterClass
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.MountableFile
import java.net.HttpURLConnection
import java.net.URI
import java.time.Duration
import javax.sql.DataSource
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

/**
 * End-to-end tests that verify cross-platform play with a fully containerized stack:
 * Keycloak (identity provider) + PostgreSQL (database) + Ktor backend.
 *
 * These tests use real Keycloak-issued JWT tokens obtained via the Resource Owner Password
 * Credentials (ROPC) flow, which is enabled on the `defender-of-egril-cli` client in the
 * test realm. The backend receives these tokens exactly as it would in production (except
 * that Keycloak sits behind the KTOR test engine rather than a reverse proxy).
 *
 * The test realm is imported from [REALM_RESOURCE] which lives in src/test/resources/.
 * It defines:
 *   - The `egril` realm
 *   - The `defender-of-egril-cli` public client with direct access grants enabled
 *   - Two test users: `e2e-player-a` and `e2e-player-b` (password: `e2e-test-password`)
 *
 * These tests are intentionally separated from the regular unit/integration tests because
 * Keycloak startup is slow (~60-90 s). Run them with:
 *   ./gradlew :server:e2eTest
 *
 * The Keycloak container is skipped automatically when Docker is unavailable.
 */
class EndToEndTest {

    companion object {
        private const val REALM_RESOURCE = "egril-realm-e2e.json"
        private const val REALM = "egril"
        private const val CLIENT_ID = "defender-of-egril-cli"
        private const val PLAYER_A_USERNAME = "e2e-player-a"
        private const val PLAYER_B_USERNAME = "e2e-player-b"
        private const val PLAYER_PASSWORD = "e2e-test-password"
        private const val KEYCLOAK_IMAGE = "quay.io/keycloak/keycloak:24.0"

        /** Single PostgreSQL container shared across all tests in this class. */
        private val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16-alpine")
            .apply { start() }

        /** Single Keycloak container shared across all tests in this class. */
        private val keycloak: GenericContainer<*> = GenericContainer(KEYCLOAK_IMAGE)
            .withExposedPorts(8080)
            .withEnv("KEYCLOAK_ADMIN", "admin")
            .withEnv("KEYCLOAK_ADMIN_PASSWORD", "admin")
            .withCopyFileToContainer(
                MountableFile.forClasspathResource(REALM_RESOURCE),
                "/opt/keycloak/data/import/$REALM_RESOURCE"
            )
            .withCommand("start-dev --import-realm --health-enabled=true")
            .waitingFor(
                // Wait for the realm-specific OIDC discovery endpoint rather than the generic
                // /health/ready endpoint. In Keycloak 24 dev mode, /health/ready can return 200
                // before the realm import completes. The OIDC discovery endpoint only becomes
                // available once the realm is fully imported and ready to issue tokens.
                Wait.forHttp("/realms/$REALM/.well-known/openid-configuration")
                    .forPort(8080)
                    .forStatusCode(200)
                    .withStartupTimeout(Duration.ofMinutes(3))
            )
            .apply { start() }

        /** DataSource connected to the Testcontainers PostgreSQL instance. */
        private val testDataSource: HikariDataSource by lazy {
            val config = HikariConfig().apply {
                jdbcUrl = postgres.jdbcUrl
                username = postgres.username
                password = postgres.password
                driverClassName = "org.postgresql.Driver"
                maximumPoolSize = 5
            }
            HikariDataSource(config).also { ds -> runMigrations(ds) }
        }

        @JvmStatic
        @AfterClass
        fun tearDown() {
            runCatching { testDataSource.close() }
            runCatching { keycloak.stop() }
            postgres.stop()
        }

        private fun runMigrations(dataSource: DataSource) {
            val thread = Thread.currentThread()
            val loader = thread.contextClassLoader
            try {
                dataSource.connection.use { conn ->
                    val db = DatabaseFactory.getInstance()
                        .findCorrectDatabaseImplementation(JdbcConnection(conn))
                    Liquibase(
                        "db/changelog/db.changelog-master.xml",
                        ClassLoaderResourceAccessor(loader),
                        db
                    ).update("")
                }
            } finally {
                thread.contextClassLoader = loader
            }
        }

        /**
         * Obtains a real access token from Keycloak using the Resource Owner Password
         * Credentials (ROPC) flow. This is the same flow the API client scripts use.
         *
         * The returned token is a genuine Keycloak-issued JWT that the backend can parse
         * with [extractUserIdFromBearerToken] in exactly the same way as production tokens.
         */
        private fun getKeycloakToken(username: String, password: String): String {
            val keycloakUrl = "http://localhost:${keycloak.getMappedPort(8080)}"
            val tokenUrl = "$keycloakUrl/realms/$REALM/protocol/openid-connect/token"
            val body = "grant_type=password&client_id=$CLIENT_ID&username=$username&password=$password"

            val connection = URI(tokenUrl).toURL().openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                connection.outputStream.use { it.write(body.toByteArray()) }

                val responseCode = connection.responseCode
                val responseBody = if (responseCode < 400) {
                    connection.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
                } else {
                    val errorBody = connection.errorStream?.use { it.readBytes().toString(Charsets.UTF_8) }
                        ?: "<no error body>"
                    error(
                        "Keycloak token request failed with HTTP $responseCode for user '$username': $errorBody"
                    )
                }
                val json = Json.parseToJsonElement(responseBody).jsonObject
                return checkNotNull(json["access_token"]?.jsonPrimitive?.content) {
                    "Keycloak token response did not contain 'access_token': $responseBody"
                }
            } finally {
                connection.disconnect()
            }
        }

        private fun jsonString(value: String): String = Json.encodeToString(JsonPrimitive(value))
    }

    private fun withRealDatabase(block: suspend ApplicationTestBuilder.() -> Unit) =
        testApplication {
            application {
                configurePlugins()
                dataSourceRef.set(testDataSource)
                configureRouting(dataSourceRef)
            }
            block()
        }

    // -------------------------------------------------------------------------
    // Keycloak token validation
    // -------------------------------------------------------------------------

    @Test
    fun `Keycloak issues valid access tokens that the backend accepts`() = withRealDatabase {
        val token = getKeycloakToken(PLAYER_A_USERNAME, PLAYER_PASSWORD)
        assertNotNull(token, "Keycloak must issue a non-null token")
        assertFalse(token.isBlank(), "Token must not be blank")
        // A Keycloak JWT has exactly three dot-separated parts
        assertEquals(3, token.split(".").size, "Token must be a valid JWT with three parts")

        // The backend accepts requests authenticated with real Keycloak tokens
        client.get("/api/savefiles") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.apply {
            assertEquals(
                HttpStatusCode.OK, status,
                "Backend must accept real Keycloak tokens for authenticated endpoints"
            )
        }
    }

    @Test
    fun `Keycloak issues distinct tokens for different users`() {
        val tokenA = getKeycloakToken(PLAYER_A_USERNAME, PLAYER_PASSWORD)
        val tokenB = getKeycloakToken(PLAYER_B_USERNAME, PLAYER_PASSWORD)

        assertFalse(tokenA == tokenB, "Tokens for different users must be different")

        // Extract 'sub' from each token to verify they are different identities
        fun extractSub(token: String): String {
            val payload = token.split(".")[1]
            val padded = payload + "=".repeat((4 - payload.length % 4) % 4)
            val decoded = java.util.Base64.getUrlDecoder().decode(padded).toString(Charsets.UTF_8)
            return Json.parseToJsonElement(decoded).jsonObject["sub"]!!.jsonPrimitive.content
        }

        val subA = extractSub(tokenA)
        val subB = extractSub(tokenB)
        assertFalse(subA == subB, "Different users must have different 'sub' claims")
    }

    // -------------------------------------------------------------------------
    // End-to-end cross-platform continuation with real tokens
    // -------------------------------------------------------------------------

    @Test
    fun `player saves on Web with real token and retrieves the save on Android`() = withRealDatabase {
        val token = getKeycloakToken(PLAYER_A_USERNAME, PLAYER_PASSWORD)
        val saveData = """{"level":3,"coins":750,"health":9,"worldMap":{"unlocked":["level-1","level-2"]}}"""

        // Simulate saving from the Web platform
        client.post("/api/savefiles") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"saveId":"e2e-world-save","data":${jsonString(saveData)},"platform":"WEB"}""")
        }.apply {
            assertEquals(HttpStatusCode.OK, status, "Web save with real token must succeed")
        }

        // Simulate loading from the Android platform (same token = same user)
        client.get("/api/savefiles") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.apply {
            assertEquals(HttpStatusCode.OK, status, "Android load with real token must succeed")
            val body = bodyAsText()
            assertContains(body, "e2e-world-save")
            assertContains(body, "level-2")
            assertContains(body, "750")
        }
    }

    @Test
    fun `player XP and abilities are preserved across platforms with real tokens`() = withRealDatabase {
        val token = getKeycloakToken(PLAYER_A_USERNAME, PLAYER_PASSWORD)
        val userData = """{"localUsername":"$PLAYER_A_USERNAME","xp":3500,"abilities":{"shield":2,"fireball":1},"levelProgress":{"level-1":"GOLD"}}"""

        // Upload from Desktop
        client.post("/api/userdata") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"data":${jsonString(userData)},"platform":"DESKTOP"}""")
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        // Load from iOS
        client.get("/api/userdata") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            val body = bodyAsText()
            assertContains(body, "3500")
            assertContains(body, "fireball")
            assertContains(body, "GOLD")
        }
    }

    @Test
    fun `settings sync across platforms with real tokens`() = withRealDatabase {
        val token = getKeycloakToken(PLAYER_A_USERNAME, PLAYER_PASSWORD)
        val settings = """{"language":"fr","darkMode":true,"soundEnabled":true}"""

        // Save on Android
        client.post("/api/settings") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"data":${jsonString(settings)},"platform":"ANDROID"}""")
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        // Load on Web
        client.get("/api/settings") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            val body = bodyAsText()
            assertContains(body, "fr")
            assertContains(body, "darkMode")
        }
    }

    @Test
    fun `two different players have isolated data with real tokens`() = withRealDatabase {
        val tokenA = getKeycloakToken(PLAYER_A_USERNAME, PLAYER_PASSWORD)
        val tokenB = getKeycloakToken(PLAYER_B_USERNAME, PLAYER_PASSWORD)

        // Player A saves on Web
        client.post("/api/savefiles") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $tokenA")
            setBody("""{"saveId":"e2e-isolation-save","data":${jsonString("""{"secret":"player-a-data","coins":9999}""")},"platform":"WEB"}""")
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        // Player B saves on Android with the same saveId
        client.post("/api/savefiles") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $tokenB")
            setBody("""{"saveId":"e2e-isolation-save","data":${jsonString("""{"secret":"player-b-data","coins":1}""")},"platform":"ANDROID"}""")
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        // Player A must still see only their own data
        client.get("/api/savefiles") {
            header(HttpHeaders.Authorization, "Bearer $tokenA")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            val body = bodyAsText()
            assertContains(body, "player-a-data")
            assertFalse(body.contains("player-b-data"), "Player A must not see Player B's data")
        }

        // Player B must see only their own data
        client.get("/api/savefiles") {
            header(HttpHeaders.Authorization, "Bearer $tokenB")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            val body = bodyAsText()
            assertContains(body, "player-b-data")
            assertFalse(body.contains("player-a-data"), "Player B must not see Player A's data")
        }
    }

    @Test
    fun `complete end-to-end cross-platform session with real Keycloak tokens`() = withRealDatabase {
        val token = getKeycloakToken(PLAYER_A_USERNAME, PLAYER_PASSWORD)

        // -- Session 1: Web --
        // User starts playing on Web: completes level 1, earns XP, unlocks fireball
        client.post("/api/userdata") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"data":${jsonString("""{"localUsername":"$PLAYER_A_USERNAME","xp":300,"abilities":{"fireball":1},"levelProgress":{"level-1":"COMPLETED"}}""")},"platform":"WEB"}""")
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        client.post("/api/savefiles") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"saveId":"e2e-main","data":${jsonString("""{"currentLevel":2,"coins":600,"health":10}""")},"platform":"WEB"}""")
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        client.post("/api/settings") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"data":${jsonString("""{"language":"it","darkMode":false}""")},"platform":"WEB"}""")
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        // -- Session 2: Android --
        // User continues on Android: completes level 2, gains more XP
        client.post("/api/userdata") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"data":${jsonString("""{"localUsername":"$PLAYER_A_USERNAME","xp":700,"abilities":{"fireball":1,"shield":1},"levelProgress":{"level-1":"COMPLETED","level-2":"COMPLETED"}}""")},"platform":"ANDROID"}""")
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        client.post("/api/savefiles") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"saveId":"e2e-main","data":${jsonString("""{"currentLevel":3,"coins":1200,"health":8}""")},"platform":"ANDROID"}""")
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        // -- Verify on Desktop: all state from Web + Android sessions is present --
        client.get("/api/userdata") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            val body = bodyAsText()
            assertContains(body, "700")
            assertContains(body, "shield")
            assertContains(body, "level-2")
        }

        client.get("/api/savefiles") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            val body = bodyAsText()
            assertContains(body, "1200")
            assertContains(body, "currentLevel")
        }

        client.get("/api/settings") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            val body = bodyAsText()
            assertContains(body, "it")
        }
    }
}
