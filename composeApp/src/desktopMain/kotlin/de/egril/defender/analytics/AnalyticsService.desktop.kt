package de.egril.defender.analytics

private const val PLATFORM = "DESKTOP"

actual fun reportEvent(eventType: String, levelName: String?) {
    postEventJson(eventType, levelName, PLATFORM)
}
