package de.egril.defender.analytics

/**
 * Reports a game lifecycle event to the backend analytics endpoint.
 *
 * @param eventType The type of event (APP_STARTED, LEVEL_STARTED, LEVEL_WON, LEVEL_LOST, LEVEL_LEFT)
 * @param levelName Display name of the current level, or null for APP_STARTED
 * @param turnNumber The current game turn number, present for LEVEL_WON, LEVEL_LOST, and LEVEL_LEFT
 */
expect fun reportEvent(eventType: GameEventType, levelName: String?, turnNumber: Int? = null)
