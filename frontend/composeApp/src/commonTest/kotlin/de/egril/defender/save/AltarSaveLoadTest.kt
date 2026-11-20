package de.egril.defender.save

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.game.GameEngine
import de.egril.defender.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AltarSaveLoadTest {
    private fun state(): GameState =
        GameState(
            Level(
                id = 1,
                name = "Altar save",
                gridWidth = 10,
                gridHeight = 6,
                startPositions = listOf(Position(0, 3)),
                targetPositions = listOf(Position(9, 3)),
                pathCells = (0..9).map { Position(it, 3) }.toSet(),
                buildAreas = setOf(Position(2, 1)),
                attackerWaves = listOf(AttackerWave(listOf(AttackerType.GOBLIN))),
            ),
        ).also { it.phase.value = GamePhase.PLAYER_TURN }

    @Test
    fun activatedAltarRoundTripsMidTurnWithoutAllowingAnotherActivation() {
        val state = state()
        state.runes.value = 2
        val wizard =
            Defender(
                id = 7,
                type = DefenderType.WIZARD_TOWER,
                position = mutableStateOf(Position(2, 1)),
                level = mutableStateOf(12),
                actionsRemaining = mutableStateOf(1),
            )
        wizard.trapCooldownRemaining.value = 6
        state.defenders.add(wizard)
        val engine = GameEngine(state)
        assertTrue(engine.sanctifyDefender(wizard.id))
        assertTrue(engine.activateAltar(wizard.id))
        val saved = SaveFileStorage.convertGameStateToSavedGame(state, "altar-save")
        val parsed = assertNotNull(SaveJsonSerializer.deserializeSavedGame(SaveJsonSerializer.serializeSavedGame(saved)))
        val restored = SaveFileStorage.convertSavedGameToGameState(parsed, state.level)
        val altar = restored.defenders.single()
        assertEquals(DefenderType.ALTAR, altar.type)
        assertEquals(7, altar.id)
        assertEquals(12, altar.level.value)
        assertTrue(altar.isChanneling.value)
        assertTrue(altar.hasBeenUsed.value)
        assertEquals(0, altar.actionsRemaining.value)
        assertEquals(6, altar.trapCooldownRemaining.value)
        assertEquals(1, restored.runes.value)
        assertFalse(GameEngine(restored).activateAltar(altar.id))
        altar.resetActions()
        assertFalse(altar.isChanneling.value)
        assertTrue(GameEngine(restored).activateAltar(altar.id))
    }

    @Test
    fun oldSavesDefaultToNotChannelingAndZeroCooldown() {
        val state = state()
        state.defenders.add(Defender(1, DefenderType.WIZARD_TOWER, mutableStateOf(Position(2, 1))))
        val saved = SaveFileStorage.convertGameStateToSavedGame(state, "old-save")
        val json =
            SaveJsonSerializer
                .serializeSavedGame(saved)
                .replace(Regex(",?\\s*\"(?:isChanneling|trapCooldownRemaining|hasBeenUsed|isDisabled|disabledTurnsRemaining)\":\\s*(?:false|true|\\d+)"), "")
        val parsed = assertNotNull(SaveJsonSerializer.deserializeSavedGame(json))
        val defender = SaveFileStorage.convertSavedGameToGameState(parsed, state.level).defenders.single()
        assertFalse(defender.isChanneling.value)
        assertFalse(defender.isDisabled.value)
        assertEquals(0, defender.trapCooldownRemaining.value)
    }

    @Test
    fun handoffSerializationRetainsAltarState() {
        val defender =
            SavedDefender(
                id = 1,
                type = DefenderType.ALTAR,
                position = Position(2, 1),
                level = 10,
                buildTimeRemaining = 0,
                placedOnTurn = 1,
                isChanneling = true,
                trapCooldownRemaining = 4,
            )
        val handoff =
            LevelHandoffSave(
                fromLevelEditorId = "first",
                toLevelEditorId = "second",
                coins = 0,
                currentMana = 0,
                maxMana = 10,
                defenders = listOf(defender),
                barricades = emptyList(),
                traps = emptyList(),
                rafts = emptyList(),
                nextDefenderId = 2,
                nextRaftId = 1,
                mapId = "map",
            )
        val parsed = assertNotNull(SaveJsonSerializer.deserializeLevelHandoffSave(SaveJsonSerializer.serializeLevelHandoffSave(handoff)))
        assertEquals(defender, parsed.defenders.single())
    }

    @Test
    fun scriptedVictoryRoundTripsAndOldSavesDefaultToFalse() {
        val state = state()
        state.scriptedVictory.value = true
        val saved = SaveFileStorage.convertGameStateToSavedGame(state, "altar-victory")
        val json = SaveJsonSerializer.serializeSavedGame(saved)
        val parsed = assertNotNull(SaveJsonSerializer.deserializeSavedGame(json))
        val restored = SaveFileStorage.convertSavedGameToGameState(parsed, state.level)
        assertTrue(restored.scriptedVictory.value)
        assertTrue(restored.isLevelWon())
        val oldJson = json.replace(Regex("\\s*\"scriptedVictory\": true,"), "")
        val oldSave = assertNotNull(SaveJsonSerializer.deserializeSavedGame(oldJson))
        assertFalse(oldSave.scriptedVictory)
    }
}
