package de.egril.defender.save

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.GamePhase
import de.egril.defender.model.GameState
import de.egril.defender.model.Gargoyle
import de.egril.defender.model.Level
import de.egril.defender.model.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GargoyleSaveLoadTest {
    private fun gargoyle(
        id: Int,
        position: Position,
        level: Int,
        currentHealth: Int,
        isActive: Boolean = false,
        activeAttackerId: Int? = null,
    ) = Gargoyle(
        id = id,
        position = mutableStateOf(position),
        level = mutableStateOf(level),
        currentHealth = mutableStateOf(currentHealth),
        isActive = mutableStateOf(isActive),
        activeAttackerId = mutableStateOf(activeAttackerId),
    )

    private val savedGargoyles =
        listOf(
            SavedGargoyle(id = 1, position = Position(3, 4), level = 2, currentHealth = 700),
            SavedGargoyle(
                id = 2,
                position = Position(5, 6),
                level = 1,
                currentHealth = 320,
                isActive = true,
                activeTurnsRemaining = 0,
                activeAttackerId = 42,
            ),
        )

    private fun savedGame(gargoyles: List<SavedGargoyle> = savedGargoyles): SavedGame =
        SavedGame(
            id = "gargoyle-save",
            timestamp = 1234L,
            levelId = 7,
            levelName = "Gargoyle Level",
            turnNumber = 4,
            coins = 80,
            healthPoints = 6,
            phase = GamePhase.PLAYER_TURN,
            defenders = emptyList(),
            attackers = emptyList(),
            nextDefenderId = 1,
            nextAttackerId = 43,
            currentWaveIndex = 0,
            spawnCounter = 0,
            attackersToSpawn = emptyList(),
            fieldEffects = emptyList(),
            traps = emptyList(),
            gargoyles = gargoyles,
            nextGargoyleId = 3,
        )

    private fun level(): Level =
        Level(
            id = 7,
            name = "Gargoyle Level",
            gridWidth = 8,
            gridHeight = 8,
            startPositions = listOf(Position(0, 0)),
            targetPositions = listOf(Position(7, 7)),
            pathCells = (0 until 8).flatMap { x -> (0 until 8).map { y -> Position(x, y) } }.toSet(),
            attackerWaves = emptyList(),
            initialCoins = 50,
            healthPoints = 10,
        )

    @Test
    fun inactiveAndActiveGargoylesRoundTripThroughJson() {
        val loaded =
            assertNotNull(SaveJsonSerializer.deserializeSavedGame(SaveJsonSerializer.serializeSavedGame(savedGame())))
        assertEquals(savedGargoyles, loaded.gargoyles)
        assertEquals(3, loaded.nextGargoyleId)
    }

    @Test
    fun oldSavesWithoutGargoylesDefaultToEmpty() {
        val json =
            SaveJsonSerializer
                .serializeSavedGame(savedGame(emptyList()))
                .replace(Regex(""""gargoyles": \[[^\]]*\],\s*"""), "")
                .replace(Regex(""""nextGargoyleId": \d+,\s*"""), "")
        assertTrue(!json.contains("gargoyles"))
        val loaded = assertNotNull(SaveJsonSerializer.deserializeSavedGame(json))
        assertTrue(loaded.gargoyles.isEmpty())
        assertEquals(1, loaded.nextGargoyleId)
    }

    @Test
    fun gameStateConversionAndRestorationPreserveGargoyles() {
        val level = level()
        val state = GameState(level)
        state.gargoyles.clear()
        state.gargoyles.add(gargoyle(1, Position(3, 4), 2, 700))
        state.gargoyles.add(gargoyle(2, Position(5, 6), 1, 320, isActive = true, activeAttackerId = 42))
        state.nextGargoyleId.value = 3

        val snapshot = SaveFileStorage.convertGameStateToSavedGame(state, "gargoyle-save", null)
        assertEquals(savedGargoyles, snapshot.gargoyles)
        assertEquals(3, snapshot.nextGargoyleId)

        val restored = SaveFileStorage.convertSavedGameToGameState(snapshot, level)
        assertEquals(3, restored.nextGargoyleId.value)
        assertEquals(
            savedGargoyles,
            restored.gargoyles.map { g ->
                SavedGargoyle(
                    id = g.id,
                    position = g.position.value,
                    level = g.level.value,
                    currentHealth = g.currentHealth.value,
                    isActive = g.isActive.value,
                    activeTurnsRemaining = g.activeTurnsRemaining.value,
                    activeAttackerId = g.activeAttackerId.value,
                )
            },
        )
    }
}
