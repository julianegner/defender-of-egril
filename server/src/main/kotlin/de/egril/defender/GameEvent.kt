package de.egril.defender

import kotlinx.serialization.Serializable

/**
 * Represents a game lifecycle event sent by the frontend.
 *
 * @param event One of: APP_STARTED, LEVEL_STARTED, LEVEL_WON, LEVEL_LOST, GAME_LEFT
 * @param levelName Display name of the level, present for all events except APP_STARTED
 */
@Serializable
data class GameEvent(
    val event: String,
    val levelName: String? = null
)
