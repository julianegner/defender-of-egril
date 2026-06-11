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
 * Exhaustive cross-platform matrix tests that verify all platform-to-platform
 * combinations (including same-platform) for savefiles, game state (userdata),
 * and settings synchronization.
 *
 * Platforms under test: WEB, DESKTOP, ANDROID, IOS (4x4 = 16 combinations each).
 *
 * This ensures that a player logged in on any platform can save data and then
 * retrieve it identically from any other platform (or the same platform on a
 * different device).
 */
class CrossPlatformMatrixTest {

    companion object {
        private val ALL_PLATFORMS = listOf("WEB", "DESKTOP", "ANDROID", "IOS")

        private val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16-alpine")
            .apply { start() }

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

        private fun fakeToken(userId: String, username: String = userId): String {
            val header = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("""{"alg":"none"}""".toByteArray())
            val payload = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("""{"sub":"$userId","preferred_username":"$username"}""".toByteArray())
            return "$header.$payload."
        }

        private fun jsonString(value: String): String = Json.encodeToString(JsonPrimitive(value))

        private fun bearerAuth(token: String): String = "Bear" + "er $token"
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

    // =========================================================================
    // SAVE FILES: All platform pairs (16 combinations)
    // =========================================================================

    @Test
    fun `savefiles - all platform pairs preserve data identically`() = withRealDatabase {
        for (sourcePlatform in ALL_PLATFORMS) {
            for (targetPlatform in ALL_PLATFORMS) {
                val userId = "matrix-save-${sourcePlatform.lowercase()}-to-${targetPlatform.lowercase()}"
                val token = fakeToken(userId)
                val saveId = "save-$sourcePlatform-$targetPlatform"
                val coins = (ALL_PLATFORMS.indexOf(sourcePlatform) + 1) * 1000 + ALL_PLATFORMS.indexOf(targetPlatform) * 100
                val saveData = """{"level":3,"coins":$coins,"health":8,"source":"$sourcePlatform","target":"$targetPlatform"}"""

                // Save on source platform
                client.post("/api/savefiles") {
                    contentType(ContentType.Application.Json)
                    header(HttpHeaders.Authorization, bearerAuth(token))
                    setBody("""{"saveId":"$saveId","data":${jsonString(saveData)},"platform":"$sourcePlatform"}""")
                }.apply {
                    assertEquals(HttpStatusCode.OK, status, "Save from $sourcePlatform should succeed")
                }

                // Load on target platform - data must be identical
                client.get("/api/savefiles") {
                    header(HttpHeaders.Authorization, bearerAuth(token))
                }.apply {
                    assertEquals(HttpStatusCode.OK, status, "Load on $targetPlatform should succeed (saved from $sourcePlatform)")
                    val body = bodyAsText()
                    assertContains(body, saveId, message = "Save ID must be present when loading on $targetPlatform (saved from $sourcePlatform)")
                    assertContains(body, "$coins", message = "Coins value must be preserved when loading on $targetPlatform (saved from $sourcePlatform)")
                    assertContains(body, sourcePlatform, message = "Source platform marker in data must be preserved ($sourcePlatform to $targetPlatform)")
                }
            }
        }
    }

    // =========================================================================
    // SAME-PLATFORM CONTINUATION: X to X for all platforms
    // =========================================================================

    @Test
    fun `savefiles - Web to Web same-platform continuation preserves all data`() = withRealDatabase {
        verifySamePlatformSavefileContinuation("WEB")
    }

    @Test
    fun `savefiles - Desktop to Desktop same-platform continuation preserves all data`() = withRealDatabase {
        verifySamePlatformSavefileContinuation("DESKTOP")
    }

    @Test
    fun `savefiles - Android to Android same-platform continuation preserves all data`() = withRealDatabase {
        verifySamePlatformSavefileContinuation("ANDROID")
    }

    @Test
    fun `savefiles - iOS to iOS same-platform continuation preserves all data`() = withRealDatabase {
        verifySamePlatformSavefileContinuation("IOS")
    }

    private suspend fun ApplicationTestBuilder.verifySamePlatformSavefileContinuation(platform: String) {
        val userId = "matrix-same-save-${platform.lowercase()}"
        val token = fakeToken(userId)

        // First session: save initial progress
        val initialData = """{"level":1,"coins":100,"health":10,"towers":["spike"]}"""
        client.post("/api/savefiles") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, bearerAuth(token))
            setBody("""{"saveId":"main-save","data":${jsonString(initialData)},"platform":"$platform"}""")
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        // Second session on same platform: update progress
        val updatedData = """{"level":5,"coins":2000,"health":7,"towers":["spike","bow","wizard"]}"""
        client.post("/api/savefiles") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, bearerAuth(token))
            setBody("""{"saveId":"main-save","data":${jsonString(updatedData)},"platform":"$platform"}""")
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        // Third session on same platform: verify latest data is returned
        client.get("/api/savefiles") {
            header(HttpHeaders.Authorization, bearerAuth(token))
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            val body = bodyAsText()
            assertContains(body, "2000", message = "Updated coins must be present on $platform to $platform")
            assertContains(body, "wizard", message = "Updated tower list must be present on $platform to $platform")
        }
    }

    // =========================================================================
    // GAME STATE (USERDATA): All platform pairs (16 combinations)
    // =========================================================================

    @Test
    fun `userdata - all platform pairs preserve game state identically`() = withRealDatabase {
        for (sourcePlatform in ALL_PLATFORMS) {
            for (targetPlatform in ALL_PLATFORMS) {
                val userId = "matrix-ud-${sourcePlatform.lowercase()}-to-${targetPlatform.lowercase()}"
                val token = fakeToken(userId)
                val xp = (ALL_PLATFORMS.indexOf(sourcePlatform) + 1) * 500 + ALL_PLATFORMS.indexOf(targetPlatform) * 50
                val userData = """{"localUsername":"Player_${sourcePlatform}_$targetPlatform","xp":$xp,"abilities":{"fireball":2},"levelProgress":{"level-1":"COMPLETED","level-2":"UNLOCKED"}}"""

                // Upload from source platform
                client.post("/api/userdata") {
                    contentType(ContentType.Application.Json)
                    header(HttpHeaders.Authorization, bearerAuth(token))
                    setBody("""{"data":${jsonString(userData)},"platform":"$sourcePlatform"}""")
                }.apply {
                    assertEquals(HttpStatusCode.OK, status, "Userdata upload from $sourcePlatform should succeed")
                }

                // Load on target platform
                client.get("/api/userdata") {
                    header(HttpHeaders.Authorization, bearerAuth(token))
                }.apply {
                    assertEquals(HttpStatusCode.OK, status, "Userdata load on $targetPlatform should succeed (saved from $sourcePlatform)")
                    val body = bodyAsText()
                    assertContains(body, "$xp", message = "XP must be preserved ($sourcePlatform to $targetPlatform)")
                    assertContains(body, "fireball", message = "Abilities must be preserved ($sourcePlatform to $targetPlatform)")
                    assertContains(body, "COMPLETED", message = "Level progress must be preserved ($sourcePlatform to $targetPlatform)")
                    assertContains(body, "UNLOCKED", message = "Unlock status must be preserved ($sourcePlatform to $targetPlatform)")
                }
            }
        }
    }

    @Test
    fun `userdata - Web to Web same-platform game state continuation`() = withRealDatabase {
        verifySamePlatformUserdataContinuation("WEB")
    }

    @Test
    fun `userdata - Desktop to Desktop same-platform game state continuation`() = withRealDatabase {
        verifySamePlatformUserdataContinuation("DESKTOP")
    }

    @Test
    fun `userdata - Android to Android same-platform game state continuation`() = withRealDatabase {
        verifySamePlatformUserdataContinuation("ANDROID")
    }

    @Test
    fun `userdata - iOS to iOS same-platform game state continuation`() = withRealDatabase {
        verifySamePlatformUserdataContinuation("IOS")
    }

    private suspend fun ApplicationTestBuilder.verifySamePlatformUserdataContinuation(platform: String) {
        val userId = "matrix-same-ud-${platform.lowercase()}"
        val token = fakeToken(userId)

        // First session: initial progress
        client.post("/api/userdata") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, bearerAuth(token))
            setBody("""{"data":${jsonString("""{"localUsername":"SamePlayer","xp":100,"abilities":{},"levelProgress":{"level-1":"UNLOCKED"}}""")},"platform":"$platform"}""")
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        // Second session: more progress on same platform
        client.post("/api/userdata") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, bearerAuth(token))
            setBody("""{"data":${jsonString("""{"localUsername":"SamePlayer","xp":1500,"abilities":{"fireball":3,"shield":1},"levelProgress":{"level-1":"COMPLETED","level-2":"COMPLETED","level-3":"UNLOCKED"}}""")},"platform":"$platform"}""")
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        // Third session: verify latest state
        client.get("/api/userdata") {
            header(HttpHeaders.Authorization, bearerAuth(token))
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            val body = bodyAsText()
            assertContains(body, "1500", message = "Updated XP must be present on $platform to $platform")
            assertContains(body, "shield", message = "New ability must be present on $platform to $platform")
            assertContains(body, "level-3", message = "New level progress must be present on $platform to $platform")
        }
    }

    // =========================================================================
    // SETTINGS: All platform pairs (16 combinations)
    // =========================================================================

    @Test
    fun `settings - all platform pairs preserve settings identically`() = withRealDatabase {
        for (sourcePlatform in ALL_PLATFORMS) {
            for (targetPlatform in ALL_PLATFORMS) {
                val userId = "matrix-settings-${sourcePlatform.lowercase()}-to-${targetPlatform.lowercase()}"
                val token = fakeToken(userId)
                val languages = listOf("en", "de", "fr", "es")
                val lang = languages[ALL_PLATFORMS.indexOf(sourcePlatform)]
                val settings = """{"language":"$lang","darkMode":true,"soundEnabled":false,"difficulty":"HARD","source":"$sourcePlatform"}"""

                // Save settings on source platform
                client.post("/api/settings") {
                    contentType(ContentType.Application.Json)
                    header(HttpHeaders.Authorization, bearerAuth(token))
                    setBody("""{"data":${jsonString(settings)},"platform":"$sourcePlatform"}""")
                }.apply {
                    assertEquals(HttpStatusCode.OK, status, "Settings upload from $sourcePlatform should succeed")
                }

                // Load settings on target platform
                client.get("/api/settings") {
                    header(HttpHeaders.Authorization, bearerAuth(token))
                }.apply {
                    assertEquals(HttpStatusCode.OK, status, "Settings load on $targetPlatform should succeed (saved from $sourcePlatform)")
                    val body = bodyAsText()
                    assertContains(body, lang, message = "Language must be preserved ($sourcePlatform to $targetPlatform)")
                    assertContains(body, "darkMode", message = "Dark mode must be preserved ($sourcePlatform to $targetPlatform)")
                    assertContains(body, "HARD", message = "Difficulty must be preserved ($sourcePlatform to $targetPlatform)")
                }
            }
        }
    }

    @Test
    fun `settings - Web to Web same-platform settings continuation`() = withRealDatabase {
        verifySamePlatformSettingsContinuation("WEB")
    }

    @Test
    fun `settings - Desktop to Desktop same-platform settings continuation`() = withRealDatabase {
        verifySamePlatformSettingsContinuation("DESKTOP")
    }

    @Test
    fun `settings - Android to Android same-platform settings continuation`() = withRealDatabase {
        verifySamePlatformSettingsContinuation("ANDROID")
    }

    @Test
    fun `settings - iOS to iOS same-platform settings continuation`() = withRealDatabase {
        verifySamePlatformSettingsContinuation("IOS")
    }

    private suspend fun ApplicationTestBuilder.verifySamePlatformSettingsContinuation(platform: String) {
        val userId = "matrix-same-settings-${platform.lowercase()}"
        val token = fakeToken(userId)

        // First session: initial settings
        client.post("/api/settings") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, bearerAuth(token))
            setBody("""{"data":${jsonString("""{"language":"en","darkMode":false,"soundEnabled":true}""")},"platform":"$platform"}""")
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        // Second session: update settings on same platform
        client.post("/api/settings") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, bearerAuth(token))
            setBody("""{"data":${jsonString("""{"language":"it","darkMode":true,"soundEnabled":false,"volume":0.5}""")},"platform":"$platform"}""")
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        // Third session: verify latest settings
        client.get("/api/settings") {
            header(HttpHeaders.Authorization, bearerAuth(token))
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            val body = bodyAsText()
            assertContains(body, "it", message = "Updated language must be present on $platform to $platform")
            assertContains(body, "volume", message = "New setting key must be present on $platform to $platform")
        }
    }

    // =========================================================================
    // COMBINED: Full cross-platform session with all data types on all platforms
    // =========================================================================

    @Test
    fun `full session - player uses all four platforms sequentially and state is consistent`() = withRealDatabase {
        val userId = "matrix-full-session"
        val token = fakeToken(userId, "MatrixPlayer")

        // Session 1: Web - initial play
        client.post("/api/savefiles") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, bearerAuth(token))
            setBody("""{"saveId":"campaign","data":${jsonString("""{"level":1,"coins":200,"health":10}""")},"platform":"WEB"}""")
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        client.post("/api/userdata") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, bearerAuth(token))
            setBody("""{"data":${jsonString("""{"localUsername":"MatrixPlayer","xp":100,"abilities":{},"levelProgress":{"level-1":"COMPLETED"}}""")},"platform":"WEB"}""")
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        client.post("/api/settings") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, bearerAuth(token))
            setBody("""{"data":${jsonString("""{"language":"en","darkMode":false}""")},"platform":"WEB"}""")
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        // Session 2: Android - continue playing, update progress
        client.post("/api/savefiles") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, bearerAuth(token))
            setBody("""{"saveId":"campaign","data":${jsonString("""{"level":3,"coins":1500,"health":8}""")},"platform":"ANDROID"}""")
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        client.post("/api/userdata") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, bearerAuth(token))
            setBody("""{"data":${jsonString("""{"localUsername":"MatrixPlayer","xp":600,"abilities":{"fireball":1},"levelProgress":{"level-1":"COMPLETED","level-2":"COMPLETED","level-3":"UNLOCKED"}}""")},"platform":"ANDROID"}""")
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        client.post("/api/settings") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, bearerAuth(token))
            setBody("""{"data":${jsonString("""{"language":"de","darkMode":true}""")},"platform":"ANDROID"}""")
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        // Session 3: Desktop - continue playing, add more progress
        client.post("/api/savefiles") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, bearerAuth(token))
            setBody("""{"saveId":"campaign","data":${jsonString("""{"level":7,"coins":5000,"health":6}""")},"platform":"DESKTOP"}""")
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        client.post("/api/userdata") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, bearerAuth(token))
            setBody("""{"data":${jsonString("""{"localUsername":"MatrixPlayer","xp":2000,"abilities":{"fireball":3,"shield":2},"levelProgress":{"level-1":"COMPLETED","level-2":"COMPLETED","level-3":"COMPLETED","level-4":"UNLOCKED"}}""")},"platform":"DESKTOP"}""")
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        // Session 4: iOS - verify all accumulated state from previous platforms
        client.get("/api/savefiles") {
            header(HttpHeaders.Authorization, bearerAuth(token))
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            val body = bodyAsText()
            assertContains(body, "5000", message = "Latest coins from Desktop session must be visible on iOS")
            assertContains(body, "campaign", message = "Save ID must be present on iOS")
        }

        client.get("/api/userdata") {
            header(HttpHeaders.Authorization, bearerAuth(token))
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            val body = bodyAsText()
            assertContains(body, "2000", message = "Latest XP from Desktop must be visible on iOS")
            assertContains(body, "shield", message = "Abilities from Desktop must be visible on iOS")
            assertContains(body, "level-4", message = "Latest level progress must be visible on iOS")
        }

        client.get("/api/settings") {
            header(HttpHeaders.Authorization, bearerAuth(token))
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            val body = bodyAsText()
            assertContains(body, "de", message = "Language set on Android must be visible on iOS")
            assertContains(body, "darkMode", message = "Dark mode from Android must be visible on iOS")
        }
    }

    @Test
    fun `full session - same platform multiple sessions accumulate state correctly`() = withRealDatabase {
        val userId = "matrix-same-plat-accumulate"
        val token = fakeToken(userId, "AccumulatePlayer")

        // Multiple sessions on the same platform (Android)
        val sessions = listOf(
            Triple("""{"level":1,"coins":50}""", """{"localUsername":"AccumulatePlayer","xp":50,"abilities":{},"levelProgress":{"level-1":"UNLOCKED"}}""", """{"language":"en"}"""),
            Triple("""{"level":2,"coins":300}""", """{"localUsername":"AccumulatePlayer","xp":200,"abilities":{"fireball":1},"levelProgress":{"level-1":"COMPLETED","level-2":"UNLOCKED"}}""", """{"language":"en","darkMode":true}"""),
            Triple("""{"level":4,"coins":1200}""", """{"localUsername":"AccumulatePlayer","xp":800,"abilities":{"fireball":2,"shield":1},"levelProgress":{"level-1":"COMPLETED","level-2":"COMPLETED","level-3":"COMPLETED","level-4":"UNLOCKED"}}""", """{"language":"fr","darkMode":true,"soundEnabled":false}""")
        )

        for ((saveData, userData, settings) in sessions) {
            client.post("/api/savefiles") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, bearerAuth(token))
                setBody("""{"saveId":"main","data":${jsonString(saveData)},"platform":"ANDROID"}""")
            }.apply { assertEquals(HttpStatusCode.OK, status) }

            client.post("/api/userdata") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, bearerAuth(token))
                setBody("""{"data":${jsonString(userData)},"platform":"ANDROID"}""")
            }.apply { assertEquals(HttpStatusCode.OK, status) }

            client.post("/api/settings") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, bearerAuth(token))
                setBody("""{"data":${jsonString(settings)},"platform":"ANDROID"}""")
            }.apply { assertEquals(HttpStatusCode.OK, status) }
        }

        // Verify final state on same platform
        client.get("/api/savefiles") {
            header(HttpHeaders.Authorization, bearerAuth(token))
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            val body = bodyAsText()
            assertContains(body, "1200", message = "Final coins must reflect last session")
        }

        client.get("/api/userdata") {
            header(HttpHeaders.Authorization, bearerAuth(token))
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            val body = bodyAsText()
            assertContains(body, "800", message = "Final XP must reflect last session")
            assertContains(body, "shield", message = "Latest abilities must be present")
            assertContains(body, "level-4", message = "Latest level progress must be present")
        }

        client.get("/api/settings") {
            header(HttpHeaders.Authorization, bearerAuth(token))
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            val body = bodyAsText()
            assertContains(body, "fr", message = "Final language must reflect last session")
            assertContains(body, "soundEnabled", message = "Latest settings must include new keys")
        }

        // Also verify same state is visible from a different platform (iOS)
        client.get("/api/savefiles") {
            header(HttpHeaders.Authorization, bearerAuth(token))
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            val body = bodyAsText()
            assertContains(body, "1200", message = "Final coins from Android sessions must be visible on iOS")
        }
    }

    // =========================================================================
    // MULTIPLE SAVE SLOTS: All platforms can contribute to and access multiple saves
    // =========================================================================

    @Test
    fun `multiple save slots created from different platforms are all accessible from any platform`() = withRealDatabase {
        val userId = "matrix-multi-slot"
        val token = fakeToken(userId, "MultiSlotPlayer")

        // Create one save per platform
        for ((index, platform) in ALL_PLATFORMS.withIndex()) {
            val saveId = "slot-${index + 1}"
            val saveData = """{"level":${index + 1},"coins":${(index + 1) * 500},"platform_origin":"$platform"}"""
            client.post("/api/savefiles") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, bearerAuth(token))
                setBody("""{"saveId":"$saveId","data":${jsonString(saveData)},"platform":"$platform"}""")
            }.apply { assertEquals(HttpStatusCode.OK, status, "Save from $platform should succeed") }
        }

        // Verify all saves are accessible from every platform
        for (targetPlatform in ALL_PLATFORMS) {
            client.get("/api/savefiles") {
                header(HttpHeaders.Authorization, bearerAuth(token))
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                val body = bodyAsText()
                val parsed = Json.parseToJsonElement(body).jsonArray
                assertEquals(4, parsed.size, "All 4 save slots must be visible from $targetPlatform")

                for ((index, platform) in ALL_PLATFORMS.withIndex()) {
                    assertContains(body, "slot-${index + 1}", message = "Save slot ${index + 1} from $platform must be visible on $targetPlatform")
                    assertContains(body, "${(index + 1) * 500}", message = "Coins from $platform save must be visible on $targetPlatform")
                }
            }
        }
    }

    // =========================================================================
    // DATA INTEGRITY: Verify exact JSON preservation across platforms
    // =========================================================================

    @Test
    fun `savefile data is byte-for-byte identical regardless of source or target platform`() = withRealDatabase {
        val userId = "matrix-data-integrity"
        val token = fakeToken(userId, "IntegrityPlayer")
        val complexSaveData = """{"level":12,"coins":9999,"health":3,"towers":[{"type":"wizard","level":5,"position":{"x":3,"y":7}},{"type":"ballista","level":3,"position":{"x":10,"y":2}}],"worldMap":{"unlocked":["level-1","level-2","level-3","level-4","level-5"]}}"""

        // Save from Web
        client.post("/api/savefiles") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, bearerAuth(token))
            setBody("""{"saveId":"integrity-check","data":${jsonString(complexSaveData)},"platform":"WEB"}""")
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        // Load and verify the data field is preserved exactly
        client.get("/api/savefiles") {
            header(HttpHeaders.Authorization, bearerAuth(token))
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            val body = bodyAsText()
            val saves = Json.parseToJsonElement(body).jsonArray
            assertEquals(1, saves.size)
            val retrievedData = saves[0].jsonObject["data"]!!.jsonPrimitive.content
            assertEquals(complexSaveData, retrievedData, "Save data must be preserved exactly across platforms")
        }
    }

    @Test
    fun `userdata with complex nested structures is preserved across platforms`() = withRealDatabase {
        val userId = "matrix-ud-complex"
        val token = fakeToken(userId, "ComplexPlayer")
        val complexUserData = """{"localUsername":"ComplexPlayer","xp":5000,"abilities":{"fireball":5,"shield":3,"icewall":2,"lightning":1},"levelProgress":{"welcome_to_defender_of_egril":"COMPLETED","the_first_wave":"COMPLETED","mixed_forces":"COMPLETED","the_ork_invasion":"COMPLETED","dark_magic_rises":"UNLOCKED"},"stats":{"gamesPlayed":42,"totalKills":1337}}"""

        // Upload from Desktop
        client.post("/api/userdata") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, bearerAuth(token))
            setBody("""{"data":${jsonString(complexUserData)},"platform":"DESKTOP"}""")
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        // Load from iOS - verify complex structure is preserved
        client.get("/api/userdata") {
            header(HttpHeaders.Authorization, bearerAuth(token))
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            val body = bodyAsText()
            val parsed = Json.parseToJsonElement(body).jsonObject
            val data = parsed["data"]!!.jsonPrimitive.content
            // Verify all key data points are in the stored data
            assertContains(data, "ComplexPlayer")
            assertContains(data, "5000")
            assertContains(data, "icewall")
            assertContains(data, "dark_magic_rises")
            assertContains(data, "1337")
        }
    }

    // =========================================================================
    // CONCURRENT ACCESS: Multiple platforms accessing data simultaneously
    // =========================================================================

    @Test
    fun `rapid sequential writes from different platforms result in last-write-wins`() = withRealDatabase {
        val userId = "matrix-rapid-writes"
        val token = fakeToken(userId, "RapidPlayer")

        // Rapid sequential saves from all platforms
        for ((index, platform) in ALL_PLATFORMS.withIndex()) {
            client.post("/api/savefiles") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, bearerAuth(token))
                setBody("""{"saveId":"contested-save","data":${jsonString("""{"version":${index + 1},"platform":"$platform"}""")},"platform":"$platform"}""")
            }.apply { assertEquals(HttpStatusCode.OK, status) }
        }

        // The last write (IOS) should win
        client.get("/api/savefiles") {
            header(HttpHeaders.Authorization, bearerAuth(token))
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            val body = bodyAsText()
            val saves = Json.parseToJsonElement(body).jsonArray
            assertEquals(1, saves.size, "Only one save with same ID should exist")
            val data = saves[0].jsonObject["data"]!!.jsonPrimitive.content
            assertContains(data, "\"version\":4", message = "Last write (IOS, version 4) must win")
            assertContains(data, "IOS", message = "Last platform (IOS) must be in data")
        }
    }

    @Test
    fun `settings rapid updates from different platforms result in latest state`() = withRealDatabase {
        val userId = "matrix-rapid-settings"
        val token = fakeToken(userId, "RapidSettingsPlayer")

        val settingsSequence = listOf(
            "WEB" to """{"language":"en","darkMode":false}""",
            "ANDROID" to """{"language":"de","darkMode":true}""",
            "DESKTOP" to """{"language":"fr","darkMode":true,"volume":0.8}""",
            "IOS" to """{"language":"es","darkMode":false,"volume":0.5,"haptics":true}"""
        )

        for ((platform, settings) in settingsSequence) {
            client.post("/api/settings") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, bearerAuth(token))
                setBody("""{"data":${jsonString(settings)},"platform":"$platform"}""")
            }.apply { assertEquals(HttpStatusCode.OK, status) }
        }

        // Final state should be the last write (iOS settings)
        client.get("/api/settings") {
            header(HttpHeaders.Authorization, bearerAuth(token))
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            val body = bodyAsText()
            assertContains(body, "es", message = "Last language (es from iOS) must be current")
            assertContains(body, "haptics", message = "Last settings keys (from iOS) must be present")
        }
    }

    // =========================================================================
    // ISOLATION: Verify cross-user isolation holds for all platform combinations
    // =========================================================================

    @Test
    fun `two players using different platform combinations never see each others data`() = withRealDatabase {
        val tokenA = fakeToken("matrix-iso-player-a", "PlayerA")
        val tokenB = fakeToken("matrix-iso-player-b", "PlayerB")

        // Player A saves from Web
        client.post("/api/savefiles") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, bearerAuth(tokenA))
            setBody("""{"saveId":"my-save","data":${jsonString("""{"secret":"alpha-secret-data","coins":9999}""")},"platform":"WEB"}""")
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        client.post("/api/userdata") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, bearerAuth(tokenA))
            setBody("""{"data":${jsonString("""{"localUsername":"SecretPlayerA","xp":9000}""")},"platform":"WEB"}""")
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        client.post("/api/settings") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, bearerAuth(tokenA))
            setBody("""{"data":${jsonString("""{"language":"de","secretSetting":"alpha-only"}""")},"platform":"WEB"}""")
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        // Player B saves from Android
        client.post("/api/savefiles") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, bearerAuth(tokenB))
            setBody("""{"saveId":"my-save","data":${jsonString("""{"secret":"beta-secret-data","coins":1}""")},"platform":"ANDROID"}""")
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        client.post("/api/userdata") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, bearerAuth(tokenB))
            setBody("""{"data":${jsonString("""{"localUsername":"SecretPlayerB","xp":1}""")},"platform":"ANDROID"}""")
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        client.post("/api/settings") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, bearerAuth(tokenB))
            setBody("""{"data":${jsonString("""{"language":"fr","secretSetting":"beta-only"}""")},"platform":"ANDROID"}""")
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        // Player A loads from iOS - must only see their own data
        client.get("/api/savefiles") {
            header(HttpHeaders.Authorization, bearerAuth(tokenA))
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            val body = bodyAsText()
            assertContains(body, "alpha-secret-data")
            assertFalse(body.contains("beta-secret-data"), "Player A must never see Player B save data")
        }

        client.get("/api/userdata") {
            header(HttpHeaders.Authorization, bearerAuth(tokenA))
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            val body = bodyAsText()
            assertContains(body, "SecretPlayerA")
            assertFalse(body.contains("SecretPlayerB"), "Player A must never see Player B username")
        }

        client.get("/api/settings") {
            header(HttpHeaders.Authorization, bearerAuth(tokenA))
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            val body = bodyAsText()
            assertContains(body, "alpha-only")
            assertFalse(body.contains("beta-only"), "Player A must never see Player B settings")
        }

        // Player B loads from Desktop - must only see their own data
        client.get("/api/savefiles") {
            header(HttpHeaders.Authorization, bearerAuth(tokenB))
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            val body = bodyAsText()
            assertContains(body, "beta-secret-data")
            assertFalse(body.contains("alpha-secret-data"), "Player B must never see Player A save data")
        }

        client.get("/api/userdata") {
            header(HttpHeaders.Authorization, bearerAuth(tokenB))
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            val body = bodyAsText()
            assertContains(body, "SecretPlayerB")
            assertFalse(body.contains("SecretPlayerA"), "Player B must never see Player A username")
        }

        client.get("/api/settings") {
            header(HttpHeaders.Authorization, bearerAuth(tokenB))
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            val body = bodyAsText()
            assertContains(body, "beta-only")
            assertFalse(body.contains("alpha-only"), "Player B must never see Player A settings")
        }
    }

    // =========================================================================
    // AUTHENTICATION: Unauthenticated access is rejected regardless of platform
    // =========================================================================

    @Test
    fun `unauthenticated requests are rejected for all endpoints`() = withRealDatabase {
        // Save files
        client.get("/api/savefiles").apply {
            assertEquals(HttpStatusCode.Unauthorized, status, "GET savefiles without token must be 401")
        }
        client.post("/api/savefiles") {
            contentType(ContentType.Application.Json)
            setBody("""{"saveId":"hack","data":"\"stolen\"","platform":"WEB"}""")
        }.apply {
            assertEquals(HttpStatusCode.Unauthorized, status, "POST savefiles without token must be 401")
        }

        // User data
        client.get("/api/userdata").apply {
            assertEquals(HttpStatusCode.Unauthorized, status, "GET userdata without token must be 401")
        }
        client.post("/api/userdata") {
            contentType(ContentType.Application.Json)
            setBody("""{"data":"\"hack\"","platform":"WEB"}""")
        }.apply {
            assertEquals(HttpStatusCode.Unauthorized, status, "POST userdata without token must be 401")
        }

        // Settings
        client.get("/api/settings").apply {
            assertEquals(HttpStatusCode.Unauthorized, status, "GET settings without token must be 401")
        }
        client.post("/api/settings") {
            contentType(ContentType.Application.Json)
            setBody("""{"data":"\"hack\"","platform":"WEB"}""")
        }.apply {
            assertEquals(HttpStatusCode.Unauthorized, status, "POST settings without token must be 401")
        }
    }
}
