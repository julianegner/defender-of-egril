package de.egril.defender.analytics

import de.egril.defender.AppBuildInfo
import de.egril.defender.iam.IamService
import de.egril.defender.utils.getPlatform

/**
 * Builds the JSON payload for an analytics event.
 */
internal fun buildEventJson(eventType: GameEventType, levelName: String?, platform: String, turnNumber: Int? = null): String = buildString {
    val currentPlatform = getPlatform()
    val platformLong = currentPlatform.name
    val platformExtended = currentPlatform.platformExtended
    val iamState = IamService.state.value
    val username = if (iamState.isAuthenticated) iamState.username else null
    append("{\"event\":\"")
    append(escapeJson(eventType.apiValue))
    append("\",\"platform\":\"")
    append(escapeJson(platform))
    append("\",\"platformLong\":\"")
    append(escapeJson(platformLong))
    append("\",\"platformExtended\":\"")
    append(escapeJson(platformExtended))
    append("\",\"versionName\":\"")
    append(escapeJson(AppBuildInfo.VERSION_NAME))
    append("\",\"commitHash\":\"")
    append(escapeJson(AppBuildInfo.COMMIT_HASH))
    append("\"")
    if (levelName != null) {
        append(",\"levelName\":\"")
        append(escapeJson(levelName))
        append("\"")
    }
    if (username != null) {
        append(",\"username\":\"")
        append(escapeJson(username))
        append("\"")
    }
    if (turnNumber != null) {
        append(",\"turnNumber\":")
        append(turnNumber)
    }
    append("}")
}

internal fun escapeJson(value: String): String = buildString {
    for (ch in value) {
        when (ch) {
            '\\' -> append("\\\\")
            '"'  -> append("\\\"")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> if (ch.code < 0x20) append("\\u${ch.code.toString(16).padStart(4, '0')}") else append(ch)
        }
    }
}
