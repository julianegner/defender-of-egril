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
}

/**
 * A single effect applied when a [LevelEvent] fires.
 *
 * @param type              The kind of effect.
 * @param amount            Amount (coins / mana / support count) depending on [type].
 * @param supportObjectType Support object granted for [EventActionType.GIVE_SUPPORT_OBJECT].
 * @param spellType         Spell granted for [EventActionType.GIVE_SUPPORT_SPELL].
 * @param position          Mine tile for [EventActionType.DESTROY_MINE].
 */
data class EventAction(
    val type: EventActionType,
    val amount: Int = 0,
    val supportObjectType: SupportObjectType? = null,
    val spellType: SpellType? = null,
    val position: Position? = null,
)

/**
 * A scripted event: a condition, the effects it applies, and an optional predefined story message.
 *
 * @param id          Unique identifier within the level (used to track whether it already fired).
 * @param condition   Condition that triggers the event.
 * @param actions     Effects applied when the event fires.
 * @param messageKey  Optional string-resource key of a predefined story text to display when the
 *                   event fires (selected via dropdown in the level editor).
 * @param repeatable  When true the event can fire again on every future evaluation; when false
 *                   (default) it fires only once.
 */
data class LevelEvent(
    val id: String,
    val condition: EventCondition,
    val actions: List<EventAction> = emptyList(),
    val messageKey: String? = null,
    val repeatable: Boolean = false,
)

/**
 * All scripted events defined for a level.
 */
data class LevelEvents(
    val events: List<LevelEvent> = emptyList(),
) {
    fun isEmpty(): Boolean = events.isEmpty()

    fun isNotEmpty(): Boolean = !isEmpty()
}
