package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.DefenderType
import de.egril.defender.model.FiefType
import de.egril.defender.model.GameState
import de.egril.defender.model.Gargoyle
import de.egril.defender.model.GargoyleRules
import de.egril.defender.model.Level
import de.egril.defender.model.Position
import de.egril.defender.model.TrapType
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GargoylePlacementBlockingTest {
    private val gargoyleTile = Position(5, 1)
    private val pathTile = Position(3, 0)

    private fun createState(): GameState {
        val level =
            Level(
                id = 1,
                name = "Gargoyle Placement Test",
                gridWidth = 12,
                gridHeight = 3,
                startPositions = listOf(Position(0, 0)),
                targetPositions = listOf(Position(10, 0)),
                pathCells = (0..10).map { Position(it, 0) }.toSet(),
                buildAreas = setOf(gargoyleTile),
                attackerWaves = emptyList(),
                initialCoins = 500,
                healthPoints = 10,
                availableTowers = setOf(DefenderType.SPIKE_TOWER),
            )
        return GameState(level = level)
    }

    private fun addInactiveGargoyle(state: GameState, position: Position) {
        state.gargoyles.add(
            Gargoyle(
                id = state.nextGargoyleId.value++,
                position = mutableStateOf(position),
                level = mutableStateOf(1),
                currentHealth = mutableStateOf(GargoyleRules.HEALTH_PER_LEVEL),
            ),
        )
    }

    @Test
    fun towerCannotBePlacedOnInactiveGargoyleTile() {
        val freeState = createState()
        assertTrue(GameEngine(freeState).canPlaceDefenderAt(DefenderType.SPIKE_TOWER, gargoyleTile))

        val state = createState()
        addInactiveGargoyle(state, gargoyleTile)
        assertFalse(GameEngine(state).canPlaceDefenderAt(DefenderType.SPIKE_TOWER, gargoyleTile))
    }

    @Test
    fun barricadeCannotBePlacedOnInactiveGargoyleTile() {
        val freeState = createState()
        assertTrue(GameEngine(freeState).placeSupportBarricade(pathTile, 100))

        val state = createState()
        addInactiveGargoyle(state, pathTile)
        assertFalse(GameEngine(state).placeSupportBarricade(pathTile, 100))
    }

    @Test
    fun trapCannotBePlacedOnInactiveGargoyleTile() {
        val freeState = createState()
        assertTrue(GameEngine(freeState).placeSupportTrap(pathTile, 5, TrapType.DWARVEN))

        val state = createState()
        addInactiveGargoyle(state, pathTile)
        assertFalse(GameEngine(state).placeSupportTrap(pathTile, 5, TrapType.DWARVEN))
    }

    @Test
    fun fiefCannotBePlacedOnInactiveGargoyleTile() {
        val freeState = createState()
        assertTrue(GameEngine(freeState).placeSupportFief(pathTile, FiefType.WOODCUTTER))

        val state = createState()
        addInactiveGargoyle(state, pathTile)
        assertFalse(GameEngine(state).placeSupportFief(pathTile, FiefType.WOODCUTTER))
    }
}
