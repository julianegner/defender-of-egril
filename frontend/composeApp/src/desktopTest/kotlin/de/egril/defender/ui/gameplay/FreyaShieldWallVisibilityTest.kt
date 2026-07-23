package de.egril.defender.ui.gameplay

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import de.egril.defender.model.Attacker
import de.egril.defender.model.AttackerType
import de.egril.defender.model.AttackerWave
import de.egril.defender.model.GamePhase
import de.egril.defender.model.GameState
import de.egril.defender.model.Level
import de.egril.defender.model.Position
import org.junit.Rule
import org.junit.Test

class FreyaShieldWallVisibilityTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun flankTilesShowShieldWallOverlay() {
        val level =
            Level(
                id = 1,
                name = "Freya Shield Visibility Test",
                gridWidth = 10,
                gridHeight = 7,
                startPositions = listOf(Position(0, 3)),
                targetPositions = listOf(Position(9, 3)),
                pathCells = (0..9).map { Position(it, 3) }.toSet(),
                attackerWaves = listOf(AttackerWave(listOf(AttackerType.FALLEN_SHIELDMAIDEN_FREYA))),
                initialCoins = 100,
                healthPoints = 10,
            )
        val gameState = GameState(level)
        gameState.phase.value = GamePhase.PLAYER_TURN
        gameState.attackers.add(
            Attacker(
                id = 1,
                type = AttackerType.FALLEN_SHIELDMAIDEN_FREYA,
                position = mutableStateOf(Position(4, 3)),
                level = mutableStateOf(1),
            ),
        )

        composeTestRule.setContent {
            GameGrid(
                gameState = gameState,
                selectedDefenderType = null,
                selectedDefenderId = null,
                selectedTargetId = null,
                selectedTargetPosition = null,
                selectedMineAction = null,
                onCellClick = { },
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule
            .onAllNodesWithContentDescription("Shield Wall")
            .assertCountEquals(1)
    }
}
