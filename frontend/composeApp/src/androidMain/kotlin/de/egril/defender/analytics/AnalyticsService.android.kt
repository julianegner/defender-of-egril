package de.egril.defender.analytics

private const val PLATFORM = "ANDROID"

actual fun reportEvent(eventType: GameEventType, levelName: String?, turnNumber: Int?, difficulty: String?) {
    postEventJson(eventType, levelName, PLATFORM, turnNumber, difficulty)
}
