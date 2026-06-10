package de.egril.defender

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertContains

class ApplicationTest {
    private val minimalPngBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+Xb1cAAAAASUVORK5CYII="

    @Test
    fun testRoot() = testApplication {
        application {
            module()
        }
        client.get("/").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("Defender of Egril Backend", bodyAsText())
        }
    }

    @Test
    fun testPostEventAppStarted() = testApplication {
        application {
            module()
        }
        client.post("/api/events") {
            contentType(ContentType.Application.Json)
            setBody("""{"event":"APP_STARTED","platform":"WEB"}""")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }

    @Test
    fun testPostEventAppStartedWithUrl() = testApplication {
        application {
            module()
        }
        client.post("/api/events") {
            contentType(ContentType.Application.Json)
            setBody("""{"event":"APP_STARTED","platform":"WEB","url":"https://egril.de/game"}""")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }

    @Test
    fun testPostEventLevelStarted() = testApplication {
        application {
            module()
        }
        client.post("/api/events") {
            contentType(ContentType.Application.Json)
            setBody("""{"event":"LEVEL_STARTED","levelName":"Welcome to Egril","platform":"WEB"}""")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }

    @Test
    fun testPostEventLevelWon() = testApplication {
        application {
            module()
        }
        client.post("/api/events") {
            contentType(ContentType.Application.Json)
            setBody("""{"event":"LEVEL_WON","levelName":"Welcome to Egril","platform":"WEB"}""")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }

    @Test
    fun testPostEventLevelLost() = testApplication {
        application {
            module()
        }
        client.post("/api/events") {
            contentType(ContentType.Application.Json)
            setBody("""{"event":"LEVEL_LOST","levelName":"Welcome to Egril","platform":"WEB"}""")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }

    @Test
    fun testPostEventGameLeft() = testApplication {
        application {
            module()
        }
        client.post("/api/events") {
            contentType(ContentType.Application.Json)
            setBody("""{"event":"GAME_LEFT","levelName":"Welcome to Egril","platform":"WEB"}""")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }

    @Test
    fun testPostEventGameWon() = testApplication {
        application {
            module()
        }
        client.post("/api/events") {
            contentType(ContentType.Application.Json)
            setBody("""{"event":"GAME_WON","levelName":"The Final Stand","platform":"WEB"}""")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }

    @Test
    fun testPostEventWithUnknownFields() = testApplication {
        application {
            module()
        }
        client.post("/api/events") {
            contentType(ContentType.Application.Json)
            setBody("""{"event":"APP_STARTED","platform":"WEB","unknownFutureField":"someValue","anotherNewField":42}""")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }

    @Test
    fun testPostEventMalformedJson() = testApplication {
        application {
            module()
        }
        client.post("/api/events") {
            contentType(ContentType.Application.Json)
            setBody("not valid json")
        }.apply {
            assertEquals(HttpStatusCode.BadRequest, status)
        }
    }

    @Test
    fun testPostFeedbackNoDatabaseReturns503() = testApplication {
        application { module() }
        client.post("/api/feedback") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "feedbackId":"11111111-1111-4111-8111-111111111111",
                  "feedbackType":"FEATURE_REQUEST",
                  "message":"Please add endless mode",
                  "platform":"WEB"
                }
                """.trimIndent()
            )
        }.apply {
            assertEquals(HttpStatusCode.ServiceUnavailable, status)
        }
    }

    @Test
    fun testPostFeedbackInvalidUuidReturns400() = testApplication {
        application { module() }
        client.post("/api/feedback") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "feedbackId":"not-a-uuid",
                  "feedbackType":"FEATURE_REQUEST",
                  "message":"Please add endless mode",
                  "platform":"WEB"
                }
                """.trimIndent()
            )
        }.apply {
            assertEquals(HttpStatusCode.BadRequest, status)
            assertContains(bodyAsText(), "UUID")
        }
    }

    @Test
    fun testPostFeedbackBugReportRequiresScreenshotAndLog() = testApplication {
        application { module() }
        client.post("/api/feedback") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "feedbackId":"22222222-2222-4222-8222-222222222222",
                  "feedbackType":"BUG_REPORT",
                  "bugTypes":["UI"],
                  "message":"Tower tooltip overlaps controls",
                  "platform":"WEB"
                }
                """.trimIndent()
            )
        }.apply {
            assertEquals(HttpStatusCode.BadRequest, status)
            assertContains(bodyAsText(), "screenshot")
        }
    }

    @Test
    fun testPostFeedbackBugReportAcceptsScreenshotAttachment() = testApplication {
        application { module() }
        client.post("/api/feedback") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "feedbackId":"23222222-2222-4222-8222-222222222222",
                  "feedbackType":"BUG_REPORT",
                  "bugTypes":["UI"],
                  "message":"Tower tooltip overlaps controls",
                  "platform":"WEB",
                  "gameLog":"turn=7",
                  "attachments":[
                    {
                      "filename":"screenshot.png",
                      "mimeType":"image/png",
                      "base64Content":"$minimalPngBase64"
                    }
                  ]
                }
                """.trimIndent()
            )
        }.apply {
            assertEquals(HttpStatusCode.ServiceUnavailable, status)
        }
    }

    // ---------------------------------------------------------------------------
    // Crash report endpoint tests
    // ---------------------------------------------------------------------------

    @Test
    fun testPostCrashNoDatabaseReturns503() = testApplication {
        application { module() }
        client.post("/api/crash") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "crashId":"33333333-3333-4333-8333-333333333333",
                  "errorType":"java.lang.IllegalStateException",
                  "errorMessage":"boom",
                  "platform":"WEB"
                }
                """.trimIndent()
            )
        }.apply {
            assertEquals(HttpStatusCode.ServiceUnavailable, status)
        }
    }

    @Test
    fun testPostCrashInvalidUuidReturns400() = testApplication {
        application { module() }
        client.post("/api/crash") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "crashId":"not-a-uuid",
                  "errorType":"java.lang.IllegalStateException",
                  "platform":"WEB"
                }
                """.trimIndent()
            )
        }.apply {
            assertEquals(HttpStatusCode.BadRequest, status)
            assertContains(bodyAsText(), "UUID")
        }
    }

    @Test
    fun testPostCrashBlankErrorTypeReturns400() = testApplication {
        application { module() }
        client.post("/api/crash") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "crashId":"44444444-4444-4444-8444-444444444444",
                  "errorType":"",
                  "platform":"WEB"
                }
                """.trimIndent()
            )
        }.apply {
            assertEquals(HttpStatusCode.BadRequest, status)
            assertContains(bodyAsText(), "errorType")
        }
    }

    @Test
    fun testPostCrashMalformedJsonReturns400() = testApplication {
        application { module() }
        client.post("/api/crash") {
            contentType(ContentType.Application.Json)
            setBody("not valid json")
        }.apply {
            assertEquals(HttpStatusCode.BadRequest, status)
        }
    }

    // ---------------------------------------------------------------------------
    // Savefile endpoint tests
    // ---------------------------------------------------------------------------

    @Test
    fun testUploadSavefileRequiresAuth() = testApplication {
        application { module() }
        client.post("/api/savefiles") {
            contentType(ContentType.Application.Json)
            setBody("""{"saveId":"save1","data":"{}"}""")
        }.apply {
            assertEquals(HttpStatusCode.Unauthorized, status)
        }
    }

    @Test
    fun testUploadSavefileWithFakeTokenNoDatabase() = testApplication {
        application { module() }
        // A minimal fake JWT with a sub claim so auth passes, but no real DB is available
        // Header: {"alg":"none"} | Payload: {"sub":"user-123"} | no signature
        val fakeToken = "eyJhbGciOiJub25lIn0.eyJzdWIiOiJ1c2VyLTEyMyJ9."
        client.post("/api/savefiles") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $fakeToken")
            setBody("""{"saveId":"save1","data":"{}"}""")
        }.apply {
            // Without a database, the endpoint returns 503
            assertEquals(HttpStatusCode.ServiceUnavailable, status)
        }
    }

    @Test
    fun testListSavefilesRequiresAuth() = testApplication {
        application { module() }
        client.get("/api/savefiles").apply {
            assertEquals(HttpStatusCode.Unauthorized, status)
        }
    }

    @Test
    fun testListSavefilesWithFakeTokenNoDatabase() = testApplication {
        application { module() }
        val fakeToken = "eyJhbGciOiJub25lIn0.eyJzdWIiOiJ1c2VyLTEyMyJ9."
        client.get("/api/savefiles") {
            header(HttpHeaders.Authorization, "Bearer $fakeToken")
        }.apply {
            // Without a database, the endpoint returns 503
            assertEquals(HttpStatusCode.ServiceUnavailable, status)
        }
    }

    @Test
    fun testUploadSavefileMalformedJson() = testApplication {
        application { module() }
        val fakeToken = "eyJhbGciOiJub25lIn0.eyJzdWIiOiJ1c2VyLTEyMyJ9."
        client.post("/api/savefiles") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $fakeToken")
            setBody("not valid json")
        }.apply {
            assertEquals(HttpStatusCode.BadRequest, status)
        }
    }

    // ---------------------------------------------------------------------------
    // Backchannel logout endpoint tests
    // ---------------------------------------------------------------------------

    @Test
    fun testBackchannelLogoutWithValidToken() = testApplication {
        application { module() }
        // Simulate Keycloak sending a backchannel logout POST with a logout_token.
        // The token uses algorithm "none" for test simplicity – the same pattern used
        // throughout this test class for fake Bearer tokens. Real Keycloak tokens are
        // RS256-signed; full JWKS validation is intentionally omitted because the
        // backend holds no server-side sessions (stateless JWT) so a spoofed request
        // is a harmless no-op.
        client.post("/api/backchannel-logout") {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody("logout_token=eyJhbGciOiJub25lIn0.eyJzdWIiOiJ1c2VyLTEyMyIsInNpZCI6InNlc3Npb24tMTIzIn0.")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }

    @Test
    fun testBackchannelLogoutMissingToken() = testApplication {
        application { module() }
        // Missing logout_token parameter should return 400
        client.post("/api/backchannel-logout") {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody("other_param=value")
        }.apply {
            assertEquals(HttpStatusCode.BadRequest, status)
        }
    }

    @Test
    fun testBackchannelLogoutMalformedToken() = testApplication {
        application { module() }
        // A value that is not a three-part JWT should return 400
        client.post("/api/backchannel-logout") {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody("logout_token=not-a-jwt")
        }.apply {
            assertEquals(HttpStatusCode.BadRequest, status)
        }
    }
}
