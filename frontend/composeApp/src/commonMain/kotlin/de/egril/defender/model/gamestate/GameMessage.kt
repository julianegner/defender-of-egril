package de.egril.defender.model

/**
 * An in-game event message queued for display to the player.
 * @param type          The kind of event.
 * @param name          Optional name (target name or gate name); for [GameMessageType.EVENT_MESSAGE]
 *                      it is the optional string-resource key of the predefined text (may be null).
 * @param eventActions  For [GameMessageType.EVENT_MESSAGE]: the actions the event applied, so the
 *                      granted elements (coins, mana, supports, …) can be shown to the player.
 * @param eventMessageFrame For [GameMessageType.EVENT_MESSAGE]: the visual frame configured for the
 *                      message in the level editor (see [EventMessageFrameId]); null = standard frame.
* @param highlightPositions  Optional pair of positions to highlight (e.g., old and new position for coven swap).
* @param eventMessageAmount  Optional numeric story argument captured when the event fires.
*/
data class GameMessage(
    val type: GameMessageType,
    val name: String? = null,
    val eventActions: List<EventAction>? = null,
    val eventMessageFrame: EventMessageFrameId? = null,
    val highlightPositions: Pair<Position, Position>? = null,
    val eventMessageAmount: Int? = null, // Numeric story argument captured when the event fired
)
