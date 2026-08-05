package de.egril.defender.game

import de.egril.defender.model.AttackerType
import de.egril.defender.model.GameState
import de.egril.defender.model.Level
import de.egril.defender.model.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for [GameEngine.spawnEnemy] honoring an optional preferred spawn point, used by the sandbox
 * tools to let the player choose where a test enemy enters the map.
 */
class SpawnEnemyAtSpawnPointTest {
    // Two separate horizontal lanes, each with its own spawn point, so the chosen spawn point is
    // unambiguous.
    private fun twoLaneLevel(): Level {
        val topLane = (0..5).map { Position(it, 0) }
        val bottomLane = (0..5).map { Position(it, 4) }
        return Level(
            id = 1,
            name = "Two Lane",
            gridWidth = 6,
            gridHeight = 5,
            startPositions = listOf(Position(0, 0), Position(0, 4)),
            targetPositions = listOf(Position(5, 0), Position(5, 4)),
            pathCells = (topLane + bottomLane).toSet(),
            attackerWaves = emptyList(),
        )
    }

    @Test
    fun spawnEnemyUsesRequestedSpawnPoint() {
        val state = GameState(level = twoLaneLevel())
        val engine = GameEngine(state)

        engine.spawnEnemy(AttackerType.ORK, level = 1, preferredSpawnPoint = Position(0, 4))

        assertEquals(1, state.attackers.size)
        assertEquals(
            Position(0, 4),
            state.attackers
                .first()
                .position.value,
        )
    }

    @Test
    fun spawnEnemyWithoutRequestUsesAFreeSpawnPoint() {
        val state = GameState(level = twoLaneLevel())
        val engine = GameEngine(state)

        engine.spawnEnemy(AttackerType.ORK, level = 1)

        assertEquals(1, state.attackers.size)
        assertTrue(
            state.attackers
                .first()
                .position.value in state.level.startPositions,
        )
    }
}
