@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package de.egril.defender.analytics

import de.egril.defender.iam.IamService

/**
 * Fire-and-forget HTTP POST via the browser Fetch API.
 * Errors are silently swallowed so analytics never disrupts gameplay.
 * If the user is authenticated via IAM, the Bearer token is attached as an optional header.
 */
@JsFun("(url, body, token) => { const headers = { 'Content-Type': 'application/json' }; if (token) { headers['Authorization'] = 'Bearer ' + token; } fetch(url, { method: 'POST', headers: headers, body: body }).catch(() => {}); }")
private external fun postJson(url: String, body: String, token: String?)

@JsFun(
    """(url, body) => {
        try {
            if (typeof navigator !== 'undefined' && typeof navigator.sendBeacon === 'function') {
                return navigator.sendBeacon(url, new Blob([body], { type: 'application/json' }));
            }
        } catch (e) {}
        try {
            fetch(url, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: body,
                keepalive: true
            }).catch(() => {});
        } catch (e) {}
        return false;
    }"""
)
private external fun postJsonOnPageHide(url: String, body: String): Boolean

@JsFun("() => { try { return window.location.href; } catch (e) { return null; } }")
private external fun getCurrentUrl(): String?

private const val PLATFORM = "WEB"

actual fun reportEvent(eventType: GameEventType, levelName: String?, turnNumber: Int?, difficulty: String?) {
    val url = if (eventType == GameEventType.APP_STARTED || eventType == GameEventType.APP_CLOSED) getCurrentUrl() else null
    val json = buildEventJson(eventType, levelName, PLATFORM, turnNumber, difficulty, url)
    if (eventType == GameEventType.APP_CLOSED) {
        postJsonOnPageHide("$backendUrl/api/events", json)
    } else {
        postJson("$backendUrl/api/events", json, IamService.getToken())
    }
}
