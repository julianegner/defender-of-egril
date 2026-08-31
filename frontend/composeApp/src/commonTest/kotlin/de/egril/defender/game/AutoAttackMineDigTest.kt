package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.Attacker
import de.egril.defender.model.AttackerType
import de.egril.defender.model.Defender
import de.egril.defender.model.DefenderType
import de.egril.defender.model.GamePhase
import de.egril.defender.model.GameState
import de.egril.defender.model.Level
import de.egril.defender.model.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AutoAttackMineDigTest {
    private fun createLevel(): Level {
        val allCells = (0 until 10).flatMap { x -> (0 until 8).map { y -> Position(x, y) } }.toSet()
        return Level(
            id = 1,
            name = "Auto Mine Dig Test",
            gridWidth = 10,
            gridHeight = 8,
            startPositions = listOf(Position(0, 3)),
            targetPositions = listOf(Position(9, 3)),
            pathCells = allCells,
            attackerWaves = emptyList(),
            initialCoins = 0,
            healthPoints = 10,
        )
    }

    private fun mine(
        id: Int,
        position: Position,
        actions: Int = 1,
    ) = Defender(
        id = id,
        type = DefenderType.DWARVEN_MINE,
        position = mutableStateOf(position),
        actionsRemaining = mutableStateOf(actions),
        buildTimeRemaining = mutableStateOf(0),
    )

    private fun attacker(
        id: Int,
        position: Position,
    ) = Attacker(id, AttackerType.GOBLIN, mutableStateOf(position), mutableStateOf(1))

    @Test
    fun autoMineDigConsumesActionAndGeneratesCoins() {
        val state = GameState(createLevel(), phase = mutableStateOf(GamePhase.PLAYER_TURN))
        val engine = GameEngine(state)
        val dwarvenMine = mine(1, Position(5, 3))
        val enemy = attacker(1, Position(0, 3))

        state.defenders.add(dwarvenMine)
        state.attackers.add(enemy)

        val coinsBefore = state.coins.value
        engine.autoMineDig()

        assertEquals(0, dwarvenMine.actionsRemaining.value, "autoMineDig should consume the mine's action")
        assertTrue(state.coins.value >= coinsBefore, "autoMineDig should never decrease coin count")
    }

    @Test
    fun autoMineDigWorksWithoutActiveEnemies() {
        val state = GameState(createLevel(), phase = mutableStateOf(GamePhase.PLAYER_TURN))
        val engine = GameEngine(state)
        val dwarvenMine = mine(1, Position(5, 3))

        state.defenders.add(dwarvenMine)
        // No enemies on map

        engine.autoMineDig()

        assertEquals(0, dwarvenMine.actionsRemaining.value, "autoMineDig should work even without active enemies")
    }

    @Test
    fun autoMineDigDoesNothingWhenNoActionsRemaining() {
        val state = GameState(createLevel(), phase = mutableStateOf(GamePhase.PLAYER_TURN))
        val engine = GameEngine(state)
        val dwarvenMine = mine(1, Position(5, 3), actions = 0)
        state.defenders.add(dwarvenMine)

        val coinsBefore = state.coins.value
        engine.autoMineDig()

        assertEquals(0, dwarvenMine.actionsRemaining.value, "Actions remain 0 when no actions available")
        assertEquals(coinsBefore, state.coins.value, "No coins should be generated when mine has no actions")
    }

    @Test
    fun autoMineDigDoesNotReportMineAsSpecialActionAfterDigging() {
        val state = GameState(createLevel(), phase = mutableStateOf(GamePhase.PLAYER_TURN))
        val engine = GameEngine(state)
        val dwarvenMine = mine(1, Position(5, 3))
        val enemy = attacker(1, Position(0, 3))

        state.defenders.add(dwarvenMine)
        state.attackers.add(enemy)

        engine.autoMineDig()

        val specialActions = state.getDefenderTypesWithSpecialActions()
        assertTrue(
            specialActions.none { it == DefenderType.DWARVEN_MINE },
            "Mine should not appear as remaining special action after auto-dig",
        )
    }
}
