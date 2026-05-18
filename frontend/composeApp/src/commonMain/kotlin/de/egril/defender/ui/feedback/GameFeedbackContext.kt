package de.egril.defender.ui.feedback

/**
 * Optional game context passed to the feedback form when triggered from within a game session.
 * Provides level name, turn number, and a compact JSON game state summary for bug reports.
 *
 * @param levelName Localized display name of the current level.
 * @param turnNumber Current turn number in the active game session.
 * @param gameStateJson Valid JSON string with a compact snapshot of the current game state.
 *   Expected schema: `{"levelId":N,"levelName":"...","turn":N,"hp":N,"coins":N,"defenders":N,"attackers":N,"phase":"..."}`
 * @param sourceContext Where the feedback button was triggered (e.g. "GAMEPLAY").
 */
data class GameFeedbackContext(
    val levelName: String,
    val turnNumber: Int,
    val gameStateJson: String,
    val sourceContext: String = "GAMEPLAY"
)

/** Escapes a string for safe inclusion in a manually-built JSON string value. */
internal fun escapeForJson(s: String): String = buildString {
    for (c in s) {
        when (c) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> append(c)
        }
    }
}
