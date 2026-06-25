package de.egril.defender.save

import kotlin.test.Test
import kotlin.test.assertContains

class BackendFeedbackServiceTest {
    @Test
    fun buildFeedbackUploadJson_includesCurrentSettingsJsonField() {
        val settingsJson =
            serializeSettingsJson(
                mapOf(
                    "language" to "en",
                    "darkMode" to "false",
                ),
            )
        val request =
            FeedbackSubmitRequest(
                feedbackId = "123e4567-e89b-12d3-a456-426614174000",
                installUuid = "123e4567-e89b-12d3-a456-426614174001",
                feedbackType = "FEATURE_REQUEST",
                bugTypes = emptyList(),
                message = "Please add this feature",
                contactEmail = null,
                sourceContext = "INFO_PAGE",
                userLanguage = "EN",
                gameLevelName = null,
                gameTurnNumber = null,
                currentSettingsJson = settingsJson,
                gameStateJson = null,
                gameLog = null,
            )

        val payload = buildFeedbackUploadJson(request)

        assertContains(payload, "\"currentSettingsJson\":")
        assertContains(payload, "\"installUuid\":\"123e4567-e89b-12d3-a456-426614174001\"")
        assertContains(payload, "\\\"language\\\": \\\"en\\\"")
        assertContains(payload, "\\\"darkMode\\\": \\\"false\\\"")
    }

    @Test
    fun buildFeedbackUploadJson_allowsNullCurrentSettingsJsonField() {
        val request =
            FeedbackSubmitRequest(
                feedbackId = "123e4567-e89b-12d3-a456-426614174000",
                installUuid = "123e4567-e89b-12d3-a456-426614174001",
                feedbackType = "FEATURE_REQUEST",
                bugTypes = emptyList(),
                message = "Please add this feature",
                contactEmail = null,
                sourceContext = "INFO_PAGE",
                userLanguage = "EN",
                gameLevelName = null,
                gameTurnNumber = null,
                currentSettingsJson = null,
                gameStateJson = null,
                gameLog = null,
            )

        val payload = buildFeedbackUploadJson(request)

        assertContains(payload, "\"currentSettingsJson\":null")
    }
}
