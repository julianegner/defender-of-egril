package de.egril.defender.save

import de.egril.defender.model.GamePhase
import de.egril.defender.model.Level
import de.egril.defender.model.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class RuneSaveLoadTest {
    @Test
    fun runesRoundTripThroughSaveAndRestore() {
        val level =
            Level(
                id = 1,
                name = "Rune Save Test",
                gridWidth = 10,
                gridHeight = 10,
                startPositions = listOf(Position(0, 0)),
                targetPositions = listOf(Position(9, 9)),
                pathCells = (0..9).flatMap { x -> (0..9).map { y -> Position(x, y) } }.toSet(),
                attackerWaves = emptyList(),
                initialCoins = 0,
                healthPoints = 10,
            )
        val savedGame =
            SavedGame(
                id = "rune-save",
                timestamp = 1L,
                levelId = level.id,
                levelName = level.name,
                turnNumber = 0,
                coins = 0,
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
                runes = 2,
            )

        val serialized = SaveJsonSerializer.serializeSavedGame(savedGame)
        val restoredSave = assertNotNull(SaveJsonSerializer.deserializeSavedGame(serialized))
        val restoredState = SaveFileStorage.convertSavedGameToGameState(restoredSave, level)

        assertEquals(2, restoredSave.runes)
        assertEquals(2, restoredState.runes.value)
    }
}
