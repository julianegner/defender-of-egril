package de.egril.defender.model

/**
 * A story message that was shown to the player, together with the round in which it was shown.
 */
data class StoryLogEntry(
    val turn: Int,
    val message: GameMessage,
)
