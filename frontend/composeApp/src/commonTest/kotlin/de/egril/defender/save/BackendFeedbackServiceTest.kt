package de.egril.defender.save

import kotlin.test.Test
import kotlin.test.assertContains

class BackendFeedbackServiceTest {

    @Test
    fun buildFeedbackUploadJson_includesCurrentSettingsJsonField() {
        val request = FeedbackSubmitRequest(
            feedbackId = "123e4567-e89b-12d3-a456-426614174000",
            feedbackType = "FEATURE_REQUEST",
            bugTypes = emptyList(),
            message = "Please add this feature",
            contactEmail = null,
            sourceContext = "INFO_PAGE",
            userLanguage = "EN",
            gameLevelName = null,
            gameTurnNumber = null,
            currentSettingsJson = """{"language":"en","darkMode":"false"}""",
            gameStateJson = null,
            gameLog = null
        )

        val payload = buildFeedbackUploadJson(request)

        assertContains(payload, "\"currentSettingsJson\":\"{\\\"language\\\":\\\"en\\\",\\\"darkMode\\\":\\\"false\\\"}\"")
    }
}
