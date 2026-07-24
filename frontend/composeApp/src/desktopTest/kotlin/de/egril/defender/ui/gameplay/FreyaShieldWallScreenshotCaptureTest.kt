package de.egril.defender.ui.gameplay

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import de.egril.defender.model.Attacker
import de.egril.defender.model.AttackerType
import de.egril.defender.model.AttackerWave
import de.egril.defender.model.GamePhase
import de.egril.defender.model.GameState
import de.egril.defender.model.Level
import de.egril.defender.model.Position
import de.egril.defender.ui.ScreenshotTestUtils
import org.junit.Rule
import org.junit.Test

class FreyaShieldWallScreenshotCaptureTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun captureFreyaShieldWallOnLevelMap() {
        val level =
            Level(
                id = 1,
                name = "Freya Shield Screenshot Test",
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
        gameState.attackers.add(
            Attacker(
                id = 2,
                type = AttackerType.SKELETON,
                position = mutableStateOf(Position(4, 2)),
                level = mutableStateOf(1),
            ),
        )
        gameState.attackers.add(
            Attacker(
                id = 3,
                type = AttackerType.SKELETON,
                position = mutableStateOf(Position(4, 4)),
                level = mutableStateOf(1),
            ),
        )

        composeTestRule.setContent {
            Box(modifier = Modifier.size(1200.dp, 900.dp)) {
                GameGrid(
                    gameState = gameState,
                    selectedDefenderType = null,
                    selectedDefenderId = null,
                    selectedTargetId = null,
                    selectedTargetPosition = null,
                    selectedMineAction = null,
                    onCellClick = { },
                    modifier = Modifier.fillMaxSize(),
                    scrollToPosition = Position(4, 3),
                    onScrollToPositionConsumed = { },
                )
            }
        }

        composeTestRule.waitForIdle()

        ScreenshotTestUtils.captureScreenshot(
            composeTestRule,
            "freya-shield-wall-level-map",
            width = 1200,
            height = 900,
        )
    }
}
