package de.egril.defender.ui

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GameViewModelAltarVictoryTest {
    private fun viewModel(
        condition: EventCondition = EventCondition(EventConditionType.ALTARS_ACTIVATED, threshold = 1),
        messageKey: String? = "event_msg_rune_network_taken_over",
        startPlayerTurn: Boolean = true,
    ): GameViewModel {
        val level =
            Level(
                id = -696,
                name = "Altar victory test",
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
                                condition = condition,
                                actions = listOf(EventAction(EventActionType.WIN_LEVEL)),
                                messageKey = messageKey,
                            ),
                        ),
                    ),
            )
        return GameViewModel().also {
            GameViewModel::class.java
                .getDeclaredMethod("startEditorPlaytestInternal", Level::class.java)
                .apply { isAccessible = true }
                .invoke(it, level)
            if (startPlayerTurn) it.startFirstPlayerTurn()
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
    fun scriptedVictoryStoryRemainsVisibleBeforeNormalLevelCompletion() {
        val viewModel = viewModel()
        try {
            assertTrue(viewModel.activateAltar(1))
            val state = assertNotNull(viewModel.gameState.value)
            assertTrue(state.scriptedVictory.value)
            assertIs<Screen.GamePlay>(viewModel.currentScreen.value)
            val message = assertNotNull(viewModel.pendingGameMessage.value)
            assertEquals("event_msg_rune_network_taken_over", message.name)
            assertEquals(1, message.eventMessageAmount)
            viewModel.dismissGameMessage()
            assertNull(viewModel.pendingGameMessage.value)
            assertTrue(assertIs<Screen.LevelComplete>(viewModel.currentScreen.value).won)
        } finally {
            viewModel.stopTimeTracking()
        }
    }

    @Test
    fun scriptedVictoryWaitsUntilAllQueuedMessagesAreDismissed() {
        val viewModel = viewModel()
        try {
            val state = assertNotNull(viewModel.gameState.value)
            state.pendingMessages.add(GameMessage(GameMessageType.EVENT_MESSAGE, name = "event_msg_test"))
            assertTrue(viewModel.activateAltar(1))
            assertIs<Screen.GamePlay>(viewModel.currentScreen.value)
            assertEquals("event_msg_test", viewModel.pendingGameMessage.value?.name)
            viewModel.dismissGameMessage()
            assertIs<Screen.GamePlay>(viewModel.currentScreen.value)
            assertEquals("event_msg_rune_network_taken_over", viewModel.pendingGameMessage.value?.name)
            assertFalse(state.isLevelLost())
            viewModel.dismissGameMessage()
            assertTrue(assertIs<Screen.LevelComplete>(viewModel.currentScreen.value).won)
        } finally {
            viewModel.stopTimeTracking()
        }
    }

    @Test
    fun silentFirstTurnVictoryCompletesWithoutRequiringAnotherAction() {
        val viewModel =
            viewModel(
                condition = EventCondition(EventConditionType.TURN_START),
                messageKey = null,
                startPlayerTurn = false,
            )
        try {
            assertIs<Screen.GamePlay>(viewModel.currentScreen.value)
            viewModel.startFirstPlayerTurn()
            assertTrue(assertIs<Screen.LevelComplete>(viewModel.currentScreen.value).won)
            assertNull(viewModel.pendingGameMessage.value)
        } finally {
            viewModel.stopTimeTracking()
        }
    }

    @Test
    fun silentCoinSpendVictoryUsesNormalCompletionImmediately() {
        val viewModel =
            viewModel(
                condition = EventCondition(EventConditionType.COINS_AT_OR_BELOW, threshold = 0),
                messageKey = null,
            )
        try {
            val state = assertNotNull(viewModel.gameState.value)
            state.coins.value = DefenderType.BOW_TOWER.baseCost
            state.defenders.clear()
            assertIs<Screen.GamePlay>(viewModel.currentScreen.value)
            assertTrue(viewModel.placeDefender(DefenderType.BOW_TOWER, Position(2, 1)))
            assertEquals(0, state.coins.value)
            assertTrue(state.scriptedVictory.value)
            assertTrue(assertIs<Screen.LevelComplete>(viewModel.currentScreen.value).won)
            assertNull(viewModel.pendingGameMessage.value)
        } finally {
            viewModel.stopTimeTracking()
        }
    }

    @Test
    fun endingTurnAfterScriptedVictoryDoesNotAdvanceTheTurn() =
        runBlocking {
            val viewModel = viewModel()
            try {
                assertTrue(viewModel.activateAltar(1))
                val state = assertNotNull(viewModel.gameState.value)
                val turnBefore = state.turnNumber.value
                val phaseBefore = state.phase.value
                viewModel.endPlayerTurn()
                delay(250)
                assertEquals(turnBefore, state.turnNumber.value)
                assertEquals(phaseBefore, state.phase.value)
                assertIs<Screen.GamePlay>(viewModel.currentScreen.value)
                assertNotNull(viewModel.pendingGameMessage.value)
                viewModel.dismissGameMessage()
                assertTrue(assertIs<Screen.LevelComplete>(viewModel.currentScreen.value).won)
                viewModel.endPlayerTurn()
                delay(250)
                assertEquals(turnBefore, state.turnNumber.value)
            } finally {
                viewModel.stopTimeTracking()
            }
        }

    @Test
    fun enemyTurnVictoryStopsMovementAndWaitsForItsStory() =
        runBlocking {
            val viewModel =
                viewModel(
                    condition = EventCondition(EventConditionType.ENEMY_TURN_START),
                )
            try {
                val state = assertNotNull(viewModel.gameState.value)
                val enemy =
                    Attacker(
                        id = 99,
                        type = AttackerType.GOBLIN,
                        position = mutableStateOf(Position(2, 3)),
                    )
                state.attackers.add(enemy)
                val positionBefore = enemy.position.value
                viewModel.endPlayerTurn()
                for (attempt in 0..100) {
                    if (viewModel.pendingGameMessage.value != null) break
                    delay(10)
                }
                assertTrue(state.scriptedVictory.value)
                assertNotNull(viewModel.pendingGameMessage.value)
                assertIs<Screen.GamePlay>(viewModel.currentScreen.value)
                delay(250)
                assertEquals(positionBefore, enemy.position.value)
                viewModel.dismissGameMessage()
                assertTrue(assertIs<Screen.LevelComplete>(viewModel.currentScreen.value).won)
            } finally {
                viewModel.stopTimeTracking()
            }
        }
}
