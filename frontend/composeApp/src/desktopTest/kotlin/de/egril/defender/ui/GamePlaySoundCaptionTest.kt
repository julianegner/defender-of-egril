package de.egril.defender.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.hyperether.resources.AppLocale
import com.hyperether.resources.currentLanguage
import de.egril.defender.audio.GlobalSoundManager
import de.egril.defender.audio.SoundEvent
import de.egril.defender.editor.EditorStorage
import de.egril.defender.game.LevelData
import de.egril.defender.model.GameState
import de.egril.defender.ui.gameplay.GamePlayScreen
import de.egril.defender.ui.settings.AppSettings
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class GamePlaySoundCaptionTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private var originalCaptionsEnabled: Boolean = false

    @Before
    fun setup() {
        currentLanguage.value = AppLocale.DEFAULT
        EditorStorage.ensureInitialized()
        originalCaptionsEnabled = AppSettings.captionsEnabled.value
    }

    @After
    fun restoreSettings() {
        AppSettings.saveCaptionsEnabled(originalCaptionsEnabled)
    }

    @Test
    fun gameplayShowsSoundCaptionWhenCaptionsAreEnabled() {
        AppSettings.saveCaptionsEnabled(true)
        val level = LevelData.createLevels().first { it.id == 1 }
        val gameState = GameState(level)

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

        composeTestRule.runOnIdle {
            GlobalSoundManager.playSound(SoundEvent.ENEMY_SPAWN)
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("gameplaySoundCaption").assertExists()
        ScreenshotTestUtils.captureScreenshot(
            composeTestRule = composeTestRule,
            filename = "gameplay-sound-caption",
        )
    }

    @Test
    fun gameplayDoesNotShowSoundCaptionWhenCaptionsAreDisabled() {
        AppSettings.saveCaptionsEnabled(false)
        val level = LevelData.createLevels().first { it.id == 1 }
        val gameState = GameState(level)

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

        composeTestRule.runOnIdle {
            GlobalSoundManager.playSound(SoundEvent.ENEMY_SPAWN)
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("gameplaySoundCaption").assertDoesNotExist()
    }
}
