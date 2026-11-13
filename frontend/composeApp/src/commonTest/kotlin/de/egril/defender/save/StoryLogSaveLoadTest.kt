package de.egril.defender.save

import de.egril.defender.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class StoryLogSaveLoadTest {
    private fun state(): GameState =
        GameState(
            Level(
                id = 1,
                name = "Story log save",
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
    fun storyLogRoundTripsThroughSaveAndLoad() {
        val state = state()
        state.storyLog.add(
            StoryLogEntry(
                turn = 3,
                message = GameMessage(type = GameMessageType.STORY_INTRO, name = "intro_level"),
            ),
        )
        state.storyLog.add(
            StoryLogEntry(
                turn = 5,
                message = GameMessage(type = GameMessageType.EVENT_MESSAGE, name = "event_msg_a", eventMessageAmount = 7),
            ),
        )

        val saved = SaveFileStorage.convertGameStateToSavedGame(state, "story-log")
        val parsed = assertNotNull(SaveJsonSerializer.deserializeSavedGame(SaveJsonSerializer.serializeSavedGame(saved)))
        val restored = SaveFileStorage.convertSavedGameToGameState(parsed, state.level)

        assertEquals(2, restored.storyLog.size)
        val first = restored.storyLog[0]
        assertEquals(3, first.turn)
        assertEquals(GameMessageType.STORY_INTRO, first.message.type)
        assertEquals("intro_level", first.message.name)
        val second = restored.storyLog[1]
        assertEquals(5, second.turn)
        assertEquals(GameMessageType.EVENT_MESSAGE, second.message.type)
        assertEquals("event_msg_a", second.message.name)
        assertEquals(7, second.message.eventMessageAmount)
    }

    @Test
    fun saveWithoutStoryLogFieldLoadsWithEmptyLog() {
        val state = state()
        val saved = SaveFileStorage.convertGameStateToSavedGame(state, "no-story-log")
        val json = SaveJsonSerializer.serializeSavedGame(saved)
        val oldJson = json.replace(Regex("\"storyLog\":\\s*\\[[^\\]]*\\],?"), "")
        assertTrue(!oldJson.contains("\"storyLog\""))

        val parsed = assertNotNull(SaveJsonSerializer.deserializeSavedGame(oldJson))
        val restored = SaveFileStorage.convertSavedGameToGameState(parsed, state.level)
        assertTrue(restored.storyLog.isEmpty())
    }
}
