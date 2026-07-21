package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for snotling stacking behavior.
 * Snotlings can move onto tiles occupied by other snotlings; when they do,
 * their HP is combined into the existing snotling and the mover is removed.
 */
class SnotlingStackingTest {
    /** A small straight-path level for movement tests. */
    private fun createStraightLevel(): Level {
        val pathCells = (0 until 8).map { x -> Position(x, 0) }.toSet()
        return Level(
            id = 1,
            name = "Straight Test",
            gridWidth = 8,
            gridHeight = 3,
            startPositions = listOf(Position(0, 0)),
            targetPositions = listOf(Position(7, 0)),
            pathCells = pathCells,
            attackerWaves = emptyList(),
            initialCoins = 100,
            healthPoints = 10,
        )
    }

    @Test
    fun testSnotlingsMergeWhenOccupyingSameTile() {
        val level = createStraightLevel()
        val state = GameState(level)
        val engine = GameEngine(state)

        // snotling1 starts far back, snotling2 starts 4 tiles ahead but is slowed to 1 step/turn.
        // After one enemy turn, snotling1 (5 steps) catches up to snotling2 (1 step) on tile (5,0).
        val snotling1 =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.SNOTLING,
                position = mutableStateOf(Position(0, 0)),
                level = mutableStateOf(1),
            )
        val snotling2 =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.SNOTLING,
                position = mutableStateOf(Position(4, 0)),
                level = mutableStateOf(1),
            )
        // Slow snotling2 to effective speed 1 so snotling1 catches it at tile (5,0)
        snotling2.movementPenalty.value = 4
        state.attackers.addAll(listOf(snotling1, snotling2))

        // Both start with 5 HP each (default)
        assertEquals(5, snotling1.currentHealth.value)
        assertEquals(5, snotling2.currentHealth.value)

        // Run one enemy turn; snotlings move up to 5 tiles each towards target
        val movements = engine.calculateEnemyTurnMovements()
        for (step in movements.allMovementSteps) {
            for ((attackerId, newPos) in step) {
                engine.applyMovement(attackerId, newPos)
            }
        }

        // After movement, exactly one live snotling should remain
        val liveSnotlings = state.attackers.filter { it.type == AttackerType.SNOTLING && !it.isDefeated.value }
        assertEquals(1, liveSnotlings.size, "Exactly one live snotling should remain after merging")

        // The surviving snotling should have combined HP (5 + 5 = 10)
        assertEquals(10, liveSnotlings.first().currentHealth.value, "Merged snotling should have combined HP")
    }

    @Test
    fun testSnotlingCanMoveOntoSnotlingTile() {
        val level = createStraightLevel()
        val state = GameState(level)
        val engine = GameEngine(state)

        // Place a stationary snotling at position 5 and a moving one at position 0
        val stationary =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.SNOTLING,
                position = mutableStateOf(Position(5, 0)),
                level = mutableStateOf(1),
            )
        val moving =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.SNOTLING,
                position = mutableStateOf(Position(0, 0)),
                level = mutableStateOf(1),
            )
        // Give moving snotling reduced speed so we can observe partial movement
        // (snotlings normally have speed 5 - they'd pass the stationary in one turn)
        // Instead, test direct merge via applyMovement
        state.attackers.addAll(listOf(stationary, moving))

        val initialHp = stationary.currentHealth.value + moving.currentHealth.value

        // Directly apply movement of moving snotling to stationary's position
        engine.applyMovement(moving.id, stationary.position.value)

        // Moving snotling should be merged (defeated) and stationary should have combined HP
        assertTrue(moving.isDefeated.value, "Moving snotling should be defeated (merged)")
        assertEquals(
            initialHp,
            stationary.currentHealth.value,
            "Stationary snotling should absorb moving snotling's HP",
        )
    }

    @Test
    fun testSnotlingCannotMoveOntoNonSnotlingTile() {
        val level = createStraightLevel()
        val state = GameState(level)
        val engine = GameEngine(state)

        // Place a goblin at position 3 and a snotling at position 2
        val goblin =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.GOBLIN,
                position = mutableStateOf(Position(3, 0)),
                level = mutableStateOf(1),
            )
        val snotling =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.SNOTLING,
                position = mutableStateOf(Position(2, 0)),
                level = mutableStateOf(1),
            )
        state.attackers.addAll(listOf(goblin, snotling))

        val goblinHp = goblin.currentHealth.value
        val snotlingPos = snotling.position.value

        // Try to move the snotling onto the goblin's tile
        engine.applyMovement(snotling.id, goblin.position.value)

        // Snotling should NOT have moved (position unchanged)
        assertEquals(
            snotlingPos,
            snotling.position.value,
            "Snotling should not be able to move onto a goblin's tile",
        )
        // Goblin should be unaffected
        assertEquals(goblinHp, goblin.currentHealth.value, "Goblin HP should be unchanged")
        // Snotling should not be defeated
        assertTrue(!snotling.isDefeated.value, "Snotling should not be defeated")
    }
}
