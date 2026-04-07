package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.*
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests that the level ends immediately when the last enemy is killed by a spell,
 * without requiring the player to click "Next Turn".
 *
 * The relevant check is in GameState.isLevelWon(), which is called from
 * GameViewModel.castSpell() after processing defeated attackers.
 */
class SpellKillLevelEndTest {

    @Test
    fun testIsLevelWonWhenLastEnemyKilledBySpell() {
        // Set up a level where all enemies have already spawned (spawnTurn 1)
        val level = createTestLevel()
        val state = GameState(level = level)

        // Advance to turn 1 so all spawns at turn 1 are considered done
        state.turnNumber.value = 1

        // Add one enemy (simulating what is present on the map)
        val goblin = Attacker(
            id = 1,
            type = AttackerType.GOBLIN,
            position = mutableStateOf(Position(1, 1)),
            level = mutableStateOf(1)
        )
        state.attackers.add(goblin)

        // Level should not be won yet while the enemy is alive
        assertFalse(state.isLevelWon(), "Level should not be won while enemy is still alive")

        // Simulate the spell killing the last enemy (sets isDefeated)
        goblin.currentHealth.value = 0
        goblin.isDefeated.value = true

        // After the spell kills the last enemy, the level should immediately be won
        assertTrue(state.isLevelWon(), "Level should be won immediately after last enemy is killed by spell")
    }

    @Test
    fun testIsNotLevelWonWhenFutureSpawnsRemain() {
        // Set up a level where there are enemies spawning in the future (turn 5)
        val level = createTestLevelWithFutureSpawn()
        val state = GameState(level = level)

        // Current turn is 1 — there are spawns at turn 5 that haven't happened yet
        state.turnNumber.value = 1

        // No active attackers on the map (none have spawned yet)
        // Level should not be won because not all spawns have occurred
        assertFalse(
            state.isLevelWon(),
            "Level should not be won while there are future enemy spawns remaining"
        )
    }

    private fun createTestLevel(): Level {
        return Level(
            id = 1,
            name = "Test Level",
            subtitle = "",
            gridWidth = 10,
            gridHeight = 10,
            startPositions = listOf(Position(0, 0)),
            targetPositions = listOf(Position(9, 9)),
            pathCells = (0..9).flatMap { x -> (0..9).map { y -> Position(x, y) } }.toSet(),
            // One enemy spawns at turn 1
            directSpawnPlan = listOf(
                PlannedEnemySpawn(attackerType = AttackerType.GOBLIN, spawnTurn = 1)
            ),
            attackerWaves = emptyList(),
            initialCoins = 100,
            healthPoints = 10,
            availableTowers = emptySet()
        )
    }

    private fun createTestLevelWithFutureSpawn(): Level {
        return Level(
            id = 2,
            name = "Test Level Future Spawn",
            subtitle = "",
            gridWidth = 10,
            gridHeight = 10,
            startPositions = listOf(Position(0, 0)),
            targetPositions = listOf(Position(9, 9)),
            pathCells = (0..9).flatMap { x -> (0..9).map { y -> Position(x, y) } }.toSet(),
            // One enemy spawns at turn 5 (in the future)
            directSpawnPlan = listOf(
                PlannedEnemySpawn(attackerType = AttackerType.GOBLIN, spawnTurn = 5)
            ),
            attackerWaves = emptyList(),
            initialCoins = 100,
            healthPoints = 10,
            availableTowers = emptySet()
        )
    }
}
