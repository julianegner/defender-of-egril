package de.egril.defender.analytics

private const val PLATFORM = "ANDROID"

actual fun reportEvent(eventType: String, levelName: String?) {
    postEventJson(eventType, levelName, PLATFORM)
}
