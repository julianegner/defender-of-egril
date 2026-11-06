package de.egril.defender.save

import de.egril.defender.model.EventMapImage
import de.egril.defender.model.GamePhase
import de.egril.defender.model.GameState
import de.egril.defender.model.Level
import de.egril.defender.model.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class EventMapImageSaveLoadTest {
    private val images =
        listOf(
            EventMapImage("sea \"one\", {wave}[0]\\", "high \"tide\", {sea}[0].png", -2.25f, -0.75f, 3.5f, 1.25f),
            EventMapImage("foreground", "overlay.webp", 2.5e-5f, 8.75f, 2f, 4.5f),
        )

    private fun savedGame(activeImages: List<EventMapImage> = images): SavedGame =
        SavedGame(
            id = "image-save",
            timestamp = 1234L,
            levelId = 7,
            levelName = "Image Level",
            turnNumber = 12,
            coins = 80,
            healthPoints = 6,
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
            activeEventMapImages = activeImages,
        )

    private fun level(): Level =
        Level(
            id = 7,
            name = "Image Level",
            gridWidth = 5,
            gridHeight = 5,
            startPositions = listOf(Position(0, 0)),
            targetPositions = listOf(Position(4, 4)),
            pathCells = (0 until 5).flatMap { x -> (0 until 5).map { y -> Position(x, y) } }.toSet(),
            attackerWaves = emptyList(),
            initialCoins = 50,
            healthPoints = 10,
        )

    @Test
    fun activeImagesRoundTripWithGeometryEscapingAndOrder() {
        val original = savedGame()
        val loaded = assertNotNull(SaveJsonSerializer.deserializeSavedGame(SaveJsonSerializer.serializeSavedGame(original)))
        assertEquals(images, loaded.activeEventMapImages)
    }

    @Test
    fun oldSavesDefaultToNoActiveImages() {
        val json =
            SaveJsonSerializer
                .serializeSavedGame(savedGame(emptyList()))
                .replace(Regex(""""activeEventMapImages": \[\],\s*"""), "")
        assertTrue(!json.contains("activeEventMapImages"))
        val loaded = assertNotNull(SaveJsonSerializer.deserializeSavedGame(json))
        assertTrue(loaded.activeEventMapImages.isEmpty())
        assertTrue(SaveFileStorage.convertSavedGameToGameState(loaded, level()).activeEventMapImages.isEmpty())
    }

    @Test
    fun gameStateConversionAndRestorationPreserveActiveImages() {
        val level = level()
        val state = GameState(level)
        state.activeEventMapImages.addAll(images)
        val snapshot = SaveFileStorage.convertGameStateToSavedGame(state, "image-save", null)
        state.activeEventMapImages.clear()
        assertEquals(images, snapshot.activeEventMapImages)

        val loaded = assertNotNull(SaveJsonSerializer.deserializeSavedGame(SaveJsonSerializer.serializeSavedGame(snapshot)))
        val restored = SaveFileStorage.convertSavedGameToGameState(loaded, level)
        assertEquals(images, restored.activeEventMapImages.toList())
    }

    @Test
    fun malformedImagesDoNotPreventValidImagesFromLoading() {
        val invalidImages =
            listOf(
                EventMapImage("zero", "sea.png", width = 0f),
                EventMapImage("negative", "sea.png", height = -1f),
                EventMapImage("infinite", "sea.png", x = Float.POSITIVE_INFINITY),
                EventMapImage("nan", "sea.png", width = Float.NaN),
                EventMapImage("", "sea.png"),
                EventMapImage("path", "../sea.png"),
            )
        val saved = savedGame(invalidImages + images)
        val loaded = assertNotNull(SaveJsonSerializer.deserializeSavedGame(SaveJsonSerializer.serializeSavedGame(saved)))
        assertEquals(images, loaded.activeEventMapImages)
        assertEquals(images, SaveFileStorage.convertSavedGameToGameState(saved, level()).activeEventMapImages.toList())
    }
}
