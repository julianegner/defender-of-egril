@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package de.egril.defender.analytics

/**
 * Fire-and-forget HTTP POST via the browser Fetch API.
 * Errors are silently swallowed so analytics never disrupts gameplay.
 */
@JsFun("(url, body) => { fetch(url, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: body }).catch(() => {}); }")
private external fun postJson(url: String, body: String)

private const val PLATFORM = "WEB"

actual fun reportEvent(eventType: String, levelName: String?) {
    val json = buildString {
        append("{\"event\":\"")
        append(eventType)
        append("\",\"platform\":\"")
        append(PLATFORM)
        append("\"")
        if (levelName != null) {
            append(",\"levelName\":\"")
            append(escapeJson(levelName))
            append("\"")
        }
        append("}")
    }
    postJson("/api/events", json)
}

private fun escapeJson(value: String): String = buildString {
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
