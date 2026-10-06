package de.egril.defender.save

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.Attacker
import de.egril.defender.model.AttackerType
import de.egril.defender.model.GamePhase
import de.egril.defender.model.GameState
import de.egril.defender.model.Level
import de.egril.defender.model.PlannedEnemySpawn
import de.egril.defender.model.Position
import de.egril.defender.model.SpawnCondition
import de.egril.defender.model.SpawnGroup
import de.egril.defender.model.SpawnGroupSpawn
import de.egril.defender.model.SpawnGroupTurn
import de.egril.defender.model.SpawnRepeatMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Mid-cycle save/load tests for spawn groups (issue #694): the runtime cursor and the UNIT_ALIVE
 * bindings must survive a full serialize → deserialize → convert round-trip so a loaded game
 * continues the loop exactly where it left off. Old saves (no cursor) must still load.
 */
class SpawnGroupSaveLoadTest {
    private fun groupLevel(groups: List<SpawnGroup>?): Level =
        Level(
            id = 7,
            name = "Spawn Loop Save",
            gridWidth = 10,
            gridHeight = 6,
            startPositions = listOf(Position(0, 2)),
            targetPositions = listOf(Position(9, 2)),
            pathCells = (0..9).map { Position(it, 2) }.toSet(),
            attackerWaves = emptyList(),
            spawnGroups = groups,
        )

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

    private val conditionGroup =
        SpawnGroup(
            groupId = "boss_loop",
            repeatMode = SpawnRepeatMode.CONDITION,
            condition = SpawnCondition.UNIT_ALIVE,
            targetUnitId = "boss",
            turns =
                listOf(
                    SpawnGroupTurn(1, listOf(SpawnGroupSpawn(AttackerType.EWHAD, unitId = "boss", firstIterationOnly = true))),
                    SpawnGroupTurn(2, listOf(SpawnGroupSpawn(AttackerType.GOBLIN))),
                ),
        )

    @Test
    fun midCycleCursorAndBindingsRestoreAndContinueExactly() {
        val level = groupLevel(listOf(conditionGroup))
        val state = GameState(level)

        simulateTurn(state, 1) // spawn + bind boss (iteration 0, offset 1)
        simulateTurn(state, 2) // goblin (iteration 0, offset 2 — final subturn)
        simulateTurn(state, 3) // deferred decision: boss alive → loop to repetition 1; boss skipped

        val cursorBefore = state.spawnGroupCursor.value
        assertEquals(0, cursorBefore.groupIndex)
        assertEquals(1, cursorBefore.repetition)
        assertEquals(3, cursorBefore.iterationStartTurn)
        assertFalse(cursorBefore.finished)
        val bossId = state.spawnGroupBindings["boss"]
        assertNotNull(bossId)

        val saved = SaveFileStorage.convertGameStateToSavedGame(state, saveId = "mid-cycle")
        val reloaded = SaveJsonSerializer.deserializeSavedGame(SaveJsonSerializer.serializeSavedGame(saved))
        assertNotNull(reloaded)
        assertNotNull(reloaded.spawnGroupCursor)
        assertEquals(bossId, reloaded.spawnGroupBindings["boss"])

        val restored = SaveFileStorage.convertSavedGameToGameState(reloaded, level)
        val rc = restored.spawnGroupCursor.value
        assertEquals(cursorBefore.groupIndex, rc.groupIndex)
        assertEquals(cursorBefore.repetition, rc.repetition)
        assertEquals(cursorBefore.iterationStartTurn, rc.iterationStartTurn)
        assertEquals(cursorBefore.lastProcessedTurn, rc.lastProcessedTurn)
        assertEquals(bossId, restored.spawnGroupBindings["boss"])
        // The boss attacker must have survived the round-trip so UNIT_ALIVE still evaluates true.
        assertTrue(restored.isSpawnGroupUnitAlive("boss"))

        // Continue on the restored state exactly where it left off: turn 4 is iteration 1's final
        // subturn (spawns the goblin); turn 5 then makes the deferred decision and loops again
        // (boss still alive → repetition 2).
        assertEquals(listOf(AttackerType.GOBLIN), simulateTurn(restored, 4).map { it.attackerType })
        assertEquals(1, restored.spawnGroupCursor.value.repetition)
        assertTrue(simulateTurn(restored, 5).isEmpty())
        assertEquals(2, restored.spawnGroupCursor.value.repetition)
        assertFalse(restored.spawnGroupCursor.value.finished)
    }

    @Test
    fun oldSavesWithoutCursorLoadWithDefaults() {
        val level = groupLevel(listOf(conditionGroup))
        val oldSave =
            SavedGame(
                id = "legacy-save",
                timestamp = 1L,
                levelId = level.id,
                levelName = level.name,
                turnNumber = 0,
                coins = 100,
                healthPoints = 10,
                phase = GamePhase.PLAYER_TURN,
                defenders = emptyList(),
                attackers = emptyList(),
                nextDefenderId = 1,
                nextAttackerId = 1,
                currentWaveIndex = 0,
                spawnCounter = 0,
                attackersToSpawn = emptyList<AttackerType>(),
                fieldEffects = emptyList(),
                traps = emptyList(),
            )

        val reloaded = SaveJsonSerializer.deserializeSavedGame(SaveJsonSerializer.serializeSavedGame(oldSave))
        assertNotNull(reloaded)
        val restored = SaveFileStorage.convertSavedGameToGameState(reloaded, level)

        // No cursor persisted → fresh cursor, and the loop runs from the beginning.
        assertEquals(0, restored.spawnGroupCursor.value.groupIndex)
        assertEquals(0, restored.spawnGroupCursor.value.repetition)
        assertFalse(restored.spawnGroupCursor.value.finished)
        assertTrue(restored.spawnGroupBindings.isEmpty())
        assertEquals(listOf(AttackerType.EWHAD), simulateTurn(restored, 1).map { it.attackerType })
    }
}
