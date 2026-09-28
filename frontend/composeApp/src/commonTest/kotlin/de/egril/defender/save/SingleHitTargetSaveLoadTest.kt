package de.egril.defender.save

import de.egril.defender.model.AttackerType
import de.egril.defender.model.AttackerWave
import de.egril.defender.model.GamePhase
import de.egril.defender.model.Level
import de.egril.defender.model.Position
import de.egril.defender.model.TargetInfo
import de.egril.defender.model.TargetType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SingleHitTargetSaveLoadTest {
    @Test
    fun capturedTargetsSurviveSaveLoadAndKeepNextTargetInOrder() {
        val capturedTarget = Position(5, 2)
        val nextTarget = Position(9, 2)
        val level =
            Level(
                id = 1,
                name = "Ordered targets",
                targetPositions = listOf(capturedTarget, nextTarget),
                pathCells = emptySet(),
                attackerWaves = listOf(AttackerWave(emptyList())),
                targetInfoMap =
                    mapOf(
                        capturedTarget to TargetInfo(type = TargetType.SINGLE_HIT),
                        nextTarget to TargetInfo(type = TargetType.SINGLE_HIT),
                    ),
                singleHitTargetOrder = listOf(capturedTarget, nextTarget),
            )
        val savedGame =
            SavedGame(
                id = "ordered-target-save",
                timestamp = 1L,
                levelId = level.id,
                levelName = level.name,
                turnNumber = 3,
                coins = 50,
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
                takenTargets = listOf(capturedTarget),
            )

        val loadedSave = SaveJsonSerializer.deserializeSavedGame(SaveJsonSerializer.serializeSavedGame(savedGame))

        assertNotNull(loadedSave)
        assertEquals(listOf(capturedTarget), loadedSave.takenTargets)

        val restoredGame = SaveFileStorage.convertSavedGameToGameState(loadedSave, level)

        assertEquals(listOf(capturedTarget), restoredGame.takenTargets)
        assertEquals(nextTarget, restoredGame.getNextSingleHitTargetPosition())
        assertEquals(listOf(nextTarget), restoredGame.getActiveTargetPositions())
    }

    @Test
    fun olderSavesWithoutCapturedTargetsRemainCompatible() {
        val oldSave =
            SavedGame(
                id = "old-save",
                timestamp = 1L,
                levelId = 1,
                levelName = "Old level",
                turnNumber = 2,
                coins = 50,
                healthPoints = 10,
                phase = GamePhase.PLAYER_TURN,
                defenders = emptyList(),
                attackers = emptyList(),
                nextDefenderId = 1,
                nextAttackerId = 1,
                currentWaveIndex = 0,
                spawnCounter = 0,
                attackersToSpawn = emptyList(),
                fieldEffects = emptyList(),
                traps = emptyList(),
            )
        val oldSaveJson =
            SaveJsonSerializer
                .serializeSavedGame(oldSave)
                .replace("  \"takenTargets\": [],\n", "")

        val loaded = SaveJsonSerializer.deserializeSavedGame(oldSaveJson)

        assertNotNull(loaded)
        assertEquals(emptyList(), loaded.takenTargets)
    }
}
