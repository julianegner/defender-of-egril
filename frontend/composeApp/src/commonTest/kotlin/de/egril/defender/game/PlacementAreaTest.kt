package de.egril.defender.game

import de.egril.defender.editor.InitialData
import de.egril.defender.model.*
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for level placement areas: when [InitialData.placementAreas] is non-empty,
 * player placements (towers, barricades, traps) are only allowed inside that set.
 */
class PlacementAreaTest {
    private fun createLevel(placementAreas: List<Position>): Level {
        val pathCells = (0..6).map { Position(it, 0) }.toSet()
        val buildAreas = setOf(Position(2, 2), Position(3, 2), Position(4, 2))
        return Level(
            id = 1,
            name = "Placement Test",
            subtitle = "Test",
            gridWidth = 10,
            gridHeight = 10,
            startPositions = listOf(Position(0, 0)),
            targetPositions = listOf(Position(6, 0)),
            pathCells = pathCells,
            buildAreas = buildAreas,
            attackerWaves = emptyList(),
            initialCoins = 1000,
            healthPoints = 10,
            availableTowers = setOf(DefenderType.SPIKE_TOWER),
            initialData = InitialData(placementAreas = placementAreas),
        )
    }

    @Test
    fun testEmptyPlacementAreasAllowsEverywhere() {
        val level = createLevel(emptyList())
        assertTrue(level.isPlacementAllowed(Position(2, 2)))
        assertTrue(level.isPlacementAllowed(Position(0, 0)))
    }

    @Test
    fun testPlacementAreaRestrictsPositions() {
        val level = createLevel(listOf(Position(2, 2)))
        assertTrue(level.isPlacementAllowed(Position(2, 2)))
        assertFalse(level.isPlacementAllowed(Position(3, 2)))
        assertFalse(level.isPlacementAllowed(Position(0, 0)))
    }

    @Test
    fun testTowerCannotBePlacedOutsidePlacementArea() {
        val level = createLevel(listOf(Position(2, 2)))
        val state = GameState(level)
        val towerManager = TowerManager(state)
        assertFalse(towerManager.canPlaceDefenderAt(DefenderType.SPIKE_TOWER, Position(3, 2)))
        assertTrue(towerManager.canPlaceDefenderAt(DefenderType.SPIKE_TOWER, Position(2, 2)))
    }

    @Test
    fun testScriptedSupportTrapIgnoresPlacementArea() {
        val level = createLevel(listOf(Position(2, 2)))
        val state = GameState(level)
        val mineOperations = MineOperations(state)
        assertTrue(mineOperations.placeSupportTrap(Position(5, 0), 10, TrapType.DWARVEN))
    }
}
