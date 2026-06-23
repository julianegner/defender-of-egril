package de.egril.defender

import kotlinx.serialization.Serializable

@Serializable
data class FeedbackAttachmentDto(
    val filename: String,
    val mimeType: String,
    val base64Content: String,
)

@Serializable
data class FeedbackSubmissionRequest(
    val feedbackId: String,
    val feedbackType: String,
    val bugTypes: List<String> = emptyList(),
    val message: String,
    val contactEmail: String? = null,
    val sourceContext: String? = null,
    val userLanguage: String? = null,
    val platform: String,
    val platformLong: String? = null,
    val platformExtended: String? = null,
    val osName: String? = null,
    val versionName: String? = null,
    val commitHash: String? = null,
    val gameLevelName: String? = null,
    val gameTurnNumber: Int? = null,
    val currentSettingsJson: String? = null,
    val gameStateJson: String? = null,
    val gameLog: String? = null,
    val screenshotBase64: String? = null,
    val attachments: List<FeedbackAttachmentDto> = emptyList(),
)

@Serializable
data class FeedbackSubmissionResponse(
    val accepted: Boolean,
    val duplicate: Boolean = false,
)

internal enum class FeedbackType {
    BUG_REPORT,
    TYPO_TRANSLATION_TEXT,
    FEATURE_REQUEST,
    ADDITIONAL_LANGUAGE_REQUEST,
    INFO_REQUEST,
    LEGAL_PROBLEM,
    OTHER,
}

internal enum class BugType {
    VISUAL,
    UI,
    GAMEPLAY,
    PERFORMANCE,
    SOUND,
    CRASH,
}
