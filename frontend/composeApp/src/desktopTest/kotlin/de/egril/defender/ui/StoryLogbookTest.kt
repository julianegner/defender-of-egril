package de.egril.defender.ui

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class StoryLogbookTest {
    private fun viewModelWithAltarVictory(): GameViewModel {
        val level =
            Level(
                id = -697,
                name = "Story logbook test",
                gridWidth = 10,
                gridHeight = 6,
                startPositions = listOf(Position(0, 3)),
                targetPositions = listOf(Position(9, 3)),
                pathCells = (0..9).map { Position(it, 3) }.toSet(),
                buildAreas = setOf(Position(2, 1)),
                attackerWaves = listOf(AttackerWave(listOf(AttackerType.GOBLIN))),
                events =
                    LevelEvents(
                        listOf(
                            LevelEvent(
                                id = "altar-victory",
                                condition = EventCondition(EventConditionType.ALTARS_ACTIVATED, threshold = 1),
                                actions = listOf(EventAction(EventActionType.WIN_LEVEL)),
                                messageKey = "event_msg_rune_network_taken_over",
                            ),
                        ),
                    ),
            )
        return GameViewModel().also {
            GameViewModel::class.java
                .getDeclaredMethod("startEditorPlaytestInternal", Level::class.java)
                .apply { isAccessible = true }
                .invoke(it, level)
            it.startFirstPlayerTurn()
            val state = assertNotNull(it.gameState.value)
            state.defenders.add(
                Defender(
                    id = 1,
                    type = DefenderType.ALTAR,
                    position = mutableStateOf(Position(2, 1)),
                    level = mutableStateOf(10),
                    actionsRemaining = mutableStateOf(1),
                ),
            )
        }
    }

    @Test
    fun storyMessageIsLoggedWithCurrentTurnAndSurvivesDismissal() {
        val viewModel = viewModelWithAltarVictory()
        try {
            val state = assertNotNull(viewModel.gameState.value)
            val expectedTurn = state.turnNumber.value
            assertTrue(state.storyLog.isEmpty())

            assertTrue(viewModel.activateAltar(1))
            val message = assertNotNull(viewModel.pendingGameMessage.value)
            assertEquals(1, state.storyLog.size)
            assertEquals(expectedTurn, state.storyLog.last().turn)
            assertEquals(message, state.storyLog.last().message)

            viewModel.dismissGameMessage()
            assertEquals(1, state.storyLog.size)
        } finally {
            viewModel.stopTimeTracking()
        }
    }
}
