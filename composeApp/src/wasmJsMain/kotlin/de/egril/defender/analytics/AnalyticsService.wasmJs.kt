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
    postJson("/api/events", buildEventJson(eventType, levelName, PLATFORM))
}
