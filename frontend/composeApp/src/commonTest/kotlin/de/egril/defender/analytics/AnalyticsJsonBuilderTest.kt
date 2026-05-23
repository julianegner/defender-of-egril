package de.egril.defender.analytics

import de.egril.defender.iam.IamService
import de.egril.defender.iam.IamState
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

class AnalyticsJsonBuilderTest {

    @Test
    fun `buildEventJson includes difficulty when provided`() {
        IamService.state.value = IamState()
        val json = buildEventJson(
            eventType = GameEventType.LEVEL_STARTED,
            levelName = "Tutorial",
            platform = "DESKTOP",
            difficulty = "HARD"
        )

        assertContains(json, "\"difficulty\":\"HARD\"")
    }

    @Test
    fun `buildEventJson omits difficulty when not provided`() {
        IamService.state.value = IamState()
        val json = buildEventJson(
            eventType = GameEventType.APP_STARTED,
            levelName = null,
            platform = "WEB"
        )

        assertFalse(json.contains("\"difficulty\":"))
    }
}
