package de.egril.defender.model

import androidx.compose.runtime.mutableStateOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for the spawn-loop runtime scheduler (issue #694): [GameState.plannedSpawnsForTurn],
 * [GameState.forecastRemainingGroupSpawns] and the completion integration in [GameState.isLevelWon].
 */
class SpawnGroupSchedulerTest {
    private fun buildLevel(groups: List<SpawnGroup>?): Level =
        Level(
            id = 1,
            name = "Spawn Loop Test",
            gridWidth = 10,
            gridHeight = 6,
            startPositions = listOf(Position(0, 2)),
            targetPositions = listOf(Position(9, 2)),
            pathCells = (0..9).map { Position(it, 2) }.toSet(),
            attackerWaves = emptyList(),
            spawnGroups = groups,
            healthPoints = 10,
        )

    /**
     * Mimics the engine spawn path for a single turn: pull the planned spawns, create attackers and
     * bind any logical unit ids, exactly as [de.egril.defender.game.EnemyMovementSystem] does.
     */
    private fun simulateTurn(
        state: GameState,
        turn: Int,
    ): List<PlannedEnemySpawn> {
        state.turnNumber.value = turn
        val planned = state.plannedSpawnsForTurn(turn)
        planned.forEach { spawn ->
            val attacker =
                Attacker(
                    id = state.nextAttackerId.value++,
                    type = spawn.attackerType,
                    position = mutableStateOf(Position(1, 2)),
                    level = mutableStateOf(spawn.level),
                )
            state.attackers.add(attacker)
            spawn.unitId?.let { state.bindSpawnGroupUnit(it, attacker.id) }
        }
        return planned
    }

    @Test
    fun countModeRunsFixedNumberOfIterationsInOrder() {
        val group =
            SpawnGroup(
                groupId = "wave",
                repeatMode = SpawnRepeatMode.COUNT,
                repeatCount = 3,
                turns =
                    listOf(
                        SpawnGroupTurn(1, listOf(SpawnGroupSpawn(AttackerType.GOBLIN, count = 2))),
                        SpawnGroupTurn(2, listOf(SpawnGroupSpawn(AttackerType.ORK, count = 1))),
                    ),
            )
        val state = GameState(buildLevel(listOf(group)))

        // 3 iterations over 2 turns each = turns 1..6.
        assertEquals(listOf(AttackerType.GOBLIN, AttackerType.GOBLIN), simulateTurn(state, 1).map { it.attackerType })
        assertEquals(listOf(AttackerType.ORK), simulateTurn(state, 2).map { it.attackerType })
        assertEquals(listOf(AttackerType.GOBLIN, AttackerType.GOBLIN), simulateTurn(state, 3).map { it.attackerType })
        assertEquals(listOf(AttackerType.ORK), simulateTurn(state, 4).map { it.attackerType })
        assertEquals(2, simulateTurn(state, 5).size) // iteration 3, goblins
        assertEquals(listOf(AttackerType.ORK), simulateTurn(state, 6).map { it.attackerType })

        // The loop decision is deferred to the next turn (so CONDITION checks can see resolved
        // combat), so the cursor is marked finished on turn 7, which itself spawns nothing.
        assertTrue(simulateTurn(state, 7).isEmpty())
        assertTrue(state.spawnGroupCursor.value.finished)

        // 6 goblins (2 per iteration) + 3 orks were scheduled in total.
        assertEquals(9, state.attackers.size)
    }

    @Test
    fun gapsAndEmptyTurnsProduceNoSpawns() {
        val group =
            SpawnGroup(
                groupId = "gappy",
                repeatMode = SpawnRepeatMode.COUNT,
                repeatCount = 1,
                turns =
                    listOf(
                        SpawnGroupTurn(1, listOf(SpawnGroupSpawn(AttackerType.GOBLIN))),
                        // offset 2 intentionally omitted (a gap)
                        SpawnGroupTurn(3, emptyList()), // explicit blank turn
                        SpawnGroupTurn(4, listOf(SpawnGroupSpawn(AttackerType.ORK))),
                    ),
            )
        val state = GameState(buildLevel(listOf(group)))

        assertEquals(listOf(AttackerType.GOBLIN), simulateTurn(state, 1).map { it.attackerType })
        assertTrue(simulateTurn(state, 2).isEmpty()) // gap
        assertTrue(simulateTurn(state, 3).isEmpty()) // explicit blank
        assertEquals(listOf(AttackerType.ORK), simulateTurn(state, 4).map { it.attackerType })
        // The single iteration completes; its end is recognized on the next turn (deferred decision).
        assertTrue(simulateTurn(state, 5).isEmpty())
        assertTrue(state.spawnGroupCursor.value.finished)
    }

    @Test
    fun conditionLoopsWhileBoundUnitIsAliveAndExitsWhenDead() {
        val group =
            SpawnGroup(
                groupId = "boss_phase",
                repeatMode = SpawnRepeatMode.CONDITION,
                condition = SpawnCondition.UNIT_ALIVE,
                targetUnitId = "boss",
                turns =
                    listOf(
                        SpawnGroupTurn(
                            1,
                            listOf(
                                SpawnGroupSpawn(AttackerType.EWHAD, unitId = "boss", firstIterationOnly = true),
                                SpawnGroupSpawn(AttackerType.GOBLIN),
                            ),
                        ),
                        SpawnGroupTurn(2, listOf(SpawnGroupSpawn(AttackerType.SKELETON))),
                    ),
            )
        val nextGroup =
            SpawnGroup(
                groupId = "after",
                repeatMode = SpawnRepeatMode.COUNT,
                repeatCount = 1,
                turns = listOf(SpawnGroupTurn(1, listOf(SpawnGroupSpawn(AttackerType.ORK)))),
            )
        val state = GameState(buildLevel(listOf(group, nextGroup)))

        // Iteration 0: boss + goblin, then skeleton. Boss alive at end of cycle → loop.
        assertEquals(listOf(AttackerType.EWHAD, AttackerType.GOBLIN), simulateTurn(state, 1).map { it.attackerType })
        val bossId = state.spawnGroupBindings["boss"]
        assertTrue(bossId != null)
        assertEquals(listOf(AttackerType.SKELETON), simulateTurn(state, 2).map { it.attackerType })

        // Iteration 1: boss is NOT respawned (firstIterationOnly) — only the add-spawns repeat.
        assertEquals(listOf(AttackerType.GOBLIN), simulateTurn(state, 3).map { it.attackerType })
        // Kill the boss during iteration 1 (before its end-of-cycle check on the next turn).
        state.attackers
            .first { it.id == bossId }
            .isDefeated.value = true
        assertEquals(listOf(AttackerType.SKELETON), simulateTurn(state, 4).map { it.attackerType })
        // Decision is deferred: iteration 1's end is only evaluated on turn 5, so the cursor is
        // still on the first group right after turn 4.
        assertEquals(0, state.spawnGroupCursor.value.groupIndex)

        // Boss dead at end of iteration 1 → turn 5 advances to the next group and spawns its ork.
        assertEquals(listOf(AttackerType.ORK), simulateTurn(state, 5).map { it.attackerType })
        assertEquals(1, state.spawnGroupCursor.value.groupIndex)
        assertFalse(state.spawnGroupCursor.value.finished)
        assertTrue(simulateTurn(state, 6).isEmpty())
        assertTrue(state.spawnGroupCursor.value.finished)
    }

    @Test
    fun conditionCheckedAfterFinalSubturnCombatNotAtSpawnTime() {
        // Single-subturn iteration: the boss and a minion spawn together each iteration (boss only on
        // the first). The boss dies AFTER the final subturn is spawned (mimicking death during that
        // same turn's completeEnemyTurn combat / acid / traps). The loop must still exit, proving the
        // CONDITION is evaluated on the next advance, not at the last subturn's spawn time.
        val group =
            SpawnGroup(
                groupId = "boss_tight_loop",
                repeatMode = SpawnRepeatMode.CONDITION,
                condition = SpawnCondition.UNIT_ALIVE,
                targetUnitId = "boss",
                turns =
                    listOf(
                        SpawnGroupTurn(
                            1,
                            listOf(
                                SpawnGroupSpawn(AttackerType.EWHAD, unitId = "boss", firstIterationOnly = true),
                                SpawnGroupSpawn(AttackerType.GOBLIN),
                            ),
                        ),
                    ),
            )
        val state = GameState(buildLevel(listOf(group)))

        assertEquals(listOf(AttackerType.EWHAD, AttackerType.GOBLIN), simulateTurn(state, 1).map { it.attackerType })
        val bossId = state.spawnGroupBindings["boss"]
        assertTrue(bossId != null)
        // Boss dies after turn 1's spawn but before turn 2's advance (as it would during combat).
        state.attackers.first { it.id == bossId }.isDefeated.value = true

        // With an eager end-of-subturn check this would have already looped and spawned another
        // goblin; with the deferred check the dead boss ends the loop and nothing more spawns.
        assertTrue(simulateTurn(state, 2).isEmpty())
        assertTrue(state.spawnGroupCursor.value.finished)
    }

    @Test
    fun conditionWithMissingBindingExitsAfterFirstIteration() {
        val group =
            SpawnGroup(
                groupId = "needs_missing_unit",
                repeatMode = SpawnRepeatMode.CONDITION,
                condition = SpawnCondition.UNIT_ALIVE,
                targetUnitId = "never_spawned",
                turns =
                    listOf(
                        SpawnGroupTurn(1, listOf(SpawnGroupSpawn(AttackerType.GOBLIN))),
                        SpawnGroupTurn(2, listOf(SpawnGroupSpawn(AttackerType.SKELETON))),
                    ),
            )
        val state = GameState(buildLevel(listOf(group)))

        simulateTurn(state, 1)
        simulateTurn(state, 2) // final subturn of the only iteration
        // The missing bound unit is treated as dead; the exit is decided on the next advance.
        assertTrue(simulateTurn(state, 3).isEmpty())
        assertTrue(state.spawnGroupCursor.value.finished)
    }

    @Test
    fun infiniteGroupNeverFinishesAndBlocksAutomaticVictory() {
        val group =
            SpawnGroup(
                groupId = "endless",
                repeatMode = SpawnRepeatMode.INFINITE,
                turns = listOf(SpawnGroupTurn(1, listOf(SpawnGroupSpawn(AttackerType.RED_WITCH)))),
            )
        val state = GameState(buildLevel(listOf(group)))

        repeat(20) { i -> assertEquals(1, simulateTurn(state, i + 1).size) }
        assertFalse(state.spawnGroupCursor.value.finished)
        // Forecast is unbounded for an active INFINITE group.
        assertNull(state.forecastRemainingGroupSpawns())

        // Even if every spawned enemy is defeated, the level cannot be auto-won.
        state.phase.value = GamePhase.PLAYER_TURN
        state.attackers.forEach { it.isDefeated.value = true }
        assertFalse(state.isLevelWon())
    }

    @Test
    fun finishedCountGroupAllowsNormalVictory() {
        val group =
            SpawnGroup(
                groupId = "wave",
                repeatMode = SpawnRepeatMode.COUNT,
                repeatCount = 1,
                turns = listOf(SpawnGroupTurn(1, listOf(SpawnGroupSpawn(AttackerType.GOBLIN)))),
            )
        val state = GameState(buildLevel(listOf(group)))

        state.phase.value = GamePhase.PLAYER_TURN
        simulateTurn(state, 1)
        // The bounded schedule is exhausted (all future spawns are in the past), so the level counts
        // as fully spawned even though the cursor is marked finished only on the next advance.
        assertTrue(state.forecastRemainingGroupSpawns()?.isEmpty() == true)
        assertFalse(state.isLevelWon()) // goblin still alive
        state.attackers.forEach { it.isDefeated.value = true }
        assertTrue(state.isLevelWon())
    }

    @Test
    fun legacyFlatPlanIsUnaffectedBySpawnGroupLogic() {
        val level =
            buildLevel(groups = null).copy(
                directSpawnPlan =
                    listOf(
                        PlannedEnemySpawn(AttackerType.GOBLIN, spawnTurn = 1),
                        PlannedEnemySpawn(AttackerType.ORK, spawnTurn = 3),
                    ),
            )
        val state = GameState(level)
        assertNull(state.spawnGroups)

        assertEquals(listOf(AttackerType.GOBLIN), state.plannedSpawnsForTurn(1).map { it.attackerType })
        assertTrue(state.plannedSpawnsForTurn(2).isEmpty())
        assertEquals(listOf(AttackerType.ORK), state.plannedSpawnsForTurn(3).map { it.attackerType })

        state.turnNumber.value = 5
        state.phase.value = GamePhase.PLAYER_TURN
        assertTrue(state.isLevelWon()) // all legacy spawns past, no attackers
    }
}
