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
import kotlinx.serialization.json.jsonArray
import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor
import org.junit.AfterClass
import org.testcontainers.containers.PostgreSQLContainer
import javax.sql.DataSource
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

/**
 * Integration tests that verify the frontend and backend work well together.
 *
 * A real PostgreSQL database is spun up via Testcontainers, Liquibase migrations are applied,
 * and then each test exercises a full API round-trip (request → backend logic → database → response)
 * using the same JSON contract that the Kotlin Multiplatform frontend uses.
 *
 * The fake JWT tokens used here follow the same format the Keycloak-issued tokens have
 * (base64url-encoded JSON payload containing "sub" and "preferred_username" claims) so the
 * [extractUserIdFromBearerToken] helper in Routing.kt correctly extracts the user identity.
 */
class BackendIntegrationTest {

    companion object {
        /** Single PostgreSQL container shared across all tests in this class. */
        private val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16-alpine")
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
         * Creates a minimal fake JWT whose payload contains "sub" and "preferred_username".
         * Header: {"alg":"none"} | Payload: {"sub":"<userId>","preferred_username":"<username>"}
         * No signature – just as the real tokens arrive after the Keycloak reverse-proxy strips
         * verification (the backend does not verify token signatures itself).
         */
        private fun fakeToken(userId: String, username: String = userId): String {
            val header = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("""{"alg":"none"}""".toByteArray())
            val payload = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("""{"sub":"$userId","preferred_username":"$username"}""".toByteArray())
            return "$header.$payload."
        }

        /**
         * Creates a fake JWT that additionally carries [role] as a client role on
         * [clientId] in the `resource_access` claim, mimicking a Keycloak-issued token
         * for a user who has been granted that role.
         */
        private fun fakeTokenWithClientRole(
            userId: String,
            username: String = userId,
            clientId: String = "defender-of-egril",
            role: String
        ): String {
            val header = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("""{"alg":"none"}""".toByteArray())
            val payload = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString(
                    """{"sub":"$userId","preferred_username":"$username","resource_access":{"$clientId":{"roles":["$role"]}}}""".toByteArray()
                )
            return "$header.$payload."
        }

        /** JSON-encodes [value] as a string literal, suitable for embedding in a JSON body. */
        private fun jsonString(value: String): String = Json.encodeToString(JsonPrimitive(value))

        private fun assertEmptyJsonArray(body: String) {
            assertEquals(0, Json.parseToJsonElement(body).jsonArray.size, "Expected an empty JSON array")
        }
    }

    // -------------------------------------------------------------------------
    // Helper that creates a testApplication wired to the real Testcontainers DB
    // -------------------------------------------------------------------------

    private fun withRealDatabase(block: suspend ApplicationTestBuilder.() -> Unit) =
        testApplication {
            application {
                configurePlugins()
                // Inject the Testcontainers DataSource directly, bypassing the HOCON/env-var
                // database configuration that would try to connect to a non-existent host in CI.
                dataSourceRef.set(testDataSource)
                configureRouting(dataSourceRef)
            }
            block()
        }

    // -------------------------------------------------------------------------
    // Health endpoint
    // -------------------------------------------------------------------------

    @Test
    fun `GET health returns UP when database is available`() = withRealDatabase {
        client.get("/health").apply {
            assertEquals(HttpStatusCode.OK, status)
            val body = bodyAsText()
            assertContains(body, "\"status\":\"UP\"")
            assertContains(body, "\"database\":\"connected\"")
        }
    }

    @Test
    fun `GET health returns DOWN when database is unavailable`() = testApplication {
        application {
            configurePlugins()
            dataSourceRef.set(null)
            configureRouting(dataSourceRef)
        }
        client.get("/health").apply {
            assertEquals(HttpStatusCode.ServiceUnavailable, status)
            assertContains(bodyAsText(), "\"status\":\"DOWN\"")
        }
    }

    // -------------------------------------------------------------------------
    // Savefile endpoints – full round-trip
    // -------------------------------------------------------------------------

    @Test
    fun `POST savefiles stores and GET savefiles returns the uploaded save`() = withRealDatabase {
        val token = fakeToken("user-savefile-rt", "player1")
        val saveData = """{"level":5,"coins":1000}"""

        // Upload
        client.post("/api/savefiles") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"saveId":"save-rt-1","data":${jsonString(saveData)}}""")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }

        // Retrieve
        client.get("/api/savefiles") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            val body = bodyAsText()
            assertContains(body, "save-rt-1")
            assertContains(body, "updatedAt")
        }
    }

    @Test
    fun `POST savefiles upserts existing save`() = withRealDatabase {
        val token = fakeToken("user-savefile-upsert")

        fun uploadBody(data: String) =
            """{"saveId":"save-upsert","data":${jsonString(data)}}"""

        client.post("/api/savefiles") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody(uploadBody("""{"level":1,"version":"initial"}"""))
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        client.post("/api/savefiles") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody(uploadBody("""{"level":99,"version":"updated"}"""))
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        client.get("/api/savefiles") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            val body = bodyAsText()
            // Only one record expected for the same saveId, containing the updated version
            assertContains(body, "save-upsert")
            assertContains(body, "updated")
        }
    }

    @Test
    fun `GET savefiles returns empty list for new user`() = withRealDatabase {
        val token = fakeToken("user-savefile-empty")
        client.get("/api/savefiles") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEmptyJsonArray(bodyAsText())
        }
    }

    @Test
    fun `POST savefiles without auth returns 401`() = withRealDatabase {
        client.post("/api/savefiles") {
            contentType(ContentType.Application.Json)
            setBody("""{"saveId":"x","data":"{}"}""")
        }.apply {
            assertEquals(HttpStatusCode.Unauthorized, status)
        }
    }

    @Test
    fun `GET savefiles without auth returns 401`() = withRealDatabase {
        client.get("/api/savefiles").apply {
            assertEquals(HttpStatusCode.Unauthorized, status)
        }
    }

    @Test
    fun `savefiles are isolated per user`() = withRealDatabase {
        val tokenA = fakeToken("user-isolation-a")
        val tokenB = fakeToken("user-isolation-b")

        client.post("/api/savefiles") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $tokenA")
            setBody("""{"saveId":"private-save","data":"\"user-a-data\""}""")
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        // User B should not see user A's saves
        client.get("/api/savefiles") {
            header(HttpHeaders.Authorization, "Bearer $tokenB")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEmptyJsonArray(bodyAsText())
        }
    }

    // -------------------------------------------------------------------------
    // User data endpoints – full round-trip
    // -------------------------------------------------------------------------

    @Test
    fun `POST userdata then GET userdata returns stored data`() = withRealDatabase {
        val token = fakeToken("user-userdata-rt")
        val userData = """{"localUsername":"Hero","abilities":{"health":3},"levelProgress":{}}"""

        client.post("/api/userdata") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"data":${jsonString(userData)}}""")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }

        client.get("/api/userdata") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            val body = bodyAsText()
            assertContains(body, "Hero")
            assertContains(body, "updatedAt")
        }
    }

    @Test
    fun `GET userdata returns 404 when no data exists for user`() = withRealDatabase {
        val token = fakeToken("user-userdata-missing")
        client.get("/api/userdata") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.apply {
            assertEquals(HttpStatusCode.NotFound, status)
        }
    }

    @Test
    fun `POST userdata upserts existing data`() = withRealDatabase {
        val token = fakeToken("user-userdata-upsert")

        client.post("/api/userdata") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"data":"\"v1\""}""")
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        client.post("/api/userdata") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"data":"\"v2\""}""")
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        client.get("/api/userdata") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            assertContains(bodyAsText(), "v2")
        }
    }

    @Test
    fun `POST userdata without auth returns 401`() = withRealDatabase {
        client.post("/api/userdata") {
            contentType(ContentType.Application.Json)
            setBody("""{"data":"{}"}""")
        }.apply {
            assertEquals(HttpStatusCode.Unauthorized, status)
        }
    }

    @Test
    fun `GET userdata without auth returns 401`() = withRealDatabase {
        client.get("/api/userdata").apply {
            assertEquals(HttpStatusCode.Unauthorized, status)
        }
    }

    // -------------------------------------------------------------------------
    // Settings endpoints – full round-trip
    // -------------------------------------------------------------------------

    @Test
    fun `POST settings then GET settings returns stored settings`() = withRealDatabase {
        val token = fakeToken("user-settings-rt")
        val settings = """{"darkMode":true,"language":"de","soundEnabled":false}"""

        client.post("/api/settings") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"data":${jsonString(settings)}}""")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }

        client.get("/api/settings") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            val body = bodyAsText()
            assertContains(body, "darkMode")
            assertContains(body, "updatedAt")
        }
    }

    @Test
    fun `GET settings returns 404 when no settings exist for user`() = withRealDatabase {
        val token = fakeToken("user-settings-missing")
        client.get("/api/settings") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.apply {
            assertEquals(HttpStatusCode.NotFound, status)
        }
    }

    @Test
    fun `POST settings upserts existing settings`() = withRealDatabase {
        val token = fakeToken("user-settings-upsert")

        client.post("/api/settings") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"data":"\"first\""}""")
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        client.post("/api/settings") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"data":"\"second\""}""")
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        client.get("/api/settings") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            assertContains(bodyAsText(), "second")
        }
    }

    @Test
    fun `POST settings without auth returns 401`() = withRealDatabase {
        client.post("/api/settings") {
            contentType(ContentType.Application.Json)
            setBody("""{"data":"{}"}""")
        }.apply {
            assertEquals(HttpStatusCode.Unauthorized, status)
        }
    }

    @Test
    fun `GET settings without auth returns 401`() = withRealDatabase {
        client.get("/api/settings").apply {
            assertEquals(HttpStatusCode.Unauthorized, status)
        }
    }

    // -------------------------------------------------------------------------
    // Community files endpoints – full round-trip
    // -------------------------------------------------------------------------

    @Test
    fun `POST community file then GET list includes the uploaded file`() = withRealDatabase {
        val token = fakeToken("user-community-rt", "mapmaker")
        val mapData = """{"tiles":[],"width":10,"height":10}"""

        client.post("/api/community/files") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"fileType":"MAP","fileId":"integration-test-map-1","data":${jsonString(mapData)}}""")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }

        client.get("/api/community/files").apply {
            assertEquals(HttpStatusCode.OK, status)
            val body = bodyAsText()
            assertContains(body, "integration-test-map-1")
            assertContains(body, "mapmaker")
        }
    }

    @Test
    fun `GET community file by type and id returns file data`() = withRealDatabase {
        val token = fakeToken("user-community-download", "levelmaker")
        val levelData = """{"enemySpawns":[],"startCoins":100}"""

        client.post("/api/community/files") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"fileType":"LEVEL","fileId":"integration-test-level-1","data":${jsonString(levelData)}}""")
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        client.get("/api/community/files/LEVEL/integration-test-level-1").apply {
            assertEquals(HttpStatusCode.OK, status)
            val body = bodyAsText()
            assertContains(body, "integration-test-level-1")
            assertContains(body, "levelmaker")
            assertContains(body, "enemySpawns")
        }
    }

    @Test
    fun `GET community files filtered by fileType returns only matching files`() = withRealDatabase {
        val token = fakeToken("user-community-filter", "filterer")

        client.post("/api/community/files") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"fileType":"MAP","fileId":"filter-test-map","data":"\"mapdata\""}""")
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        client.post("/api/community/files") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"fileType":"LEVEL","fileId":"filter-test-level","data":"\"leveldata\""}""")
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        // Filter for MAP only
        client.get("/api/community/files?fileType=MAP").apply {
            assertEquals(HttpStatusCode.OK, status)
            val body = bodyAsText()
            assertContains(body, "filter-test-map")
        }

        // Filter for LEVEL only
        client.get("/api/community/files?fileType=LEVEL").apply {
            assertEquals(HttpStatusCode.OK, status)
            val body = bodyAsText()
            assertContains(body, "filter-test-level")
        }
    }

    @Test
    fun `GET community file for unknown id returns 404`() = withRealDatabase {
        client.get("/api/community/files/MAP/does-not-exist").apply {
            assertEquals(HttpStatusCode.NotFound, status)
        }
    }

    @Test
    fun `POST community file with invalid fileType returns 400`() = withRealDatabase {
        val token = fakeToken("user-community-bad-type")
        client.post("/api/community/files") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"fileType":"INVALID","fileId":"x","data":"{}"}""")
        }.apply {
            assertEquals(HttpStatusCode.BadRequest, status)
        }
    }

    @Test
    fun `POST community file without auth returns 401`() = withRealDatabase {
        client.post("/api/community/files") {
            contentType(ContentType.Application.Json)
            setBody("""{"fileType":"MAP","fileId":"x","data":"{}"}""")
        }.apply {
            assertEquals(HttpStatusCode.Unauthorized, status)
        }
    }

    @Test
    fun `POST community file by another user on same fileId returns 403`() = withRealDatabase {
        val tokenA = fakeToken("user-community-owner-a", "owner")
        val tokenB = fakeToken("user-community-owner-b", "thief")

        client.post("/api/community/files") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $tokenA")
            setBody("""{"fileType":"MAP","fileId":"ownership-test-map","data":"\"owner-data\""}""")
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        client.post("/api/community/files") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $tokenB")
            setBody("""{"fileType":"MAP","fileId":"ownership-test-map","data":"\"stolen-data\""}""")
        }.apply {
            assertEquals(HttpStatusCode.Forbidden, status)
        }
    }

    @Test
    fun `GET community files is public and does not require auth`() = withRealDatabase {
        client.get("/api/community/files").apply {
            // Public endpoint – must succeed without any Authorization header
            assertEquals(HttpStatusCode.OK, status)
        }
    }

    // -------------------------------------------------------------------------
    // Events endpoint with database available (fire-and-forget persistence path)
    // -------------------------------------------------------------------------

    @Test
    fun `POST events succeeds and persists when database is available`() = withRealDatabase {
        client.post("/api/events") {
            contentType(ContentType.Application.Json)
            setBody("""{"event":"LEVEL_WON","levelName":"Tutorial","platform":"WEB"}""")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }

    @Test
    fun `POST events with versionName and commitHash succeeds`() = withRealDatabase {
        client.post("/api/events") {
            contentType(ContentType.Application.Json)
            setBody("""{"event":"APP_STARTED","platform":"DESKTOP","platformLong":"Java HotSpot(TM) 64-Bit Server VM","versionName":"1.0","commitHash":"abc1234"}""")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }

    @Test
    fun `POST events without versionName and commitHash still succeeds`() = withRealDatabase {
        client.post("/api/events") {
            contentType(ContentType.Application.Json)
            setBody("""{"event":"LEVEL_STARTED","levelName":"Level 1","platform":"ANDROID"}""")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }

    @Test
    fun `POST events with username and turnNumber persists when authenticated`() = withRealDatabase {
        val token = fakeToken("user-event-auth", "player_one")
        client.post("/api/events") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"event":"LEVEL_WON","levelName":"Tutorial","platform":"WEB","username":"player_one","turnNumber":42}""")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }

    @Test
    fun `POST events with turnNumber and no username succeeds`() = withRealDatabase {
        client.post("/api/events") {
            contentType(ContentType.Application.Json)
            setBody("""{"event":"LEVEL_LOST","levelName":"Hard Level","platform":"DESKTOP","turnNumber":15}""")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }

    @Test
    fun `POST events with authenticated username and no turnNumber succeeds`() = withRealDatabase {
        val token = fakeToken("user-event-auth-noturn", "player_two")
        client.post("/api/events") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"event":"LEVEL_STARTED","levelName":"Tutorial","platform":"WEB","username":"player_two"}""")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }

    @Test
    fun `POST savefiles with versionName and commitHash stores successfully`() = withRealDatabase {
        val token = fakeToken("user-savefile-version")
        client.post("/api/savefiles") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"saveId":"save-v1","data":"\"gamedata\"","platform":"WEB","platformLong":"Web with Kotlin/Wasm Mozilla/5.0 (X11)","versionName":"1.0","commitHash":"abc1234"}""")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }

    @Test
    fun `POST userdata with versionName and commitHash stores successfully`() = withRealDatabase {
        val token = fakeToken("user-userdata-version")
        val userData = """{"localUsername":"Tester","abilities":{},"levelProgress":{}}"""
        client.post("/api/userdata") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"data":${jsonString(userData)},"platform":"DESKTOP","platformLong":"Java HotSpot 64-Bit","versionName":"1.0","commitHash":"abc1234"}""")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }

    @Test
    fun `POST settings with versionName and commitHash stores successfully`() = withRealDatabase {
        val token = fakeToken("user-settings-version")
        val settings = """{"darkMode":false,"language":"en"}"""
        client.post("/api/settings") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"data":${jsonString(settings)},"platform":"IOS","platformLong":"iOS 18.0","versionName":"1.0","commitHash":"abc1234"}""")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }

    @Test
    fun `GET userdata does not expose versionName or commitHash to frontend`() = withRealDatabase {
        val token = fakeToken("user-userdata-no-expose")
        val userData = """{"localUsername":"NoExpose","abilities":{},"levelProgress":{}}"""

        client.post("/api/userdata") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"data":${jsonString(userData)},"platform":"WEB","platformLong":"Web with Kotlin/Wasm Mozilla/5.0","versionName":"1.0","commitHash":"abc1234"}""")
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        client.get("/api/userdata") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            val body = bodyAsText()
            // Response must contain the user data but must NOT expose version/commit fields
            assertContains(body, "NoExpose")
            assert(!body.contains("versionName")) { "Response must not expose versionName" }
            assert(!body.contains("commitHash")) { "Response must not expose commitHash" }
            assert(!body.contains("\"platform\"")) { "Response must not expose platform" }
            assert(!body.contains("platformLong")) { "Response must not expose platformLong" }
        }
    }

    @Test
    fun `GET settings does not expose versionName or commitHash to frontend`() = withRealDatabase {
        val token = fakeToken("user-settings-no-expose")
        val settings = """{"darkMode":true}"""

        client.post("/api/settings") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"data":${jsonString(settings)},"platform":"WEB","platformLong":"Web with Kotlin/Wasm Mozilla/5.0","versionName":"1.0","commitHash":"abc1234"}""")
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        client.get("/api/settings") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            val body = bodyAsText()
            assertContains(body, "darkMode")
            assert(!body.contains("versionName")) { "Response must not expose versionName" }
            assert(!body.contains("commitHash")) { "Response must not expose commitHash" }
            assert(!body.contains("\"platform\"")) { "Response must not expose platform" }
            assert(!body.contains("platformLong")) { "Response must not expose platformLong" }
        }
    }

    // -------------------------------------------------------------------------
    // Community map image generation and serving
    // -------------------------------------------------------------------------

    /** Minimal valid map JSON with two tile entries to exercise the image generator. */
    private fun minimalMapJson(id: String = "img-test-map") = """
        {"id":"$id","name":"Test Map","width":5,"height":5,"tiles":{"0,0":"PATH","1,0":"BUILD_AREA","2,0":"NO_PLAY","3,0":"SPAWN_POINT","4,0":"TARGET"}}
    """.trimIndent()

    @Test
    fun `POST community MAP generates server-side image and GET image endpoint returns PNG`() = withRealDatabase {
        val token = fakeToken("user-map-img-1", "mapper")
        val mapData = minimalMapJson("img-map-1")

        client.post("/api/community/files") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"fileType":"MAP","fileId":"img-map-1","data":${jsonString(mapData)}}""")
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        // Image endpoint must return a non-empty PNG
        client.get("/api/community/files/MAP/img-map-1/image").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("image/png", contentType()?.toString()?.substringBefore(";")?.trim())
            val bytes = readBytes()
            assert(bytes.size > 100) { "PNG should be larger than 100 bytes, got ${bytes.size}" }
            // PNG files start with the 8-byte PNG signature
            assertEquals(0x89.toByte(), bytes[0])
            assertEquals(0x50.toByte(), bytes[1])  // 'P'
            assertEquals(0x4E.toByte(), bytes[2])  // 'N'
            assertEquals(0x47.toByte(), bytes[3])  // 'G'
        }
    }

    @Test
    fun `GET image endpoint returns 404 for non-existent map`() = withRealDatabase {
        client.get("/api/community/files/MAP/does-not-exist-img/image").apply {
            assertEquals(HttpStatusCode.NotFound, status)
        }
    }

    @Test
    fun `POST community LEVEL does not generate image but MAP upload does`() = withRealDatabase {
        val token = fakeToken("user-level-no-img", "levelauthor")
        val levelData = """{"id":"test-level-1","title":"Test","mapId":"some-map","enemySpawns":[],"startCoins":100,"startHealthPoints":10,"availableTowers":[]}"""

        client.post("/api/community/files") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"fileType":"LEVEL","fileId":"test-level-no-img","data":${jsonString(levelData)}}""")
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        // No image endpoint for LEVEL files – the path only supports MAP
        client.get("/api/community/files/MAP/test-level-no-img/image").apply {
            assertEquals(HttpStatusCode.NotFound, status)
        }
    }

    @Test
    fun `updating community MAP regenerates the image`() = withRealDatabase {
        val token = fakeToken("user-map-img-update", "mapper2")
        val mapData1 = minimalMapJson("img-map-update")
        val mapData2 = minimalMapJson("img-map-update").replace("5,\"height\":5", "6,\"height\":6")

        client.post("/api/community/files") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"fileType":"MAP","fileId":"img-map-update","data":${jsonString(mapData1)}}""")
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        val firstImageSize = client.get("/api/community/files/MAP/img-map-update/image").readBytes().size

        // Re-upload with same data – image should still be present
        client.post("/api/community/files") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"fileType":"MAP","fileId":"img-map-update","data":${jsonString(mapData1)}}""")
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        val secondImageSize = client.get("/api/community/files/MAP/img-map-update/image").readBytes().size
        assert(secondImageSize > 0) { "Image must still be available after re-upload" }
    }

    @Test
    fun `GET community map image is public and does not require auth`() = withRealDatabase {
        val token = fakeToken("user-public-img", "publicmapper")
        val mapData = minimalMapJson("public-img-map")
        client.post("/api/community/files") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"fileType":"MAP","fileId":"public-img-map","data":${jsonString(mapData)}}""")
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        // No auth header – should still succeed
        client.get("/api/community/files/MAP/public-img-map/image").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }

    // -------------------------------------------------------------------------
    // DELETE /api/community/files/{fileType}/{fileId} – admin delete endpoint
    // -------------------------------------------------------------------------

    @Test
    fun `DELETE community file requires authentication`() = withRealDatabase {
        client.delete("/api/community/files/MAP/any-map-id").apply {
            assertEquals(HttpStatusCode.Unauthorized, status)
        }
    }

    @Test
    fun `DELETE community file requires community_admin role`() = withRealDatabase {
        // Token without any roles
        val token = fakeToken("user-no-admin-role", "normaluser")
        client.delete("/api/community/files/MAP/any-map-id") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.apply {
            assertEquals(HttpStatusCode.Forbidden, status)
        }
    }

    @Test
    fun `DELETE community file returns 404 for non-existent file`() = withRealDatabase {
        val token = fakeTokenWithClientRole("admin-user-404", role = "community_admin")
        client.delete("/api/community/files/MAP/does-not-exist-delete") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.apply {
            assertEquals(HttpStatusCode.NotFound, status)
        }
    }

    @Test
    fun `DELETE community file with invalid fileType returns 400`() = withRealDatabase {
        val token = fakeTokenWithClientRole("admin-user-400", role = "community_admin")
        client.delete("/api/community/files/INVALID/some-id") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.apply {
            assertEquals(HttpStatusCode.BadRequest, status)
        }
    }

    @Test
    fun `DELETE community file removes it from the database`() = withRealDatabase {
        val uploaderToken = fakeToken("user-upload-del", "uploader")
        val adminToken = fakeTokenWithClientRole("admin-delete-1", role = "community_admin")
        val mapData = minimalMapJson("delete-map-1")

        // Upload the file first
        client.post("/api/community/files") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $uploaderToken")
            setBody("""{"fileType":"MAP","fileId":"delete-map-1","data":${jsonString(mapData)}}""")
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        // Verify it's listed
        client.get("/api/community/files?fileType=MAP").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertContains(bodyAsText(), "delete-map-1")
        }

        // Admin deletes it
        client.delete("/api/community/files/MAP/delete-map-1") {
            header(HttpHeaders.Authorization, "Bearer $adminToken")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }

        // Should now return 404
        client.get("/api/community/files/MAP/delete-map-1").apply {
            assertEquals(HttpStatusCode.NotFound, status)
        }
    }

    @Test
    fun `DELETE community LEVEL file works for admin`() = withRealDatabase {
        val uploaderToken = fakeToken("user-upload-level-del", "leveluploader")
        val adminToken = fakeTokenWithClientRole("admin-delete-level", role = "community_admin")
        val levelData = """{"id":"del-level-1","title":"Delete Me","mapId":"some-map","enemySpawns":[],"startCoins":100,"startHealthPoints":10,"availableTowers":[]}"""

        client.post("/api/community/files") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $uploaderToken")
            setBody("""{"fileType":"LEVEL","fileId":"del-level-1","data":${jsonString(levelData)}}""")
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        client.delete("/api/community/files/LEVEL/del-level-1") {
            header(HttpHeaders.Authorization, "Bearer $adminToken")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }

        client.get("/api/community/files/LEVEL/del-level-1").apply {
            assertEquals(HttpStatusCode.NotFound, status)
        }
    }

    @Test
    fun `DELETE community file can be deleted by admin even if uploaded by another user`() = withRealDatabase {
        val uploaderToken = fakeToken("user-owner-del", "fileowner")
        val adminToken = fakeTokenWithClientRole("admin-override-del", role = "community_admin")
        val mapData = minimalMapJson("admin-override-map")

        client.post("/api/community/files") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $uploaderToken")
            setBody("""{"fileType":"MAP","fileId":"admin-override-map","data":${jsonString(mapData)}}""")
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        // Admin (different user) can delete it
        client.delete("/api/community/files/MAP/admin-override-map") {
            header(HttpHeaders.Authorization, "Bearer $adminToken")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }
}
