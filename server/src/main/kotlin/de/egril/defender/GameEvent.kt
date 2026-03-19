package de.egril.defender

import kotlinx.serialization.Serializable

/**
 * Represents a game lifecycle event sent by the frontend.
 *
 * @param event One of: APP_STARTED, LEVEL_STARTED, LEVEL_WON, LEVEL_LOST, GAME_LEFT
 * @param levelName Display name of the level, present for all events except APP_STARTED
 * @param platform The frontend platform that sent this event (e.g. WEB, DESKTOP, ANDROID, IOS)
 * @param versionName The version name of the frontend (e.g. "1.0"), optional
 * @param commitHash The short git commit hash of the frontend build, optional
 */
@Serializable
data class GameEvent(
    val event: String,
    val levelName: String? = null,
    val platform: String,
    val versionName: String? = null,
    val commitHash: String? = null
)
