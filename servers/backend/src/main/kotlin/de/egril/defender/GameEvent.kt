package de.egril.defender

import kotlinx.serialization.Serializable

/**
 * Represents a game lifecycle event sent by the frontend.
 *
 * @param event One of: APP_STARTED, APP_CLOSED, TUTORIAL_DEEP_LINK, LEVEL_STARTED, LEVEL_LOADED, LEVEL_WON, LEVEL_LOST, LEVEL_LEFT, GAME_WON
 * @param levelName Display name of the level when applicable
 * @param platform The short frontend platform identifier (e.g. WEB, DESKTOP, ANDROID, IOS)
 * @param platformLong The full platform name including user agent for web (e.g. "Web with Kotlin/Wasm Mozilla/5.0 ..."), optional
 * @param platformExtended Extended OS/browser detail: OS name+version for desktop, Android release version for Android, iOS system+version for iOS, browser name+version for web, optional
 * @param osName The operating system name and version (e.g. "Ubuntu 22.04.5 LTS", "Android 14", "iOS 17.0", "Windows 10/11"), optional
 * @param versionName The version name of the frontend (e.g. "1.0"), optional
 * @param commitHash The short git commit hash of the frontend build, optional
 * @param installUuid Pseudonymous install UUID used for anonymous install counting, optional
 * @param turnNumber The current game turn number at the time of the event, optional
 * @param difficulty The selected game difficulty (BABY, EASY, MEDIUM, HARD, NIGHTMARE), optional
 * @param url The full URL of the web page at the time of the event (web platform, APP_STARTED and APP_CLOSED), optional
 */
@Serializable
data class GameEvent(
    val event: String,
    val levelName: String? = null,
    val platform: String,
    val platformLong: String? = null,
    val platformExtended: String? = null,
    val osName: String? = null,
    val versionName: String? = null,
    val commitHash: String? = null,
    val installUuid: String? = null,
    val turnNumber: Int? = null,
    val difficulty: String? = null,
    val url: String? = null,
)
