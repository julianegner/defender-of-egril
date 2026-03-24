package de.egril.defender.analytics

/**
 * Reports a game lifecycle event to the backend analytics endpoint.
 *
 * @param eventType One of: APP_STARTED, LEVEL_STARTED, LEVEL_WON, LEVEL_LOST, GAME_LEFT
 * @param levelName Display name of the current level, or null for APP_STARTED
 */
expect fun reportEvent(eventType: String, levelName: String?)
