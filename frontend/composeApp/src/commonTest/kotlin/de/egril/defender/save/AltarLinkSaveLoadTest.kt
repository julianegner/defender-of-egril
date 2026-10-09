package de.egril.defender.save

import de.egril.defender.model.AltarLink
import de.egril.defender.model.GamePhase
import de.egril.defender.model.GameState
import de.egril.defender.model.Level
import de.egril.defender.model.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AltarLinkSaveLoadTest {
    private val links =
        listOf(
            AltarLink("link \"one\", {x}", Position(2, 3), Position(4, 5)),
            AltarLink("second", Position(0, 1), Position(7, 1)),
        )

    private fun savedGame(activeLinks: List<AltarLink> = links): SavedGame =
        SavedGame(
            id = "altar-link-save",
            timestamp = 1234L,
            levelId = 7,
            levelName = "Altar Level",
            turnNumber = 4,
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
            activeAltarLinks = activeLinks,
        )

    private fun level(): Level =
        Level(
            id = 7,
            name = "Altar Level",
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
    fun activeAltarLinksRoundTripThroughJson() {
        val loaded = assertNotNull(SaveJsonSerializer.deserializeSavedGame(SaveJsonSerializer.serializeSavedGame(savedGame())))
        assertEquals(links, loaded.activeAltarLinks)
    }

    @Test
    fun oldSavesDefaultToNoAltarLinks() {
        val json =
            SaveJsonSerializer
                .serializeSavedGame(savedGame(emptyList()))
                .replace(Regex(""""activeAltarLinks": \[\],\s*"""), "")
        assertTrue(!json.contains("activeAltarLinks"))
        val loaded = assertNotNull(SaveJsonSerializer.deserializeSavedGame(json))
        assertTrue(loaded.activeAltarLinks.isEmpty())
    }

    @Test
    fun gameStateConversionAndRestorationPreserveAltarLinks() {
        val level = level()
        val state = GameState(level)
        state.activeAltarLinks.addAll(links)
        val snapshot = SaveFileStorage.convertGameStateToSavedGame(state, "altar-link-save", null)
        state.activeAltarLinks.clear()
        assertEquals(links, snapshot.activeAltarLinks)

        val restored = SaveFileStorage.convertSavedGameToGameState(snapshot, level)
        assertEquals(links, restored.activeAltarLinks.toList())
    }
}
