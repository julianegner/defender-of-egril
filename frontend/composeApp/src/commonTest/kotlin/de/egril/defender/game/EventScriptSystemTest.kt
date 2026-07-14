package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.Attacker
import de.egril.defender.model.AttackerType
import de.egril.defender.model.Defender
import de.egril.defender.model.DefenderType
import de.egril.defender.model.EventAction
import de.egril.defender.model.EventActionType
import de.egril.defender.model.EventCondition
import de.egril.defender.model.EventConditionType
import de.egril.defender.model.GameMessageType
import de.egril.defender.model.GameState
import de.egril.defender.model.Level
import de.egril.defender.model.LevelEvent
import de.egril.defender.model.LevelEvents
import de.egril.defender.model.Position
import de.egril.defender.model.SpellType
import de.egril.defender.model.SupportObjectType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for [EventScriptSystem]: scripted level events (conditions, actions, story messages).
 */
class EventScriptSystemTest {
    private fun createLevel(events: LevelEvents): Level =
        Level(
            id = 1,
            name = "Test Level",
            subtitle = "Test",
            gridWidth = 10,
            gridHeight = 10,
            startPositions = listOf(Position(0, 0)),
            targetPositions = listOf(Position(5, 0)),
            pathCells = (0..5).map { Position(it, 0) }.toSet(),
            buildAreas = setOf(Position(2, 2), Position(3, 2)),
            attackerWaves = emptyList(),
            initialCoins = 100,
            healthPoints = 10,
            availableTowers = setOf(DefenderType.SPIKE_TOWER),
            events = events,
        )

    @Test
    fun testTurnStartConditionFires() {
        val event =
            LevelEvent(
                id = "e1",
                condition = EventCondition(type = EventConditionType.TURN_START),
                actions = listOf(EventAction(type = EventActionType.GIVE_COINS, amount = 50)),
            )
        val state = GameState(createLevel(LevelEvents(listOf(event))))
        state.turnNumber.value = 1
        val system = EventScriptSystem(state)

        // Enemy trigger should NOT fire a player-turn-start event.
        system.evaluate(EventTrigger.ENEMY_TURN_START)
        assertEquals(100, state.coins.value, "Enemy turn should not fire a TURN_START event")

        system.evaluate(EventTrigger.PLAYER_TURN_START)
        assertEquals(150, state.coins.value, "Player turn start should grant coins")
    }

    @Test
    fun testNonRepeatableEventFiresOnce() {
        val event =
            LevelEvent(
                id = "once",
                condition = EventCondition(type = EventConditionType.TURN_START),
                actions = listOf(EventAction(type = EventActionType.GIVE_COINS, amount = 10)),
                repeatable = false,
            )
        val state = GameState(createLevel(LevelEvents(listOf(event))))
        state.turnNumber.value = 1
        val system = EventScriptSystem(state)

        system.evaluate(EventTrigger.PLAYER_TURN_START)
        system.evaluate(EventTrigger.PLAYER_TURN_START)
        assertEquals(110, state.coins.value, "Non-repeatable event should fire only once")
        assertTrue(state.triggeredEventIds.contains("once"))
    }

    @Test
    fun testRepeatableEventFiresEachTime() {
        val event =
            LevelEvent(
                id = "rep",
                condition = EventCondition(type = EventConditionType.TURN_START),
                actions = listOf(EventAction(type = EventActionType.GIVE_COINS, amount = 10)),
                repeatable = true,
            )
        val state = GameState(createLevel(LevelEvents(listOf(event))))
        state.turnNumber.value = 1
        val system = EventScriptSystem(state)

        system.evaluate(EventTrigger.PLAYER_TURN_START)
        system.evaluate(EventTrigger.PLAYER_TURN_START)
        assertEquals(120, state.coins.value, "Repeatable event should fire every time")
    }

    @Test
    fun testFromTurnGate() {
        val event =
            LevelEvent(
                id = "gated",
                condition = EventCondition(type = EventConditionType.COINS_AT_OR_BELOW, fromTurn = 5, threshold = 50),
                actions = listOf(EventAction(type = EventActionType.GIVE_COINS, amount = 50)),
            )
        val state = GameState(createLevel(LevelEvents(listOf(event))))
        state.coins.value = 20
        val system = EventScriptSystem(state)

        state.turnNumber.value = 3
        system.evaluate(EventTrigger.PLAYER_TURN_START)
        assertEquals(20, state.coins.value, "Event should not fire before its fromTurn gate")

        state.turnNumber.value = 5
        system.evaluate(EventTrigger.PLAYER_TURN_START)
        assertEquals(70, state.coins.value, "Event should fire once the fromTurn gate is reached")
    }

    @Test
    fun testEnemiesKilledCondition() {
        val event =
            LevelEvent(
                id = "kills",
                condition = EventCondition(type = EventConditionType.ENEMIES_KILLED, threshold = 3),
                actions = listOf(EventAction(type = EventActionType.GIVE_MANA, amount = 5)),
            )
        val state = GameState(createLevel(LevelEvents(listOf(event))))
        state.turnNumber.value = 1
        state.maxMana.value = 100
        val system = EventScriptSystem(state)

        state.enemiesKilledTotal.value = 2
        system.evaluate(EventTrigger.PLAYER_TURN_START)
        assertEquals(0, state.currentMana.value, "Event should not fire below the kill threshold")

        state.enemiesKilledTotal.value = 3
        system.evaluate(EventTrigger.PLAYER_TURN_START)
        assertEquals(5, state.currentMana.value, "Event should fire once the kill threshold is met")
    }

    @Test
    fun testEnemyTypeKilledCondition() {
        val event =
            LevelEvent(
                id = "orkkills",
                condition =
                    EventCondition(
                        type = EventConditionType.ENEMY_TYPE_KILLED,
                        threshold = 2,
                        attackerType = AttackerType.ORK,
                    ),
                actions = listOf(EventAction(type = EventActionType.GIVE_COINS, amount = 25)),
            )
        val state = GameState(createLevel(LevelEvents(listOf(event))))
        state.turnNumber.value = 1
        val system = EventScriptSystem(state)

        state.enemiesKilledByType[AttackerType.GOBLIN] = 5
        system.evaluate(EventTrigger.PLAYER_TURN_START)
        assertEquals(100, state.coins.value, "Kills of a different type should not count")

        state.enemiesKilledByType[AttackerType.ORK] = 2
        system.evaluate(EventTrigger.PLAYER_TURN_START)
        assertEquals(125, state.coins.value, "Reaching the type-specific kill threshold should fire the event")
    }

    @Test
    fun testUnitReachedCondition() {
        val target = Position(3, 0)
        val event =
            LevelEvent(
                id = "reached",
                condition = EventCondition(type = EventConditionType.UNIT_REACHED, position = target),
                actions = listOf(EventAction(type = EventActionType.GIVE_COINS, amount = 30)),
            )
        val state = GameState(createLevel(LevelEvents(listOf(event))))
        state.turnNumber.value = 1
        val system = EventScriptSystem(state)

        val attacker = Attacker(id = 1, type = AttackerType.GOBLIN, position = mutableStateOf(Position(1, 0)))
        state.attackers.add(attacker)

        system.evaluate(EventTrigger.PLAYER_TURN_START)
        assertEquals(100, state.coins.value, "Event should not fire while no unit is on the target tile")

        attacker.position.value = target
        system.evaluate(EventTrigger.PLAYER_TURN_START)
        assertEquals(130, state.coins.value, "Event should fire when a unit reaches the target tile")
    }

    @Test
    fun testUnitReachedConditionIsTypeSpecificWhenTypeSet() {
        val target = Position(3, 0)
        val event =
            LevelEvent(
                id = "reached_ork",
                condition =
                    EventCondition(
                        type = EventConditionType.UNIT_REACHED,
                        position = target,
                        attackerType = AttackerType.ORK,
                    ),
                actions = listOf(EventAction(type = EventActionType.GIVE_COINS, amount = 30)),
            )
        val state = GameState(createLevel(LevelEvents(listOf(event))))
        state.turnNumber.value = 1
        val system = EventScriptSystem(state)

        // A goblin on the target tile must NOT fire an ork-specific UNIT_REACHED event.
        val goblin = Attacker(id = 1, type = AttackerType.GOBLIN, position = mutableStateOf(target))
        state.attackers.add(goblin)
        system.evaluate(EventTrigger.PLAYER_TURN_START)
        assertEquals(100, state.coins.value, "A different unit type on the tile should not fire a type-specific event")

        // An ork on the target tile fires it.
        val ork = Attacker(id = 2, type = AttackerType.ORK, position = mutableStateOf(target))
        state.attackers.add(ork)
        system.evaluate(EventTrigger.PLAYER_TURN_START)
        assertEquals(130, state.coins.value, "The specified unit type reaching the tile should fire the event")
    }

    @Test
    fun testGiveSupportActions() {
        val event =
            LevelEvent(
                id = "support",
                condition = EventCondition(type = EventConditionType.TURN_START),
                actions =
                    listOf(
                        EventAction(
                            type = EventActionType.GIVE_SUPPORT_OBJECT,
                            amount = 2,
                            supportObjectType = SupportObjectType.BARRICADE,
                        ),
                        EventAction(
                            type = EventActionType.GIVE_SUPPORT_SPELL,
                            amount = 1,
                            spellType = SpellType.FREEZE_SPELL,
                        ),
                    ),
            )
        val state = GameState(createLevel(LevelEvents(listOf(event))))
        state.turnNumber.value = 1
        val system = EventScriptSystem(state)

        system.evaluate(EventTrigger.PLAYER_TURN_START)
        assertEquals(2, state.supportObjectsRemaining[SupportObjectType.BARRICADE])
        assertEquals(1, state.supportSpellsRemaining[SpellType.FREEZE_SPELL])
    }

    @Test
    fun testDestroyMineAction() {
        val minePos = Position(2, 2)
        val event =
            LevelEvent(
                id = "destroy",
                condition = EventCondition(type = EventConditionType.TURN_START),
                actions = listOf(EventAction(type = EventActionType.DESTROY_MINE, position = minePos)),
            )
        val state = GameState(createLevel(LevelEvents(listOf(event))))
        state.turnNumber.value = 1
        val mine = Defender(id = 1, type = DefenderType.DWARVEN_MINE, position = mutableStateOf(minePos))
        state.defenders.add(mine)
        val system = EventScriptSystem(state)

        system.evaluate(EventTrigger.PLAYER_TURN_START)
        assertFalse(state.defenders.contains(mine), "Mine should be removed")
        assertTrue(state.destroyedMinePositions.contains(minePos), "Destroyed mine position should be recorded")
    }

    @Test
    fun testGiveSupportActionsFallBackToFirstEntryWhenTypeMissing() {
        // Events authored before a type was persisted (null type) should still grant a support,
        // matching the first entry the editor displayed as selected.
        val event =
            LevelEvent(
                id = "support_no_type",
                condition = EventCondition(type = EventConditionType.TURN_START),
                actions =
                    listOf(
                        EventAction(type = EventActionType.GIVE_SUPPORT_OBJECT, amount = 1),
                        EventAction(type = EventActionType.GIVE_SUPPORT_SPELL, amount = 1),
                    ),
            )
        val state = GameState(createLevel(LevelEvents(listOf(event))))
        state.turnNumber.value = 1
        val system = EventScriptSystem(state)

        system.evaluate(EventTrigger.PLAYER_TURN_START)
        assertEquals(1, state.supportObjectsRemaining[SupportObjectType.entries.first()])
        assertEquals(1, state.supportSpellsRemaining[SpellType.entries.first()])
    }

    @Test
    fun testMessageIsQueued() {
        val event =
            LevelEvent(
                id = "msg",
                condition = EventCondition(type = EventConditionType.TURN_START),
                messageKey = "event_msg_reinforcements",
            )
        val state = GameState(createLevel(LevelEvents(listOf(event))))
        state.turnNumber.value = 1
        val system = EventScriptSystem(state)

        system.evaluate(EventTrigger.PLAYER_TURN_START)
        assertEquals(1, state.pendingMessages.size, "A message should be queued")
        assertEquals(GameMessageType.EVENT_MESSAGE, state.pendingMessages.first().type)
        assertEquals("event_msg_reinforcements", state.pendingMessages.first().name)
    }

    @Test
    fun testImmediateTriggerFiresThresholdConditions() {
        val killEvent =
            LevelEvent(
                id = "kills",
                condition = EventCondition(type = EventConditionType.ENEMIES_KILLED, threshold = 5),
                actions = listOf(EventAction(type = EventActionType.GIVE_COINS, amount = 50)),
            )
        val coinsEvent =
            LevelEvent(
                id = "lowcoins",
                condition = EventCondition(type = EventConditionType.COINS_AT_OR_BELOW, threshold = 30),
                actions = listOf(EventAction(type = EventActionType.GIVE_MANA, amount = 5)),
            )
        val state = GameState(createLevel(LevelEvents(listOf(coinsEvent, killEvent))))
        state.turnNumber.value = 1
        state.coins.value = 20
        state.maxMana.value = 100
        val system = EventScriptSystem(state)

        // Threshold-based events fire mid-turn on the IMMEDIATE trigger.
        state.enemiesKilledTotal.value = 5
        system.evaluate(EventTrigger.IMMEDIATE)
        assertEquals(70, state.coins.value, "Enemies-killed event should fire immediately")
        assertEquals(5, state.currentMana.value, "Coins-at-or-below event should fire immediately")
    }

    @Test
    fun testImmediateTriggerDoesNotFireTurnStartConditions() {
        val event =
            LevelEvent(
                id = "turnstart",
                condition = EventCondition(type = EventConditionType.TURN_START),
                actions = listOf(EventAction(type = EventActionType.GIVE_COINS, amount = 50)),
            )
        val state = GameState(createLevel(LevelEvents(listOf(event))))
        state.turnNumber.value = 1
        val system = EventScriptSystem(state)

        system.evaluate(EventTrigger.IMMEDIATE)
        assertEquals(100, state.coins.value, "Turn-start events must not fire on the IMMEDIATE trigger")
    }

    @Test
    fun testImmediateTriggerDoesNotFireEnemyTurnStartConditions() {
        val event =
            LevelEvent(
                id = "enemyturnstart",
                condition = EventCondition(type = EventConditionType.ENEMY_TURN_START),
                actions = listOf(EventAction(type = EventActionType.GIVE_COINS, amount = 50)),
            )
        val state = GameState(createLevel(LevelEvents(listOf(event))))
        state.turnNumber.value = 1
        val system = EventScriptSystem(state)

        system.evaluate(EventTrigger.IMMEDIATE)
        assertEquals(100, state.coins.value, "Enemy-turn-start events must not fire on the IMMEDIATE trigger")
    }
}
