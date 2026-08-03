package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.*
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DangerHintTest {
    private fun state(pathLength: Int): GameState {
        val path = (0 until pathLength).map { Position(it, 1) }.toSet()
        return GameState(
            Level(
                id = 1,
                name = "Danger hint",
                gridWidth = pathLength + 1,
                gridHeight = 3,
                startPositions = listOf(Position(0, 1)),
                targetPositions = listOf(Position(pathLength, 1)),
                pathCells = path,
                attackerWaves = emptyList(),
            ),
        )
    }

    @Test
    fun marksEnemyThatReachesTargetNextTurn() {
        val gameState = state(pathLength = 2)
        val attacker =
            Attacker(
                id = 1,
                type = AttackerType.GOBLIN,
                position = mutableStateOf(Position(0, 1)),
                level = mutableStateOf(1),
            )
        gameState.attackers.add(attacker)

        assertTrue(EnemyMovementSystem(gameState, PathfindingSystem(gameState)).canReachTargetNextTurn(attacker))
    }

    @Test
    fun doesNotMarkEnemyTooFarAway() {
        val gameState = state(pathLength = 8)
        val attacker =
            Attacker(
                id = 1,
                type = AttackerType.GOBLIN,
                position = mutableStateOf(Position(0, 1)),
                level = mutableStateOf(1),
            )
        gameState.attackers.add(attacker)

        assertFalse(EnemyMovementSystem(gameState, PathfindingSystem(gameState)).canReachTargetNextTurn(attacker))
    }
}
