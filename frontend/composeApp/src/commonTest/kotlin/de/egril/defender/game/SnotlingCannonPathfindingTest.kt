package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.Attacker
import de.egril.defender.model.AttackerType
import de.egril.defender.model.GameState
import de.egril.defender.model.Level
import de.egril.defender.model.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SnotlingCannonPathfindingTest {
    private fun makeAttacker(health: Int): Attacker =
        Attacker(
            id = 1,
            type = AttackerType.SNOTLING,
            position = mutableStateOf(Position(0, 1)),
            level = mutableStateOf(1),
            currentTarget = mutableStateOf(Position(8, 1)),
        ).also { it.currentHealth.value = health }

    private fun createLevel(): Level {
        val directLane = (0..8).filter { it != 2 && it != 5 }.map { Position(it, 1) }
        val detour = (0..8).map { Position(it, 2) }
        return Level(
            id = 1,
            name = "Snotling Cannon Pathfinding Test",
            gridWidth = 9,
            gridHeight = 4,
            startPositions = listOf(Position(0, 1)),
            targetPositions = listOf(Position(8, 1)),
            pathCells = (directLane + detour).toSet(),
            attackerWaves = emptyList(),
            initialCoins = 100,
            healthPoints = 10,
        )
    }

    @Test
    fun cannonCapableStackUsesShorterRouteAcrossMultipleNoPlayTiles() {
        val state = GameState(createLevel())
        val pathfinding = PathfindingSystem(state)
        val snotling = makeAttacker(150)

        val path = pathfinding.findPath(Position(0, 1), Position(8, 1), snotling)

        assertEquals(Position(8, 1), path.last())
        assertTrue(path.contains(Position(3, 1)), "The first jump should land on the path after the first NO_PLAY tile")
        assertTrue(path.contains(Position(6, 1)), "The second jump should land on the path after the second NO_PLAY tile")
        assertTrue(path.size < 9, "The cannon route should be shorter than the continuous detour")
    }

    @Test
    fun stackBelowCannonThresholdCannotUseNoPlayJump() {
        val level =
            Level(
                id = 1,
                name = "Snotling Cannon Threshold Test",
                gridWidth = 6,
                gridHeight = 3,
                startPositions = listOf(Position(0, 1)),
                targetPositions = listOf(Position(4, 1)),
                pathCells = setOf(Position(1, 1), Position(3, 1), Position(4, 1)),
                attackerWaves = emptyList(),
                initialCoins = 100,
                healthPoints = 10,
            )
        val pathfinding = PathfindingSystem(GameState(level))
        val snotling = makeAttacker(100)

        val path = pathfinding.findPath(Position(1, 1), Position(4, 1), snotling)

        assertFalse(path.contains(Position(3, 1)), "A stack below the cannon threshold must not jump the NO_PLAY tile")
        assertEquals(Position(1, 1), path.last(), "An unreachable target should not produce a false route")
    }
}
