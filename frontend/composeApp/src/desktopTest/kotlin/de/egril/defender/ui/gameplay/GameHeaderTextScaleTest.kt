package de.egril.defender.ui.gameplay

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.hyperether.resources.AppLocale
import com.hyperether.resources.currentLanguage
import de.egril.defender.editor.EditorStorage
import de.egril.defender.game.LevelData
import de.egril.defender.model.GamePhase
import de.egril.defender.model.GameState
import de.egril.defender.ui.ScreenshotTestUtils
import de.egril.defender.ui.settings.AppSettings
import de.egril.defender.ui.settings.HeaderTextSize
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertTrue

class GameHeaderTextScaleTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        currentLanguage.value = AppLocale.DEFAULT
        EditorStorage.ensureInitialized()
    }

    @After
    fun tearDown() {
        AppSettings.saveHeaderTextSize(HeaderTextSize.DEFAULT)
    }

    @Test
    fun mapButtonGrowsWhenHeaderTextSizeIsLarge() {
        val gameState = createTestGameState()

        AppSettings.saveHeaderTextSize(HeaderTextSize.SMALL)
        composeTestRule.setContent {
            Column {
                GameHeader(
                    gameState = gameState,
                    showOverlay = false,
                    onShowOverlayChange = {},
                    onBackToMap = {},
                    onSaveGame = null,
                    onCheatCode = null,
                )
            }
        }
        composeTestRule.waitForIdle()
        val smallBounds = composeTestRule.onNodeWithText("Map").getUnclippedBoundsInRoot()
        val smallHeight = smallBounds.bottom - smallBounds.top

        AppSettings.saveHeaderTextSize(HeaderTextSize.LARGE)
        composeTestRule.setContent {
            Column {
                GameHeader(
                    gameState = gameState,
                    showOverlay = false,
                    onShowOverlayChange = {},
                    onBackToMap = {},
                    onSaveGame = null,
                    onCheatCode = null,
                )
            }
        }
        composeTestRule.waitForIdle()
        val largeBounds = composeTestRule.onNodeWithText("Map").getUnclippedBoundsInRoot()
        val largeHeight = largeBounds.bottom - largeBounds.top

        assertTrue(largeHeight > smallHeight, "Map button should grow at larger header text scale")
    }

    @Test
    fun captureHeaderAtLargeTextScale() {
        val gameState = createTestGameState()
        AppSettings.saveHeaderTextSize(HeaderTextSize.LARGE)

        composeTestRule.setContent {
            Column {
                GameHeader(
                    gameState = gameState,
                    showOverlay = false,
                    onShowOverlayChange = {},
                    onBackToMap = {},
                    onSaveGame = null,
                    onCheatCode = null,
                )
            }
        }
        composeTestRule.waitForIdle()

        ScreenshotTestUtils.captureScreenshot(
            composeTestRule = composeTestRule,
            filename = "game-header-large-scale",
        )
    }

    @Test
    fun captureEnemyListButtonCenteredIcon() {
        val gameState = createTestGameState()
        AppSettings.saveHeaderTextSize(HeaderTextSize.DEFAULT)

        composeTestRule.setContent {
            Column {
                GameHeader(
                    gameState = gameState,
                    showOverlay = false,
                    onShowOverlayChange = {},
                    onBackToMap = {},
                    onSaveGame = null,
                    onCheatCode = null,
                )
            }
        }
        composeTestRule.waitForIdle()

        ScreenshotTestUtils.captureScreenshot(
            composeTestRule = composeTestRule,
            filename = "game-header-enemy-list-button-centered",
        )
    }

    private fun createTestGameState(): GameState {
        val level = LevelData.createLevels().first { it.id == 1 }
        return GameState(level).also {
            it.phase.value = GamePhase.PLAYER_TURN
        }
    }
}
