package de.egril.defender.model

/**
 * Scripted level events.
 *
 * Each [LevelEvent] pairs a [EventCondition] with a list of [EventAction]s and/or an optional
 * predefined story message. When the condition is met during gameplay the actions are applied and
 * the message (if any) is shown to the player. Events are configured in the level editor's
 * "Events" tab and stored with the level.
 */

/**
 * The kind of condition that triggers a [LevelEvent].
 */
enum class EventConditionType {
    /** Fires at the beginning of a player turn. */
    TURN_START,

    /** Fires at the beginning of an enemy turn. */
    ENEMY_TURN_START,

    /** Fires when the total number of enemies killed reaches [EventCondition.threshold]. */
    ENEMIES_KILLED,

    /**
     * Fires when the number of killed enemies of [EventCondition.attackerType] reaches
     * [EventCondition.threshold].
     */
    ENEMY_TYPE_KILLED,

    /**
     * Fires when a unit reaches [EventCondition.position]. When [EventCondition.attackerType] is set
     * only units of that type count, otherwise any unit qualifies.
     */
    UNIT_REACHED,

    /** Fires when the player's health points are at or below [EventCondition.threshold]. */
    HEALTH_AT_OR_BELOW,

    /** Fires when the player's mana is at or below [EventCondition.threshold]. */
    MANA_AT_OR_BELOW,

    /** Fires when the player's coins are at or below [EventCondition.threshold]. */
    COINS_AT_OR_BELOW,
}

/**
 * A condition that must be satisfied for a [LevelEvent] to fire.
 *
 * @param type          The kind of condition.
 * @param fromTurn      The event is only evaluated from this turn onwards (0 = from the start).
 * @param threshold     Numeric threshold (kill count / health / mana / coins) depending on [type].
 * @param attackerType  Optional enemy type for [EventConditionType.ENEMY_TYPE_KILLED] and
 *                      [EventConditionType.UNIT_REACHED].
 * @param position      Target tile for [EventConditionType.UNIT_REACHED].
 */
data class EventCondition(
    val type: EventConditionType,
    val fromTurn: Int = 0,
    val threshold: Int = 0,
    val attackerType: AttackerType? = null,
    val position: Position? = null,
)

/**
 * The kind of effect an [EventAction] applies.
 */
enum class EventActionType {
    /** Grant [EventAction.amount] coins to the player. */
    GIVE_COINS,

    /** Grant [EventAction.amount] mana to the player (capped at the current max). */
    GIVE_MANA,

    /**
     * Grant [EventAction.amount] uses of the placeable support object [EventAction.supportObjectType]
     * (getting support / reinforcements).
     */
    GIVE_SUPPORT_OBJECT,

    /** Grant [EventAction.amount] casts of the support spell [EventAction.spellType]. */
    GIVE_SUPPORT_SPELL,

    /** Destroy the dwarven mine located at [EventAction.position] (e.g. a dragon destroys a mine). */
    DESTROY_MINE,

    /** Activate the tile zone [EventAction.zoneId] (e.g. flood the lowlands at high tide). */
    APPLY_TILE_ZONE,

    /** Deactivate the tile zone [EventAction.zoneId], restoring the base map tiles. */
    REVERT_TILE_ZONE,

    /** Activate the tile zone [EventAction.zoneId] when inactive, otherwise deactivate it. */
    TOGGLE_TILE_ZONE,

    /** Stop the running loop of the event [EventAction.targetEventId]. */
    STOP_EVENT_LOOP,
}

/**
 * A single effect applied when a [LevelEvent] fires.
 *
 * @param type              The kind of effect.
 * @param amount            Amount (coins / mana / support count) depending on [type].
 * @param supportObjectType Support object granted for [EventActionType.GIVE_SUPPORT_OBJECT].
 * @param spellType         Spell granted for [EventActionType.GIVE_SUPPORT_SPELL].
 * @param position          Mine tile for [EventActionType.DESTROY_MINE].
 * @param zoneId            Tile zone for the tile-zone actions.
 * @param targetEventId     Event whose loop is stopped by [EventActionType.STOP_EVENT_LOOP].
 */
data class EventAction(
    val type: EventActionType,
    val amount: Int = 0,
    val supportObjectType: SupportObjectType? = null,
    val spellType: SpellType? = null,
    val position: Position? = null,
    val zoneId: String? = null,
    val targetEventId: String? = null,
)

/**
 * Identifier of the visual frame used for an event message popup.
 *
 * A frame id is the suffix of one of the game's `message_background_<id>` drawables (for example
 * `kraken` or `waaagh`), so every existing message frame — the story frame, the villain frames and
 * any frame added later — can be selected for a scripted-event message. `null` means the standard
 * story frame, which is what event messages used before frames became selectable.
 *
 * Frame ids are kept as plain strings so newly added frame artwork is offered automatically without
 * a code change, and so unknown ids in old or hand-written level files simply fall back to the
 * standard frame instead of breaking the level.
 */
typealias EventMessageFrameId = String

/**
 * One step of an [EventLoop].
 *
 * When the step becomes current, it waits [waitTurns] player turns, then applies [actions] (and
 * shows [messageKey], if set). Afterwards the optional [nestedLoop] runs completely before the
 * enclosing loop continues with its next step.
 *
 * No message popup is shown when [messageKey] is null. [messageFrame] selects the popup's visual
 * frame and is only relevant when a message is shown.
 */
data class EventLoopStep(
    val waitTurns: Int = 1,
    val actions: List<EventAction> = emptyList(),
    val messageKey: String? = null,
    val messageFrame: EventMessageFrameId? = null,
    val nestedLoop: EventLoop? = null,
)

/**
 * A sequence of [steps] that is repeated [repeatCount] times, or endlessly when [repeatCount] is 0.
 * Loops can be nested via [EventLoopStep.nestedLoop].
 */
data class EventLoop(
    val steps: List<EventLoopStep> = emptyList(),
    val repeatCount: Int = 0,
) {
    val isEndless: Boolean get() = repeatCount <= 0

    /**
     * True when a single pass through this loop (including nested loops) waits at least one turn.
     * A loop without any waiting would run forever within a single turn and is therefore invalid.
     */
    fun waitsAtLeastOneTurnPerPass(): Boolean = steps.any { it.waitTurns > 0 || it.nestedLoop?.waitsAtLeastOneTurnPerPass() == true }

    /** True when this loop and all nested loops wait at least one turn per pass. */
    fun isValid(): Boolean = steps.isNotEmpty() && waitsAtLeastOneTurnPerPass() && steps.all { it.nestedLoop?.isValid() ?: true }

    /** True when an endless nested loop prevents later steps of an enclosing loop from ever running. */
    fun hasUnreachableSteps(): Boolean =
        steps.withIndex().any { (index, step) ->
            val nested = step.nestedLoop
            nested != null && ((nested.isEndless && index < steps.lastIndex) || nested.hasUnreachableSteps())
        }

    /** All actions used anywhere in this loop, including nested loops. */
    fun allActions(): List<EventAction> = steps.flatMap { it.actions + (it.nestedLoop?.allActions() ?: emptyList()) }
}

/**
 * A scripted event: a condition, the effects it applies, and an optional predefined story message.
 *
 * @param id          Unique identifier within the level (used to track whether it already fired).
 * @param condition   Condition that triggers the event.
 * @param actions     Effects applied when the event fires.
 * @param messageKey  Optional string-resource key of a predefined story text to display when the
 *                   event fires (selected via dropdown in the level editor). When null ("No
 *                   message" in the editor) no message popup is shown at all.
 * @param messageFrame Visual frame of the message popup (see [EventMessageFrameId]); only relevant
 *                    when [messageKey] is set. Null uses the standard story frame.
 * @param repeatable  When true the event can fire again on every future evaluation; when false
 *                   (default) it fires only once.
 * @param loop        Optional loop started when the event fires (e.g. alternating tides).
 */
data class LevelEvent(
    val id: String,
    val condition: EventCondition,
    val actions: List<EventAction> = emptyList(),
    val messageKey: String? = null,
    val messageFrame: EventMessageFrameId? = null,
    val repeatable: Boolean = false,
    val loop: EventLoop? = null,
) {
    /** All actions of this event, including those inside its loop. */
    fun allActions(): List<EventAction> = actions + (loop?.allActions() ?: emptyList())
}

/**
 * All scripted events defined for a level.
 */
data class LevelEvents(
    val events: List<LevelEvent> = emptyList(),
) {
    fun isEmpty(): Boolean = events.isEmpty()

    fun isNotEmpty(): Boolean = !isEmpty()
}
