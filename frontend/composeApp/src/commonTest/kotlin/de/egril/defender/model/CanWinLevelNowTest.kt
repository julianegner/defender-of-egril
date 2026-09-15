package de.egril.defender.model

import androidx.compose.runtime.mutableStateOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for the "finish level fast" feature ([GameState.canWinLevelNow]).
 *
 * A level can be won instantly when the worst-case damage of every remaining enemy (alive plus
 * still to spawn) is strictly less than the player's current health points.
 */
class CanWinLevelNowTest {
    private fun buildLevel(
        healthPoints: Int = 10,
        spawnPlan: List<PlannedEnemySpawn> = emptyList(),
        targetInfoMap: Map<Position, TargetInfo> = emptyMap(),
    ): Level =
        Level(
            id = 1,
            name = "Win Now Test",
            gridWidth = 10,
            gridHeight = 6,
            startPositions = listOf(Position(0, 2)),
            targetPositions = listOf(Position(9, 2)),
            pathCells = (0..9).map { Position(it, 2) }.toSet(),
            attackerWaves = emptyList(),
            directSpawnPlan = spawnPlan,
            healthPoints = healthPoints,
            targetInfoMap = targetInfoMap,
        )

    private fun playerTurnState(level: Level): GameState {
        val state = GameState(level)
        state.phase.value = GamePhase.PLAYER_TURN
        state.turnNumber.value = 5
        return state
    }

    private fun addAttacker(
        state: GameState,
        type: AttackerType,
        level: Int = 1,
    ) {
        state.attackers.add(
            Attacker(
                id = state.nextAttackerId.value++,
                type = type,
                position = mutableStateOf(Position(1, 2)),
                level = mutableStateOf(level),
            ),
        )
    }

    @Test
    fun winWhenWeakEnemiesCannotDepleteHealth() {
        val state = playerTurnState(buildLevel(healthPoints = 10))
        // Two goblins deal 1 HP each = 2 total, less than 10 HP
        addAttacker(state, AttackerType.GOBLIN)
        addAttacker(state, AttackerType.GOBLIN)
        assertTrue(state.canWinLevelNow())
        assertEquals(2L, state.getRemainingEnemyThreat())
    }

    @Test
    fun noWinWhenThreatEqualsHealth() {
        val state = playerTurnState(buildLevel(healthPoints = 2))
        addAttacker(state, AttackerType.GOBLIN)
        addAttacker(state, AttackerType.GOBLIN)
        // Threat 2 == HP 2 -> not guaranteed
        assertFalse(state.canWinLevelNow())
    }

    @Test
    fun includesEnemiesYetToSpawn() {
        val spawnPlan =
            listOf(
                PlannedEnemySpawn(AttackerType.GOBLIN, spawnTurn = 20),
                PlannedEnemySpawn(AttackerType.ORK, spawnTurn = 21),
            )
        val state = playerTurnState(buildLevel(healthPoints = 10, spawnPlan = spawnPlan))
        addAttacker(state, AttackerType.GOBLIN)
        // 1 (alive goblin) + 1 (goblin) + 1 (ork) = 3 < 10
        assertEquals(3L, state.getRemainingEnemyThreat())
        assertTrue(state.canWinLevelNow())
    }

    @Test
    fun noWinWhenSummonerRemains() {
        // Evil wizard can summon additional enemies, so a win cannot be guaranteed even with high HP.
        val state = playerTurnState(buildLevel(healthPoints = 100))
        addAttacker(state, AttackerType.EVIL_WIZARD, level = 1)
        assertFalse(state.canWinLevelNow())
    }

    @Test
    fun noWinWhenSummonerYetToSpawn() {
        val spawnPlan = listOf(PlannedEnemySpawn(AttackerType.EWHAD, spawnTurn = 30))
        val state = playerTurnState(buildLevel(healthPoints = 1000, spawnPlan = spawnPlan))
        addAttacker(state, AttackerType.GOBLIN)
        assertFalse(state.canWinLevelNow())
    }

    @Test
    fun noWinWhenVillainAlive() {
        // A villain reaching a target loses the level outright, so no guaranteed win can be offered
        // while one is on the battlefield, regardless of how much health remains.
        val state = playerTurnState(buildLevel(healthPoints = 1000))
        addAttacker(state, AttackerType.GAROKK, level = 1)
        assertFalse(state.canWinLevelNow())
    }

    @Test
    fun noWinWhenVillainYetToSpawn() {
        val spawnPlan = listOf(PlannedEnemySpawn(AttackerType.GAROKK, spawnTurn = 30))
        val state = playerTurnState(buildLevel(healthPoints = 1000, spawnPlan = spawnPlan))
        addAttacker(state, AttackerType.GOBLIN)
        assertFalse(state.canWinLevelNow())
    }

    @Test
    fun noWinDuringBuildingPhase() {
        val level = buildLevel(healthPoints = 10)
        val state = GameState(level)
        // Default phase is INITIAL_BUILDING
        addAttacker(state, AttackerType.GOBLIN)
        assertFalse(state.canWinLevelNow())
    }

    @Test
    fun noWinWithSingleHitTargets() {
        val targetInfoMap =
            mapOf(Position(9, 2) to TargetInfo(name = "Gate", type = TargetType.SINGLE_HIT))
        val state = playerTurnState(buildLevel(healthPoints = 10, targetInfoMap = targetInfoMap))
        addAttacker(state, AttackerType.GOBLIN)
        assertFalse(state.canWinLevelNow())
    }

    @Test
    fun noWinWhenNoEnemiesRemain() {
        val state = playerTurnState(buildLevel(healthPoints = 10))
        // No alive attackers and no pending spawns -> level already effectively won, no offer
        assertFalse(state.canWinLevelNow())
    }
}
