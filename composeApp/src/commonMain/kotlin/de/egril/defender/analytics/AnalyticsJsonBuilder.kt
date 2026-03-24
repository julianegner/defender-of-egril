package de.egril.defender.analytics

import de.egril.defender.AppBuildInfo
import de.egril.defender.utils.getPlatform

/**
 * Builds the JSON payload for an analytics event.
 */
internal fun buildEventJson(eventType: String, levelName: String?, platform: String): String = buildString {
    val platformLong = getPlatform().name
    append("{\"event\":\"")
    append(escapeJson(eventType))
    append("\",\"platform\":\"")
    append(escapeJson(platform))
    append("\",\"platformLong\":\"")
    append(escapeJson(platformLong))
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
