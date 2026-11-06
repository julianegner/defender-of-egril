package de.egril.defender.ui.gameplay

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import de.egril.defender.model.Attacker
import de.egril.defender.model.AttackerType
import de.egril.defender.model.AttackerWave
import de.egril.defender.model.GameState
import de.egril.defender.model.Level
import de.egril.defender.model.Position
import de.egril.defender.ui.MapPanController
import de.egril.defender.ui.settings.AppSettings
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class MapKeyboardPanTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun buildGameState(): GameState {
        val pathCells = (0..19).map { Position(it, 1) }.toSet()
        val finalTarget = Position(19, 1)
        val level =
            Level(
                id = 1,
                name = "Pan Test",
                gridWidth = 60,
                gridHeight = 60,
                startPositions = listOf(Position(0, 1)),
                targetPositions = listOf(finalTarget),
                pathCells = pathCells,
                attackerWaves = listOf(AttackerWave(listOf(AttackerType.GOBLIN))),
                initialCoins = 100,
                availableTowers = emptySet(),
            )
        val gameState = GameState(level)
        gameState.attackers.add(
            Attacker(
                id = 1,
                type = AttackerType.GOBLIN,
                position = mutableStateOf(Position(0, 1)),
                level = mutableStateOf(1),
                currentTarget = mutableStateOf(finalTarget),
            ),
        )
        return gameState
    }

    @Test
    fun arrowKeyPansMapWhenMapHasFocus() {
        AppSettings.showEnemyPathfinding.value = true
        composeTestRule.setContent {
            GameGrid(
                gameState = buildGameState(),
                selectedDefenderType = null,
                selectedDefenderId = null,
                selectedAttackerId = 1,
                selectedTargetId = null,
                selectedTargetPosition = null,
                selectedMineAction = null,
                onCellClick = { },
            )
        }
        composeTestRule.waitForIdle()

        val before = composeTestRule.onAllNodesWithText("PATH").fetchSemanticsNodes().first().boundsInRoot
        composeTestRule.onRoot().performKeyInput { pressKey(Key.DirectionDown) }
        composeTestRule.waitForIdle()
        val after = composeTestRule.onAllNodesWithText("PATH").fetchSemanticsNodes().first().boundsInRoot

        assertNotEquals(before.top, after.top, "Arrow key did not move the map")
    }

    @Test
    fun panControllerMovesMap() {
        AppSettings.showEnemyPathfinding.value = true
        val controller = MapPanController()
        composeTestRule.setContent {
            GameGrid(
                gameState = buildGameState(),
                selectedDefenderType = null,
                selectedDefenderId = null,
                selectedAttackerId = 1,
                selectedTargetId = null,
                selectedTargetPosition = null,
                selectedMineAction = null,
                onCellClick = { },
                mapPanController = controller,
            )
        }
        composeTestRule.waitForIdle()

        val before = composeTestRule.onAllNodesWithText("PATH").fetchSemanticsNodes().first().boundsInRoot
        composeTestRule.runOnIdle { controller.pan(0f, -200f) }
        composeTestRule.waitForIdle()
        val after = composeTestRule.onAllNodesWithText("PATH").fetchSemanticsNodes().first().boundsInRoot

        assertTrue(before.top != after.top, "MapPanController did not move the map")
    }
}
