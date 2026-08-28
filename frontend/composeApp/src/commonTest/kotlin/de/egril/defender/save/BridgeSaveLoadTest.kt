package de.egril.defender.save

import de.egril.defender.model.BridgeType
import de.egril.defender.model.GamePhase
import de.egril.defender.model.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class BridgeSaveLoadTest {
    @Test
    fun testBridgesRoundTripThroughSaveJson() {
        val savedGame =
            SavedGame(
                id = "bridge-save",
                timestamp = 1234L,
                levelId = 7,
                levelName = "Bridge Level",
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
                bridges =
                    listOf(
                        SavedBridge(
                            id = 1,
                            type = BridgeType.WOODEN,
                            positions = listOf(Position(2, 3)),
                            currentHealth = 35,
                            turnsRemaining = 0,
                            createdByAttackerId = 11,
                            createdOnTurn = 8,
                        ),
                        SavedBridge(
                            id = 2,
                            type = BridgeType.MAGICAL,
                            positions = listOf(Position(5, 1)),
                            currentHealth = 0,
                            turnsRemaining = 2,
                            createdByAttackerId = 12,
                            createdOnTurn = 9,
                        ),
                    ),
                nextBridgeId = 3,
            )

        val json = SaveJsonSerializer.serializeSavedGame(savedGame)
        val loaded = SaveJsonSerializer.deserializeSavedGame(json)

        assertNotNull(loaded)
        assertEquals(savedGame.bridges, loaded.bridges)
        assertEquals(savedGame.nextBridgeId, loaded.nextBridgeId)
    }
}
