package de.egril.defender.game

import de.egril.defender.config.GameLogBuffer
import de.egril.defender.model.DefenderType
import de.egril.defender.model.EventAction
import de.egril.defender.model.EventActionType
import de.egril.defender.model.EventCondition
import de.egril.defender.model.EventConditionType
import de.egril.defender.model.GameMessage
import de.egril.defender.model.GameMessageType
import de.egril.defender.model.GameState
import de.egril.defender.model.LevelEvent
import de.egril.defender.model.SpellType
import de.egril.defender.model.SupportObjectType
import de.egril.defender.model.combineSupportCounts

/**
 * When scripted-event evaluation is triggered during the turn cycle.
 */
enum class EventTrigger {
    /** Beginning of a player turn. */
    PLAYER_TURN_START,

    /** Beginning of an enemy turn. */
    ENEMY_TURN_START,

    /**
     * A mid-turn state change (enemy killed, coins/mana/health changed, unit moved). Used to fire
     * threshold- and position-based events immediately instead of waiting for the next turn start.
     * Turn-start events ([EventConditionType.TURN_START]/[EventConditionType.ENEMY_TURN_START]) do
     * not fire on this trigger.
     */
    IMMEDIATE,
}

/**
 * Evaluates and executes a level's scripted events (see [de.egril.defender.model.LevelEvent]).
 *
 * Events are evaluated at the beginning of each player turn and each enemy turn. When an event's
 * [EventCondition] is satisfied (and the current turn is at least [EventCondition.fromTurn]), its
 * [EventAction]s are applied and, if configured, a predefined story message is queued for display.
 * Non-repeatable events fire only once per level.
 */
class EventScriptSystem(
    private val state: GameState,
) {
    fun evaluate(trigger: EventTrigger) {
        // Sandbox levels have no scripted events.
        if (state.level.isSandbox) return
        val events = state.level.events.events
        if (events.isEmpty()) return

        for (event in events) {
            if (!event.repeatable && state.triggeredEventIds.contains(event.id)) continue
            if (state.turnNumber.value < event.condition.fromTurn) continue
            if (!conditionMet(event.condition, trigger)) continue

            fireEvent(event)
            if (!event.repeatable) {
                state.triggeredEventIds.add(event.id)
            }
        }
    }

    private fun conditionMet(
        condition: EventCondition,
        trigger: EventTrigger,
    ): Boolean =
        when (condition.type) {
            EventConditionType.TURN_START -> trigger == EventTrigger.PLAYER_TURN_START
            EventConditionType.ENEMY_TURN_START -> trigger == EventTrigger.ENEMY_TURN_START
            EventConditionType.ENEMIES_KILLED -> state.enemiesKilledTotal.value >= condition.threshold
            EventConditionType.ENEMY_TYPE_KILLED -> {
                val type = condition.attackerType
                type != null && (state.enemiesKilledByType[type] ?: 0) >= condition.threshold
            }
            EventConditionType.UNIT_REACHED -> {
                val position = condition.position
                position != null &&
                    state.attackers.any { attacker ->
                        !attacker.isDefeated.value &&
                            attacker.position.value == position &&
                            (condition.attackerType == null || attacker.type == condition.attackerType)
                    }
            }
            EventConditionType.HEALTH_AT_OR_BELOW -> state.healthPoints.value <= condition.threshold
            EventConditionType.MANA_AT_OR_BELOW -> state.currentMana.value <= condition.threshold
            EventConditionType.COINS_AT_OR_BELOW -> state.coins.value <= condition.threshold
        }

    private fun fireEvent(event: LevelEvent) {
        GameLogBuffer.log(
            "EVENT",
            "Scripted event '${event.id}' fired on turn ${state.turnNumber.value} " +
                "(${event.actions.size} action(s), message=${event.messageKey ?: "none"})",
        )
        for (action in event.actions) {
            applyAction(action)
        }
        // Always queue a message so the player is informed of the event's effects, even when no
        // predefined story text was selected. The applied actions are carried on the message so the
        // granted elements (coins, mana, supports, …) can be displayed with symbols, names and
        // amounts.
        state.pendingMessages.add(
            GameMessage(
                type = GameMessageType.EVENT_MESSAGE,
                name = event.messageKey,
                eventActions = event.actions,
            ),
        )
    }

    private fun applyAction(action: EventAction) {
        when (action.type) {
            EventActionType.GIVE_COINS -> {
                if (action.amount != 0) {
                    state.coins.value = (state.coins.value + action.amount).coerceAtLeast(0)
                }
            }
            EventActionType.GIVE_MANA -> {
                if (action.amount != 0) {
                    state.currentMana.value =
                        (state.currentMana.value + action.amount).coerceIn(0, state.maxMana.value)
                }
            }
            EventActionType.GIVE_SUPPORT_OBJECT -> {
                // Fall back to the first support object so events authored before a type was
                // persisted (the editor displayed the first entry as selected) still grant one.
                val type = action.supportObjectType ?: SupportObjectType.entries.first()
                val count = if (action.amount > 0) action.amount else 1
                state.supportObjectsRemaining[type] =
                    combineSupportCounts(state.supportObjectsRemaining[type] ?: 0, count)
            }
            EventActionType.GIVE_SUPPORT_SPELL -> {
                // Fall back to the first spell for events authored before a type was persisted.
                val spell = action.spellType ?: SpellType.entries.first()
                val count = if (action.amount > 0) action.amount else 1
                state.supportSpellsRemaining[spell] =
                    combineSupportCounts(state.supportSpellsRemaining[spell] ?: 0, count)
            }
            EventActionType.DESTROY_MINE -> {
                val position = action.position ?: return
                val mine =
                    state.defenders.firstOrNull {
                        it.type == DefenderType.DWARVEN_MINE &&
                            it.position.value == position &&
                            !state.destroyedMinePositions.contains(position)
                    } ?: return
                state.destroyedMinePositions.add(position)
                state.defenders.remove(mine)
            }
        }
    }
}
