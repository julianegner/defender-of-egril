package de.egril.defender.analytics

private const val PLATFORM = "ANDROID"

actual fun reportEvent(eventType: GameEventType, levelName: String?, turnNumber: Int?) {
    postEventJson(eventType, levelName, PLATFORM, turnNumber)
}
