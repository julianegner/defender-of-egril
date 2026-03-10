package de.egril.defender

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals

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
            setBody("""{"event":"APP_STARTED"}""")
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
            setBody("""{"event":"LEVEL_STARTED","levelName":"Welcome to Egril"}""")
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
            setBody("""{"event":"LEVEL_WON","levelName":"Welcome to Egril"}""")
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
            setBody("""{"event":"LEVEL_LOST","levelName":"Welcome to Egril"}""")
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
            setBody("""{"event":"GAME_LEFT","levelName":"Welcome to Egril"}""")
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
}
