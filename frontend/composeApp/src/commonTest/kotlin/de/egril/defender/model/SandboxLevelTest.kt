package de.egril.defender.model

import androidx.compose.runtime.mutableStateOf
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for Sandbox level behaviour on [GameState]:
 *  - A sandbox level can never be won, even when all enemies are gone.
 *  - A sandbox level can still be lost by losing all health points.
 *  - The instant "Win Level now" is never offered for sandbox levels.
 */
class SandboxLevelTest {
    private fun buildLevel(
        isSandbox: Boolean,
        healthPoints: Int = 10,
        spawnPlan: List<PlannedEnemySpawn> = emptyList(),
    ): Level =
        Level(
            id = 1,
            name = "Sandbox Test",
            gridWidth = 10,
            gridHeight = 6,
            startPositions = listOf(Position(0, 2)),
            targetPositions = listOf(Position(9, 2)),
            pathCells = (0..9).map { Position(it, 2) }.toSet(),
            attackerWaves = emptyList(),
            directSpawnPlan = spawnPlan,
            healthPoints = healthPoints,
            isSandbox = isSandbox,
        )

    private fun playerTurnState(level: Level): GameState {
        val state = GameState(level)
        state.phase.value = GamePhase.PLAYER_TURN
        state.turnNumber.value = 5
        return state
    }

    @Test
    fun sandboxLevelIsNeverWonEvenWithNoEnemies() {
        val state = playerTurnState(buildLevel(isSandbox = true))
        // No enemies alive and nothing left to spawn: a normal level would be won here.
        assertFalse(state.isLevelWon())
    }

    @Test
    fun nonSandboxLevelIsWonWhenNoEnemiesRemain() {
        val state = playerTurnState(buildLevel(isSandbox = false))
        assertTrue(state.isLevelWon())
    }

    @Test
    fun sandboxLevelCanStillBeLostByLosingAllHealth() {
        val state = playerTurnState(buildLevel(isSandbox = true, healthPoints = 3))
        assertFalse(state.isLevelLost())
        state.healthPoints.value = 0
        assertTrue(state.isLevelLost())
    }

    @Test
    fun sandboxLevelNeverOffersInstantWin() {
        val state = playerTurnState(buildLevel(isSandbox = true, healthPoints = 100))
        state.attackers.add(
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.GOBLIN,
                position = mutableStateOf(Position(1, 2)),
            ),
        )
        assertFalse(state.canWinLevelNow())
    }
}
