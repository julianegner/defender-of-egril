package de.egril.defender.analytics

/**
 * Typed enumeration of the game lifecycle events sent to the analytics backend.
 *
 * Each entry carries the [apiValue] string that is written to the API so the
 * wire format stays stable regardless of future enum renames.
 */
enum class GameEventType(val apiValue: String) {
    APP_STARTED("APP_STARTED"),
    LEVEL_STARTED("LEVEL_STARTED"),
    LEVEL_WON("LEVEL_WON"),
    LEVEL_LOST("LEVEL_LOST"),
    LEVEL_LEFT("LEVEL_LEFT")
}
