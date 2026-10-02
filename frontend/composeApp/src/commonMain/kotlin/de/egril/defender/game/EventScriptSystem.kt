package de.egril.defender.game

import de.egril.defender.config.GameLogBuffer
import de.egril.defender.model.ActiveEventLoop
import de.egril.defender.model.DefenderType
import de.egril.defender.model.EventAction
import de.egril.defender.model.EventActionType
import de.egril.defender.model.EventCondition
import de.egril.defender.model.EventConditionType
import de.egril.defender.model.EventLoop
import de.egril.defender.model.EventLoopFrame
import de.egril.defender.model.EventLoopStep
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
    private val tileZoneSystem: TileZoneSystem = TileZoneSystem(state),
) {
    fun evaluate(trigger: EventTrigger) {
        // Sandbox levels have no scripted events.
        if (state.level.isSandbox) return
        val events = state.level.events.events
        if (events.isEmpty()) return

        // Loops advance once per player turn. Loops started during this evaluation begin waiting
        // from the next player turn on.
        if (trigger == EventTrigger.PLAYER_TURN_START) {
            advanceLoops()
        }

        for (event in events) {
            if (!event.repeatable && state.triggeredEventIds.contains(event.id)) continue
            if (state.turnNumber.value < event.condition.fromTurn) continue
            if (!conditionMet(event.condition, trigger)) continue

            fireEvent(event)
            if (!event.repeatable) {
                state.triggeredEventIds.add(event.id)
            }
        }

        if (trigger == EventTrigger.PLAYER_TURN_START) {
            tileZoneSystem.resurfaceSubmergedUnits()
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
        // A message popup is only shown when a predefined story text was selected. Events
        // configured with "No message" apply their effects silently. The applied actions are
        // carried on the message so the granted elements (coins, mana, supports, …) can be
        // displayed with symbols, names and amounts.
        val messageKey = event.messageKey
        if (messageKey != null) {
            state.pendingMessages.add(
                GameMessage(
                    type = GameMessageType.EVENT_MESSAGE,
                    name = messageKey,
                    eventActions = event.actions,
                    eventMessageFrame = event.messageFrame,
                ),
            )
        }

        val loop = event.loop
        if (loop != null && loop.steps.isNotEmpty() && state.activeEventLoops.none { it.eventId == event.id }) {
            if (!loop.isValid()) {
                GameLogBuffer.log("EVENT", "Loop of event '${event.id}' is invalid (no waiting turn per pass) and was not started")
                return
            }
            state.activeEventLoops.add(
                ActiveEventLoop(
                    eventId = event.id,
                    frames = listOf(EventLoopFrame(stepIndex = 0, turnsRemaining = loop.steps.first().waitTurns)),
                ),
            )
            runLoop(event.id)
        }
    }

    /** Count down the waiting step of every running loop and execute the steps that became due. */
    private fun advanceLoops() {
        for (active in state.activeEventLoops.toList()) {
            val index = state.activeEventLoops.indexOfFirst { it.eventId == active.eventId }
            if (index < 0) continue
            val frames = active.frames
            if (frames.isEmpty()) continue
            val top = frames.last()
            state.activeEventLoops[index] =
                active.copy(frames = frames.dropLast(1) + top.copy(turnsRemaining = top.turnsRemaining - 1))
            runLoop(active.eventId)
        }
    }

    /**
     * Execute all steps of the loop of [eventId] that are due, descending into nested loops and
     * advancing/finishing loops as needed, until the loop waits again or has finished.
     */
    private fun runLoop(eventId: String) {
        val rootLoop =
            state.level.events.events
                .firstOrNull { it.id == eventId }
                ?.loop
        var guard = 0
        while (true) {
            val index = state.activeEventLoops.indexOfFirst { it.eventId == eventId }
            if (index < 0) return
            val frames = state.activeEventLoops[index].frames.toMutableList()
            val loops = rootLoop?.let { resolveLoops(it, frames) }
            if (frames.isEmpty() || loops == null) {
                state.activeEventLoops.removeAt(index)
                return
            }
            if (++guard > MAX_LOOP_STEPS_PER_TURN) {
                GameLogBuffer.log("EVENT", "Loop of event '$eventId' exceeded $MAX_LOOP_STEPS_PER_TURN steps in one turn and was stopped")
                state.activeEventLoops.removeAt(index)
                return
            }

            val depth = frames.lastIndex
            val loop = loops[depth]
            val frame = frames[depth]
            val step = loop.steps[frame.stepIndex]

            if (!frame.stepExecuted) {
                if (frame.turnsRemaining > 0) return
                frames[depth] = frame.copy(stepExecuted = true)
                val nested = step.nestedLoop
                if (nested != null && nested.steps.isNotEmpty()) {
                    frames.add(EventLoopFrame(stepIndex = 0, turnsRemaining = nested.steps.first().waitTurns))
                }
                state.activeEventLoops[index] = ActiveEventLoop(eventId, frames.toList())
                executeLoopStep(eventId, step)
                continue
            }

            // The current step (including its nested loop) is complete: advance.
            var nextIndex = frame.stepIndex + 1
            var iterations = frame.iterationsDone
            if (nextIndex >= loop.steps.size) {
                iterations++
                if (!loop.isEndless && iterations >= loop.repeatCount) {
                    frames.removeAt(depth)
                    state.activeEventLoops[index] = ActiveEventLoop(eventId, frames.toList())
                    continue
                }
                nextIndex = 0
            }
            frames[depth] = EventLoopFrame(stepIndex = nextIndex, turnsRemaining = loop.steps[nextIndex].waitTurns, iterationsDone = iterations)
            state.activeEventLoops[index] = ActiveEventLoop(eventId, frames.toList())
        }
    }

    /**
     * The loop definition for every frame of [frames] (index 0 = [root]). Returns null when the
     * frames no longer match the level's loop definition (e.g. an outdated save game).
     */
    private fun resolveLoops(
        root: EventLoop,
        frames: List<EventLoopFrame>,
    ): List<EventLoop>? {
        val loops = mutableListOf<EventLoop>()
        var current: EventLoop? = root
        for ((depth, frame) in frames.withIndex()) {
            val loop = current ?: return null
            if (frame.stepIndex !in loop.steps.indices) return null
            loops.add(loop)
            current = if (depth < frames.lastIndex) loop.steps[frame.stepIndex].nestedLoop else null
        }
        return loops
    }

    private fun executeLoopStep(
        eventId: String,
        step: EventLoopStep,
    ) {
        GameLogBuffer.log(
            "EVENT",
            "Loop step of event '$eventId' executed on turn ${state.turnNumber.value} (${step.actions.size} action(s))",
        )
        for (action in step.actions) {
            applyAction(action)
        }
        // As for events themselves: loop steps without a selected message stay silent.
        val messageKey = step.messageKey
        if (messageKey != null) {
            state.pendingMessages.add(
                GameMessage(
                    type = GameMessageType.EVENT_MESSAGE,
                    name = messageKey,
                    eventActions = step.actions,
                    eventMessageFrame = step.messageFrame,
                ),
            )
        }
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
            EventActionType.APPLY_TILE_ZONE -> action.zoneId?.let { tileZoneSystem.applyZone(it) }
            EventActionType.REVERT_TILE_ZONE -> action.zoneId?.let { tileZoneSystem.revertZone(it) }
            EventActionType.TOGGLE_TILE_ZONE -> action.zoneId?.let { tileZoneSystem.toggleZone(it) }
            EventActionType.STOP_EVENT_LOOP -> {
                val target = action.targetEventId ?: return
                state.activeEventLoops.removeAll { it.eventId == target }
            }
            EventActionType.SHOW_MAP_IMAGE -> {
                val image = action.mapImage
                if (image == null || !image.isValid()) {
                    GameLogBuffer.log("EVENT", "Invalid map image in SHOW_MAP_IMAGE action")
                    return
                }
                val index = state.activeEventMapImages.indexOfFirst { it.id == image.id }
                if (index >= 0) {
                    state.activeEventMapImages[index] = image
                } else {
                    state.activeEventMapImages.add(image)
                }
            }
            EventActionType.HIDE_MAP_IMAGE -> {
                val id = action.imageId
                if (id.isNullOrBlank()) {
                    GameLogBuffer.log("EVENT", "Missing image id in HIDE_MAP_IMAGE action")
                    return
                }
                state.activeEventMapImages.removeAll { it.id == id }
            }
        }
    }

    companion object {
        /** Safety net against loops that would run forever within a single turn. */
        const val MAX_LOOP_STEPS_PER_TURN = 1000
    }
}
