package de.egril.defender.ui

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import com.hyperether.resources.AppLocale
import com.hyperether.resources.currentLanguage
import de.egril.defender.editor.EditorStorage
import de.egril.defender.game.LevelData
import de.egril.defender.model.DefenderType
import de.egril.defender.model.GameState
import de.egril.defender.model.InfoType
import de.egril.defender.ui.gameplay.GamePlayScreen
import org.junit.Rule
import org.junit.Test

class SpecialInfoFocusTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun mapKeepsFocusAfterSpecialTowersInfoIsDismissed() {
        currentLanguage.value = AppLocale.DEFAULT
        EditorStorage.ensureInitialized()
        val level =
            LevelData
                .createLevels()
                .maxBy { it.gridWidth * it.gridHeight }
                .copy(availableTowers = setOf(DefenderType.WIZARD_TOWER))
        val gameState = GameState(level)
        gameState.infoState.value = gameState.infoState.value.showInfo(InfoType.SPECIAL_TOWERS_INFO)
        composeTestRule.setContent {
            GamePlayScreen(
                gameState = gameState,
                onPlaceDefender = { _, _ -> true },
                onUpgradeDefender = { true },
                onUndoTower = { true },
                onSellTower = { true },
                onStartFirstPlayerTurn = {},
                onDefenderAttack = { _, _ -> true },
                onDefenderAttackPosition = { _, _ -> true },
                onEndPlayerTurn = {},
                onAutoAttackAndEndTurn = {},
                onBackToMap = {},
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Got it!").performClick()
        composeTestRule.waitForIdle()

        // Arrow keys are only handled by the focused element, so something must hold focus after the info box closes
        composeTestRule.onNode(isFocused()).assertExists("No element has focus after dismissing the info box")
        composeTestRule.onRoot().performKeyInput { pressKey(Key.DirectionDown) }
        composeTestRule.waitForIdle()
    }
}
