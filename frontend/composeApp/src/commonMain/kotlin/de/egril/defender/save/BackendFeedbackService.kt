package de.egril.defender.save

data class FeedbackSubmitRequest(
    val feedbackId: String,
    val feedbackType: String,
    val bugTypes: List<String>,
    val message: String,
    val contactEmail: String?,
    val sourceContext: String?,
    val gameLevelName: String?,
    val gameTurnNumber: Int?,
    val gameStateJson: String?,
    val gameLog: String?,
    val screenshotBase64: String?
)

expect object BackendFeedbackService {
    suspend fun submitFeedback(request: FeedbackSubmitRequest, token: String?): Boolean
}

internal fun buildFeedbackUploadJson(request: FeedbackSubmitRequest): String = buildString {
    val platform = de.egril.defender.utils.getClientPlatformName()
    val platformLong = de.egril.defender.utils.getPlatform().name
    val versionName = de.egril.defender.AppBuildInfo.VERSION_NAME
    val commitHash = de.egril.defender.AppBuildInfo.COMMIT_HASH

    append("{")
    append("\"feedbackId\":\"${escapeJsonString(request.feedbackId)}\",")
    append("\"feedbackType\":\"${escapeJsonString(request.feedbackType)}\",")
    append("\"bugTypes\":[${request.bugTypes.joinToString(",") { "\"${escapeJsonString(it)}\"" }}],")
    append("\"message\":\"${escapeJsonString(request.message)}\",")
    appendNullableString("contactEmail", request.contactEmail)
    append(',')
    appendNullableString("sourceContext", request.sourceContext)
    append(',')
    appendNullableString("gameLevelName", request.gameLevelName)
    append(',')
    if (request.gameTurnNumber != null) {
        append("\"gameTurnNumber\":${request.gameTurnNumber},")
    } else {
        append("\"gameTurnNumber\":null,")
    }
    appendNullableString("gameStateJson", request.gameStateJson)
    append(',')
    appendNullableString("gameLog", request.gameLog)
    append(',')
    appendNullableString("screenshotBase64", request.screenshotBase64)
    append(',')
    appendClientInfo(platform, platformLong, versionName, commitHash)
    append("}")
}

private fun StringBuilder.appendNullableString(key: String, value: String?) {
    if (value == null) {
        append("\"$key\":null")
    } else {
        append("\"$key\":\"${escapeJsonString(value)}\"")
    }
}
