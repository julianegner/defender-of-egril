package de.egril.defender

import kotlinx.serialization.Serializable

/**
 * Request body for `POST /api/crash`.
 *
 * Sent by the frontend whenever the global error boundary catches an unhandled
 * error or an uncaught exception. Mirrors the field set used by
 * [FeedbackSubmissionRequest] for client identification (version, commit hash,
 * platform, platform-extended) and additionally carries the error type and
 * message (non-localized), the in-memory game log buffer and the current
 * settings JSON.
 *
 * `errorMessage` and `stackTrace` are optional because some platforms (e.g.
 * Kotlin/Wasm) do not always provide them, and the user can also choose to
 * not send error information at all – in which case the frontend will not
 * call this endpoint.
 */
@Serializable
data class CrashReportRequest(
    val crashId: String,
    val errorType: String,
    val errorMessage: String? = null,
    val stackTrace: String? = null,
    val gameLog: String? = null,
    val settingsJson: String? = null,
    val platform: String,
    val platformLong: String? = null,
    val platformExtended: String? = null,
    val osName: String? = null,
    val versionName: String? = null,
    val commitHash: String? = null,
)

@Serializable
data class CrashReportResponse(
    val accepted: Boolean,
    val duplicate: Boolean = false,
)
