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
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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
import kotlin.test.assertFalse

/**
 * Cross-platform continuation tests that verify a player can leave the game on one platform
 * and continue on another platform with all values preserved identically.
 *
 * These tests cover the key cross-platform scenarios:
 * - Save game state (level progress, coins, HP) on Platform A, load it on Platform B
 * - User data (XP, abilities, spell unlocks) persisted and available on any platform
 * - Settings (language, theme) synced across platforms
 * - Multiple saves from different platforms coexist and are isolated per user
 * - Platform-specific metadata is stored but not exposed to other platforms
 *
 * A real PostgreSQL database is spun up via Testcontainers, and the fake JWT tokens
 * follow the same format as real Keycloak tokens (base64url-encoded payload with "sub"
 * and "preferred_username" claims), so the backend's identity extraction works identically.
 */
class CrossPlatformContinuationTest {

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
         * Creates a fake JWT for a player on a given platform.
         * The token mimics a real Keycloak JWT: base64url-encoded JSON payload with
         * "sub" (unique user ID) and "preferred_username" claims. The same userId
         * on different platforms maps to the same backend user, which is the
         * key invariant for cross-platform continuation.
         */
        private fun fakeToken(userId: String, username: String = userId): String {
            val header = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("""{"alg":"none"}""".toByteArray())
            val payload = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("""{"sub":"$userId","preferred_username":"$username"}""".toByteArray())
            return "$header.$payload."
        }

        private fun jsonString(value: String): String = Json.encodeToString(JsonPrimitive(value))

        private fun assertEmptyJsonArray(body: String) {
            assertEquals(0, Json.parseToJsonElement(body).jsonArray.size, "Expected an empty JSON array")
        }
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
    // Cross-platform save file continuation
    // -------------------------------------------------------------------------

    @Test
    fun `player saves game on Web and can continue on Android with same save data`() = withRealDatabase {
        val userId = "cp-player-web-to-android"
        val token = fakeToken(userId, "CrossPlatformPlayer")
        val saveData = """{"level":7,"coins":2500,"health":8,"worldMapProgress":{"unlocked":["level-1","level-2","level-3"]}}"""

        // Save game on Web platform
        client.post("/api/savefiles") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"saveId":"world-save","data":${jsonString(saveData)},"platform":"WEB","versionName":"1.0"}""")
        }.apply {
            assertEquals(HttpStatusCode.OK, status, "Web save should succeed")
        }

        // Load game on Android platform – same user, same data
        client.get("/api/savefiles") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.apply {
            assertEquals(HttpStatusCode.OK, status, "Android load should succeed")
            val body = bodyAsText()
            assertContains(body, "world-save")
            assertContains(body, "level-3")
            assertContains(body, "2500")
        }
    }

    @Test
    fun `player saves game on Desktop and can continue on iOS with same save data`() = withRealDatabase {
        val userId = "cp-player-desktop-to-ios"
        val token = fakeToken(userId, "DesktopToIosPlayer")
        val saveData = """{"level":12,"coins":9800,"health":5,"abilities":{"fireball":true,"icewall":false}}"""

        // Save on Desktop
        client.post("/api/savefiles") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"saveId":"main-save","data":${jsonString(saveData)},"platform":"DESKTOP"}""")
        }.apply {
            assertEquals(HttpStatusCode.OK, status, "Desktop save should succeed")
        }

        // Continue on iOS – same user identity (same sub claim)
        client.get("/api/savefiles") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.apply {
            assertEquals(HttpStatusCode.OK, status, "iOS load should succeed")
            val body = bodyAsText()
            assertContains(body, "fireball")
            assertContains(body, "9800")
        }
    }

    @Test
    fun `player saves game on iOS and can continue on Web with same save data`() = withRealDatabase {
        val userId = "cp-player-ios-to-web"
        val token = fakeToken(userId, "IosToWebPlayer")
        val saveData = """{"level":3,"coins":500,"health":10,"spells":["lightning","shield"]}"""

        // Save on iOS
        client.post("/api/savefiles") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"saveId":"campaign-save","data":${jsonString(saveData)},"platform":"IOS"}""")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }

        // Continue on Web
        client.get("/api/savefiles") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            val body = bodyAsText()
            assertContains(body, "campaign-save")
            assertContains(body, "lightning")
            assertContains(body, "shield")
        }
    }

    @Test
    fun `player can update save on one platform and the update is visible on another`() = withRealDatabase {
        val userId = "cp-player-update"
        val token = fakeToken(userId, "UpdatingPlayer")

        // Initial save from Android
        client.post("/api/savefiles") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"saveId":"progress-save","data":${jsonString("""{"level":1,"coins":100}""")},"platform":"ANDROID"}""")
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        // Update the save from Desktop (continued playing)
        client.post("/api/savefiles") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"saveId":"progress-save","data":${jsonString("""{"level":5,"coins":3000}""")},"platform":"DESKTOP"}""")
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        // Load from iOS – must see the updated (most recent) data
        client.get("/api/savefiles") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            val body = bodyAsText()
            // Only one record for the same saveId (upsert semantics)
            val saves = Json.parseToJsonElement(body).jsonArray
            assertEquals(1, saves.size, "Upsert must produce exactly one save record")
            // The data field contains the latest (updated) save – must contain 3000 coins, not 100
            val dataField = saves[0].jsonObject["data"]!!.jsonPrimitive.content
            assertContains(dataField, "3000")
            assertFalse(dataField.contains("\"coins\":100"), "Stale initial coin count must not appear in upserted save")
        }
    }

    @Test
    fun `multiple named saves from different platforms all persist and are accessible`() = withRealDatabase {
        val userId = "cp-player-multi-save"
        val token = fakeToken(userId, "MultiSavePlayer")

        val savesFromPlatforms = listOf(
            Triple("slot-1", "WEB", """{"level":1,"slotName":"Quick Save 1"}"""),
            Triple("slot-2", "ANDROID", """{"level":4,"slotName":"Campaign Save"}"""),
            Triple("slot-3", "DESKTOP", """{"level":9,"slotName":"Hard Mode Run"}""")
        )

        for ((saveId, platform, data) in savesFromPlatforms) {
            client.post("/api/savefiles") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody("""{"saveId":"$saveId","data":${jsonString(data)},"platform":"$platform"}""")
            }.apply { assertEquals(HttpStatusCode.OK, status, "Save $saveId from $platform should succeed") }
        }

        // All saves are visible regardless of which platform loads them
        client.get("/api/savefiles") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            val body = bodyAsText()
            assertContains(body, "slot-1")
            assertContains(body, "slot-2")
            assertContains(body, "slot-3")
            assertContains(body, "Quick Save 1")
            assertContains(body, "Campaign Save")
            assertContains(body, "Hard Mode Run")
        }
    }

    @Test
    fun `save files from different players are never visible to each other`() = withRealDatabase {
        val tokenPlayerA = fakeToken("cp-isolation-player-a", "PlayerA")
        val tokenPlayerB = fakeToken("cp-isolation-player-b", "PlayerB")

        client.post("/api/savefiles") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $tokenPlayerA")
            setBody("""{"saveId":"shared-slot-name","data":${jsonString("""{"secret":"player-a-data"}""")},"platform":"WEB"}""")
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        client.post("/api/savefiles") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $tokenPlayerB")
            setBody("""{"saveId":"shared-slot-name","data":${jsonString("""{"secret":"player-b-data"}""")},"platform":"ANDROID"}""")
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        // Player A only sees their own data
        client.get("/api/savefiles") {
            header(HttpHeaders.Authorization, "Bearer $tokenPlayerA")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            val body = bodyAsText()
            assertContains(body, "player-a-data")
            assertFalse(body.contains("player-b-data"), "Player A must not see Player B's data")
        }

        // Player B only sees their own data
        client.get("/api/savefiles") {
            header(HttpHeaders.Authorization, "Bearer $tokenPlayerB")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            val body = bodyAsText()
            assertContains(body, "player-b-data")
            assertFalse(body.contains("player-a-data"), "Player B must not see Player A's data")
        }
    }

    // -------------------------------------------------------------------------
    // Cross-platform user data (XP, abilities, level progress) continuation
    // -------------------------------------------------------------------------

    @Test
    fun `player XP and abilities are preserved when switching from Desktop to Android`() = withRealDatabase {
        val userId = "cp-userdata-desktop-to-android"
        val token = fakeToken(userId, "XpPlayer")
        val userData = """{"localUsername":"XpPlayer","xp":4200,"abilities":{"fireball":3,"shield":2,"icewall":1},"levelProgress":{"level-1":"GOLD","level-2":"SILVER","level-3":"BRONZE"}}"""

        // Upload from Desktop
        client.post("/api/userdata") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"data":${jsonString(userData)},"platform":"DESKTOP"}""")
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        // Load from Android
        client.get("/api/userdata") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            val body = bodyAsText()
            assertContains(body, "XpPlayer")
            assertContains(body, "4200")
            assertContains(body, "fireball")
            assertContains(body, "levelProgress")
            assertContains(body, "GOLD")
        }
    }

    @Test
    fun `level unlock progress is preserved across all platforms`() = withRealDatabase {
        val userId = "cp-levels-unlocked"
        val token = fakeToken(userId, "LevelUnlockPlayer")
        val userData = """{"localUsername":"LevelUnlockPlayer","xp":0,"abilities":{},"levelProgress":{"welcome_to_defender_of_egril":"COMPLETED","the_first_wave":"COMPLETED","mixed_forces":"UNLOCKED"}}"""

        client.post("/api/userdata") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"data":${jsonString(userData)},"platform":"IOS"}""")
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        client.get("/api/userdata") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            val body = bodyAsText()
            assertContains(body, "welcome_to_defender_of_egril")
            assertContains(body, "the_first_wave")
            assertContains(body, "mixed_forces")
            assertContains(body, "COMPLETED")
            assertContains(body, "UNLOCKED")
        }
    }

    @Test
    fun `spell and ability unlocks are updated and visible on subsequent platforms`() = withRealDatabase {
        val userId = "cp-spell-unlock"
        val token = fakeToken(userId, "SpellUnlockPlayer")

        // Start on Web: no spells unlocked
        client.post("/api/userdata") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"data":${jsonString("""{"localUsername":"SpellUnlockPlayer","xp":100,"abilities":{},"spells":[]}""")},"platform":"WEB"}""")
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        // Play on Android: unlock fireball and lightning
        client.post("/api/userdata") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"data":${jsonString("""{"localUsername":"SpellUnlockPlayer","xp":800,"abilities":{"fireball":1},"spells":["fireball","lightning"]}""")},"platform":"ANDROID"}""")
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        // Continue on Desktop – must see spells unlocked on Android
        client.get("/api/userdata") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            val body = bodyAsText()
            assertContains(body, "fireball")
            assertContains(body, "lightning")
            assertContains(body, "800")
        }
    }

    @Test
    fun `user data from different players is always isolated`() = withRealDatabase {
        val tokenA = fakeToken("cp-userdata-iso-a", "PlayerAlpha")
        val tokenB = fakeToken("cp-userdata-iso-b", "PlayerBeta")

        client.post("/api/userdata") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $tokenA")
            setBody("""{"data":${jsonString("""{"localUsername":"PlayerAlpha","xp":9999}""")},"platform":"WEB"}""")
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        client.post("/api/userdata") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $tokenB")
            setBody("""{"data":${jsonString("""{"localUsername":"PlayerBeta","xp":1}""")},"platform":"ANDROID"}""")
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        client.get("/api/userdata") {
            header(HttpHeaders.Authorization, "Bearer $tokenA")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            val body = bodyAsText()
            assertContains(body, "PlayerAlpha")
            assertFalse(body.contains("PlayerBeta"), "Player A must not see Player B's username")
        }

        client.get("/api/userdata") {
            header(HttpHeaders.Authorization, "Bearer $tokenB")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            val body = bodyAsText()
            assertContains(body, "PlayerBeta")
            assertFalse(body.contains("PlayerAlpha"), "Player B must not see Player A's username")
        }
    }

    // -------------------------------------------------------------------------
    // Cross-platform settings continuation
    // -------------------------------------------------------------------------

    @Test
    fun `player settings are synced across all platforms`() = withRealDatabase {
        val userId = "cp-settings-sync"
        val token = fakeToken(userId, "SettingsPlayer")
        val settings = """{"darkMode":true,"language":"de","soundEnabled":false,"difficulty":"HARD"}"""

        // Save settings on Android
        client.post("/api/settings") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"data":${jsonString(settings)},"platform":"ANDROID"}""")
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        // Load settings on Desktop – must be identical
        client.get("/api/settings") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            val body = bodyAsText()
            assertContains(body, "darkMode")
            assertContains(body, "de")
            assertContains(body, "HARD")
        }
    }

    @Test
    fun `settings updated on one platform override previous settings from another platform`() = withRealDatabase {
        val userId = "cp-settings-update"
        val token = fakeToken(userId, "SettingsUpdatePlayer")

        // Initial settings from Web
        client.post("/api/settings") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"data":${jsonString("""{"language":"en","darkMode":false}""")},"platform":"WEB"}""")
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        // Player changes language on Android
        client.post("/api/settings") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"data":${jsonString("""{"language":"fr","darkMode":false}""")},"platform":"ANDROID"}""")
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        // Load settings on Desktop – must reflect Android change
        client.get("/api/settings") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            val body = bodyAsText()
            assertContains(body, "fr")
            assertFalse(body.contains("\"language\":\"en\""), "Old language setting must be overwritten")
        }
    }

    @Test
    fun `settings from different players are isolated across platforms`() = withRealDatabase {
        val tokenA = fakeToken("cp-settings-iso-a", "SettingsPlayerA")
        val tokenB = fakeToken("cp-settings-iso-b", "SettingsPlayerB")

        client.post("/api/settings") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $tokenA")
            setBody("""{"data":${jsonString("""{"language":"de","darkMode":true}""")},"platform":"WEB"}""")
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        client.post("/api/settings") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $tokenB")
            setBody("""{"data":${jsonString("""{"language":"it","darkMode":false}""")},"platform":"ANDROID"}""")
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        client.get("/api/settings") {
            header(HttpHeaders.Authorization, "Bearer $tokenA")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            val body = bodyAsText()
            assertContains(body, "de")
            assertFalse(body.contains("\"language\":\"it\""), "Player A must not see Player B's language setting")
        }

        client.get("/api/settings") {
            header(HttpHeaders.Authorization, "Bearer $tokenB")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            val body = bodyAsText()
            assertContains(body, "it")
            assertFalse(body.contains("\"language\":\"de\""), "Player B must not see Player A's language setting")
        }
    }

    // -------------------------------------------------------------------------
    // Full cross-platform play session: combined save + userdata + settings
    // -------------------------------------------------------------------------

    @Test
    fun `complete cross-platform session - player switches between three platforms`() = withRealDatabase {
        val userId = "cp-full-session"
        val token = fakeToken(userId, "FullSessionPlayer")

        // Session 1: Play on Web, complete level 1
        client.post("/api/userdata") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"data":${jsonString("""{"localUsername":"FullSessionPlayer","xp":200,"abilities":{},"levelProgress":{"level-1":"COMPLETED"}}""")},"platform":"WEB"}""")
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        client.post("/api/savefiles") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"saveId":"main","data":${jsonString("""{"currentLevel":2,"coins":400,"health":9}""")},"platform":"WEB"}""")
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        client.post("/api/settings") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"data":${jsonString("""{"language":"es","darkMode":true}""")},"platform":"WEB"}""")
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        // Session 2: Continue on Android, complete level 2
        client.post("/api/userdata") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"data":${jsonString("""{"localUsername":"FullSessionPlayer","xp":500,"abilities":{"fireball":1},"levelProgress":{"level-1":"COMPLETED","level-2":"COMPLETED"}}""")},"platform":"ANDROID"}""")
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        client.post("/api/savefiles") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"saveId":"main","data":${jsonString("""{"currentLevel":3,"coins":800,"health":7}""")},"platform":"ANDROID"}""")
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        // Session 3: Continue on Desktop – verify all state from previous platforms is present
        client.get("/api/userdata") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            val body = bodyAsText()
            assertContains(body, "500")
            assertContains(body, "fireball")
            assertContains(body, "level-2")
        }

        client.get("/api/savefiles") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            val body = bodyAsText()
            assertContains(body, "800")
            assertContains(body, "currentLevel")
        }

        client.get("/api/settings") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            val body = bodyAsText()
            assertContains(body, "es")
        }
    }

    // -------------------------------------------------------------------------
    // Platform metadata is stored but not leaked to other platforms
    // -------------------------------------------------------------------------

    @Test
    fun `platform metadata is stored server-side but not exposed in save file responses`() = withRealDatabase {
        val token = fakeToken("cp-meta-save", "MetaPlayer")
        client.post("/api/savefiles") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"saveId":"meta-save","data":"\"gamedata\"","platform":"WEB","platformLong":"Web with Kotlin/Wasm Mozilla/5.0","versionName":"1.2","commitHash":"abc0001"}""")
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        client.get("/api/savefiles") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            val body = bodyAsText()
            assertContains(body, "meta-save")
            assertFalse(body.contains("platformLong"), "Platform long description must not be exposed")
            assertFalse(body.contains("versionName"), "Version name must not be exposed")
            assertFalse(body.contains("commitHash"), "Commit hash must not be exposed")
        }
    }

    @Test
    fun `platform metadata is stored server-side but not exposed in user data responses`() = withRealDatabase {
        val token = fakeToken("cp-meta-userdata", "MetaUserPlayer")
        client.post("/api/userdata") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"data":${jsonString("""{"localUsername":"MetaUserPlayer","xp":0}""")},"platform":"IOS","platformLong":"iOS 18.0 (iPhone)","versionName":"2.0","commitHash":"def0002"}""")
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        client.get("/api/userdata") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            val body = bodyAsText()
            assertContains(body, "MetaUserPlayer")
            assertFalse(body.contains("platformLong"), "Platform long description must not be exposed")
            assertFalse(body.contains("versionName"), "Version name must not be exposed")
            assertFalse(body.contains("commitHash"), "Commit hash must not be exposed")
        }
    }
}
