package de.egril.defender.analytics

val backendUrl: String = "https://defender-backend.egril.de"

/**
 * Reports a game lifecycle event to the backend analytics endpoint.
 *
 * @param eventType The type of event (APP_STARTED, TUTORIAL_DEEP_LINK, LEVEL_STARTED, LEVEL_LOADED, LEVEL_WON, LEVEL_LOST, LEVEL_LEFT, GAME_WON)
 * @param levelName Display name of the current level, or null for APP_STARTED
 * @param turnNumber The current game turn number, present for LEVEL_WON, LEVEL_LOST, and LEVEL_LEFT
 * @param difficulty The selected game difficulty (e.g. BABY, EASY, MEDIUM, HARD, NIGHTMARE), optional
 */
expect fun reportEvent(eventType: GameEventType, levelName: String?, turnNumber: Int? = null, difficulty: String? = null)
