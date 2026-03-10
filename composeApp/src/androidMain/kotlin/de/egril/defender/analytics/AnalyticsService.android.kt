package de.egril.defender.analytics

actual fun reportEvent(eventType: String, levelName: String?) {
    // Android does not connect to the backend analytics service
}
