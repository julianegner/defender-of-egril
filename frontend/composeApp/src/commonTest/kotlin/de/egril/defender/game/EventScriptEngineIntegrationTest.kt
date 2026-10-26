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
import de.egril.defender.model.GameState
import de.egril.defender.model.Level
import de.egril.defender.model.LevelEvent
import de.egril.defender.model.LevelEvents
import de.egril.defender.model.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Integration tests that verify scripted level events fire through the [GameEngine] turn cycle,
 * covering the "coins at or below" event reported as not firing.
 */
class EventScriptEngineIntegrationTest {
    @Test
    fun activatingRequiredAltarsWinsThroughEngineImmediately() {
        val level =
            coinsBelowLevel(threshold = 0, grant = 0, initialCoins = 100).copy(
                events =
                    LevelEvents(
                        listOf(
                            LevelEvent(
                                id = "rune-network",
                                condition = EventCondition(EventConditionType.ALTARS_ACTIVATED, threshold = 2),
                                actions = listOf(EventAction(EventActionType.WIN_LEVEL)),
                                messageKey = "event_msg_rune_network_taken_over",
                            ),
                        ),
                    ),
            )
        val state = GameState(level)
        val engine = GameEngine(state)
        engine.startFirstPlayerTurn()
        state.attackers.add(Attacker(id = 1, type = AttackerType.GOBLIN, position = mutableStateOf(Position(0, 3))))
        state.defenders.addAll(
            listOf(
                Defender(
                    id = 1,
                    type = DefenderType.ALTAR,
                    position = mutableStateOf(Position(2, 1)),
                    actionsRemaining = mutableStateOf(1),
                ),
                Defender(
                    id = 2,
                    type = DefenderType.ALTAR,
                    position = mutableStateOf(Position(2, 2)),
                    actionsRemaining = mutableStateOf(1),
                ),
            ),
        )
        assertTrue(engine.activateAltar(1))
        assertFalse(state.scriptedVictory.value)
        assertTrue(engine.activateAltar(2))
        assertTrue(state.isLevelWon())
        assertEquals("event_msg_rune_network_taken_over", state.pendingMessages.last().name)
        assertEquals(0, state.defenders[1].actionsRemaining.value)
    }

    private fun coinsBelowLevel(
        threshold: Int,
        grant: Int,
        initialCoins: Int,
    ): Level =
        Level(
            id = 1,
            name = "Test",
            gridWidth = 10,
            gridHeight = 6,
            startPositions = listOf(Position(0, 3)),
            targetPositions = listOf(Position(9, 3)),
            pathCells = (0..9).map { Position(it, 3) }.toSet(),
            buildAreas = setOf(Position(2, 1), Position(2, 2)),
            attackerWaves = emptyList(),
            initialCoins = initialCoins,
            healthPoints = 10,
            availableTowers = setOf(DefenderType.SPIKE_TOWER),
            events =
                LevelEvents(
                    listOf(
                        LevelEvent(
                            id = "lowcoins",
                            condition =
                                EventCondition(
                                    type = EventConditionType.COINS_AT_OR_BELOW,
                                    threshold = threshold,
                                ),
                            actions = listOf(EventAction(type = EventActionType.GIVE_COINS, amount = grant)),
                        ),
                    ),
                ),
        )

    @Test
    fun coinsAtOrBelowFiresImmediatelyWhenPlacingATowerDropsCoinsToThreshold() {
        val state = GameState(coinsBelowLevel(threshold = 100, grant = 50, initialCoins = 110))
        val engine = GameEngine(state)
        engine.startFirstPlayerTurn()

        // SPIKE_TOWER costs 10; placing it drops coins 110 -> 100 (<= 100), firing the event (+50).
        engine.placeDefender(DefenderType.SPIKE_TOWER, Position(2, 1))

        assertEquals(150, state.coins.value, "Coins-at-or-below event should fire immediately on the coin spend")
    }

    @Test
    fun coinsAtOrBelowFiresAtPlayerTurnStartWhenAlreadyBelow() {
        val state = GameState(coinsBelowLevel(threshold = 100, grant = 50, initialCoins = 80))
        val engine = GameEngine(state)

        engine.startFirstPlayerTurn()

        assertEquals(130, state.coins.value, "Coins-at-or-below event should fire at the first player turn start")
    }
}
