package de.egril.defender.analytics

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

class AnalyticsJsonBuilderTest {
    @Test
    fun `buildEventJson includes difficulty when provided`() {
        val json =
            buildEventJson(
                eventType = GameEventType.LEVEL_STARTED,
                levelName = "Tutorial",
                platform = "DESKTOP",
                difficulty = "HARD",
                installUuid = "11111111-1111-4111-8111-111111111111",
            )

        assertContains(json, "\"difficulty\":\"HARD\"")
        assertContains(json, "\"installUuid\":\"")
    }

    @Test
    fun `buildEventJson includes url when provided`() {
        val json =
            buildEventJson(
                eventType = GameEventType.APP_STARTED,
                levelName = null,
                platform = "WEB",
                url = "https://egril.de/game?level=1",
                installUuid = "11111111-1111-4111-8111-111111111111",
            )

        assertContains(json, "\"url\":\"https://egril.de/game?level=1\"")
        assertContains(json, "\"installUuid\":\"")
    }

    @Test
    fun `buildEventJson omits url when not provided`() {
        val json =
            buildEventJson(
                eventType = GameEventType.APP_STARTED,
                levelName = null,
                platform = "WEB",
                installUuid = "11111111-1111-4111-8111-111111111111",
            )

        assertFalse(json.contains("\"url\":"))
        assertContains(json, "\"installUuid\":\"")
    }

    @Test
    fun `buildEventJson includes app closed gameplay context`() {
        val json =
            buildEventJson(
                eventType = GameEventType.APP_CLOSED,
                levelName = "Tutorial",
                platform = "WEB",
                turnNumber = 12,
                difficulty = "HARD",
                url = "https://egril.de/game?level=tutorial",
                installUuid = "11111111-1111-4111-8111-111111111111",
            )

        assertContains(json, "\"event\":\"APP_CLOSED\"")
        assertContains(json, "\"levelName\":\"Tutorial\"")
        assertContains(json, "\"turnNumber\":12")
        assertContains(json, "\"difficulty\":\"HARD\"")
        assertContains(json, "\"url\":\"https://egril.de/game?level=tutorial\"")
        assertContains(json, "\"installUuid\":\"")
        assertFalse(json.contains("\"username\":"))
    }
}
