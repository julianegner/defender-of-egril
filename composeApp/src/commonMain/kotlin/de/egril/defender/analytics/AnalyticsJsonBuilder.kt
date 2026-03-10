package de.egril.defender.analytics

/**
 * Builds the JSON payload for an analytics event.
 */
internal fun buildEventJson(eventType: String, levelName: String?, platform: String): String = buildString {
    append("{\"event\":\"")
    append(escapeJson(eventType))
    append("\",\"platform\":\"")
    append(escapeJson(platform))
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
