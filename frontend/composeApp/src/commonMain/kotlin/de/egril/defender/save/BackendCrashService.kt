package de.egril.defender.save

/**
 * Payload for an unhandled-error report submitted by the frontend error boundary.
 * Mirrors [FeedbackSubmitRequest] field set for client identification so the
 * backend can record version, commit hash, platform and platform-extended data
 * in addition to the (non-localized) error type/message, in-memory game log
 * and current settings JSON.
 */
data class CrashReportSubmitRequest(
    val crashId: String,
    val errorType: String,
    val errorMessage: String?,
    val stackTrace: String?,
    val gameLog: String?,
    val settingsJson: String?
)

expect object BackendCrashService {
    /**
     * Submit a crash report to the backend. Returns null on HTTP 2xx, otherwise
     * the HTTP status code (or -1 on network/transport errors).
     *
     * @param token optional bearer token of the currently authenticated user.
     */
    suspend fun submitCrashReport(request: CrashReportSubmitRequest, token: String?): Int?
}

internal fun buildCrashReportUploadJson(request: CrashReportSubmitRequest): String = buildString {
    val platform = de.egril.defender.utils.getClientPlatformName()
    val platformLong = de.egril.defender.utils.getPlatform().name
    val versionName = de.egril.defender.AppBuildInfo.VERSION_NAME
    val commitHash = de.egril.defender.AppBuildInfo.COMMIT_HASH

    append("{")
    append("\"crashId\":\"${escapeJsonString(request.crashId)}\",")
    append("\"errorType\":\"${escapeJsonString(request.errorType)}\",")
    appendNullableJsonString("errorMessage", request.errorMessage)
    append(',')
    appendNullableJsonString("stackTrace", request.stackTrace)
    append(',')
    appendNullableJsonString("gameLog", request.gameLog)
    append(',')
    appendNullableJsonString("settingsJson", request.settingsJson)
    append(',')
    appendClientInfo(platform, platformLong, versionName, commitHash)
    append("}")
}

private fun StringBuilder.appendNullableJsonString(key: String, value: String?) {
    if (value == null) {
        append("\"$key\":null")
    } else {
        append("\"$key\":\"${escapeJsonString(value)}\"")
    }
}
