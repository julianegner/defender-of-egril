package de.egril.defender.save

import de.egril.defender.model.GamePhase
import de.egril.defender.model.Level
import de.egril.defender.model.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PortalSaveLoadTest {
    @Test
    fun testPortalsRoundTripThroughSaveJson() {
        val savedGame =
            SavedGame(
                id = "portal-save",
                timestamp = 1234L,
                levelId = 7,
                levelName = "Portal Level",
                turnNumber = 12,
                coins = 80,
                healthPoints = 6,
                phase = GamePhase.ENEMY_TURN,
                defenders = emptyList(),
                attackers = emptyList(),
                nextDefenderId = 3,
                nextAttackerId = 4,
                currentWaveIndex = 1,
                spawnCounter = 2,
                attackersToSpawn = emptyList(),
                fieldEffects = emptyList(),
                traps = emptyList(),
                activePortals =
                    listOf(
                        SavedPortal(
                            id = 1,
                            entryPosition = Position(2, 3),
                            exitPosition = Position(8, 9),
                            villainId = 42,
                            runeIndex = 5,
                            usedThisTurn = true,
                        ),
                        SavedPortal(
                            id = 2,
                            entryPosition = Position(4, 1),
                            exitPosition = Position(10, 2),
                            villainId = 42,
                            runeIndex = 6,
                            usedThisTurn = false,
                        ),
                    ),
                nextPortalId = 3,
            )

        val json = SaveJsonSerializer.serializeSavedGame(savedGame)
        val loaded = SaveJsonSerializer.deserializeSavedGame(json)

        assertNotNull(loaded)
        assertEquals(savedGame.activePortals, loaded.activePortals)
        assertEquals(savedGame.nextPortalId, loaded.nextPortalId)
    }

    @Test
    fun testConvertSavedGameToGameStateRestoresPortals() {
        val level =
            Level(
                id = 7,
                name = "Portal Level",
                gridWidth = 20,
                gridHeight = 20,
                startPositions = listOf(Position(0, 0)),
                targetPositions = listOf(Position(19, 19)),
                pathCells = (0 until 20).flatMap { x -> (0 until 20).map { y -> Position(x, y) } }.toSet(),
                attackerWaves = emptyList(),
                initialCoins = 50,
                healthPoints = 10,
            )
        val savedGame =
            SavedGame(
                id = "portal-restore",
                timestamp = 1234L,
                levelId = level.id,
                levelName = level.name,
                turnNumber = 3,
                coins = 60,
                healthPoints = 8,
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
                activePortals =
                    listOf(
                        SavedPortal(
                            id = 7,
                            entryPosition = Position(3, 3),
                            exitPosition = Position(12, 11),
                            villainId = 99,
                            runeIndex = 13,
                            usedThisTurn = true,
                        ),
                    ),
                nextPortalId = 8,
            )

        val restored = SaveFileStorage.convertSavedGameToGameState(savedGame, level)

        assertEquals(1, restored.activePortals.size)
        assertEquals(8, restored.nextPortalId.value)
        assertEquals(7, restored.activePortals[0].id)
        assertEquals(Position(3, 3), restored.activePortals[0].entryPosition)
        assertEquals(Position(12, 11), restored.activePortals[0].exitPosition)
        assertEquals(99, restored.activePortals[0].villainId)
        assertEquals(13, restored.activePortals[0].runeIndex)
        assertEquals(true, restored.activePortals[0].usedThisTurn.value)
    }
}
