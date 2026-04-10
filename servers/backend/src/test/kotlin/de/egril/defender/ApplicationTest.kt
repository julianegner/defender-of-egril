package de.egril.defender

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertContains

class ApplicationTest {
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
