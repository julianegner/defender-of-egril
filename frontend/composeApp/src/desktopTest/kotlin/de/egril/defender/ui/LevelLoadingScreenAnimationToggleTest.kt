package de.egril.defender.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import de.egril.defender.ui.gameplay.LevelLoadingScreen
import de.egril.defender.ui.settings.AppSettings
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class LevelLoadingScreenAnimationToggleTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private var originalAnimationsEnabled: Boolean = true

    @Before
    fun storeOriginalSetting() {
        originalAnimationsEnabled = AppSettings.enableAnimations.value
    }

    @After
    fun restoreAnimationsSetting() {
        AppSettings.saveEnableAnimations(originalAnimationsEnabled)
    }

    @Test
    fun levelLoadingScreenShowsStaticCenterWhenAnimationsDisabled() {
        AppSettings.saveEnableAnimations(false)

        composeTestRule.setContent {
            LevelLoadingScreen()
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("levelLoadingStaticCenter", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithTag("levelLoadingSpinner", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun levelLoadingScreenShowsSpinnerWhenAnimationsEnabled() {
        AppSettings.saveEnableAnimations(true)

        composeTestRule.setContent {
            LevelLoadingScreen()
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("levelLoadingSpinner", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithTag("levelLoadingStaticCenter", useUnmergedTree = true).assertDoesNotExist()
    }
}
