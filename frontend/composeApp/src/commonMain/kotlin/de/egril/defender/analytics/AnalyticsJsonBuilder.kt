package de.egril.defender.analytics

import de.egril.defender.AppBuildInfo
import de.egril.defender.utils.getPlatform

/**
 * Builds the JSON payload for an analytics event.
 */
internal fun buildEventJson(
    eventType: GameEventType,
    levelName: String?,
    platform: String,
    turnNumber: Int? = null,
    difficulty: String? = null,
    url: String? = null,
    installUuid: String? = null,
): String =
    buildString {
        val currentPlatform = getPlatform()
        val platformLong = currentPlatform.name
        val platformExtended = currentPlatform.platformExtended
        val osName = currentPlatform.osName
        append("{\"event\":\"")
        append(escapeJson(eventType.apiValue))
        append("\",\"platform\":\"")
        append(escapeJson(platform))
        append("\",\"platformLong\":\"")
        append(escapeJson(platformLong))
        append("\",\"platformExtended\":\"")
        append(escapeJson(platformExtended))
        append("\"")
        if (osName != null) {
            append(",\"osName\":\"")
            append(escapeJson(osName))
            append("\"")
        }
        append(",\"versionName\":\"")
        append(escapeJson(AppBuildInfo.VERSION_NAME))
        append("\",\"commitHash\":\"")
        append(escapeJson(AppBuildInfo.COMMIT_HASH))
        append("\"")
        if (installUuid != null) {
            append(",\"installUuid\":\"")
            append(escapeJson(installUuid))
            append("\"")
        }
        if (levelName != null) {
            append(",\"levelName\":\"")
            append(escapeJson(levelName))
            append("\"")
        }
        if (turnNumber != null) {
            append(",\"turnNumber\":")
            append(turnNumber)
        }
        if (difficulty != null) {
            append(",\"difficulty\":\"")
            append(escapeJson(difficulty))
            append("\"")
        }
        if (url != null) {
            append(",\"url\":\"")
            append(escapeJson(url))
            append("\"")
        }
        append("}")
    }

internal fun escapeJson(value: String): String =
    buildString {
        for (ch in value) {
            when (ch) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (ch.code < 0x20) append("\\u${ch.code.toString(16).padStart(4, '0')}") else append(ch)
            }
        }
    }
