package de.egril.defender.ui.infopage

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.hyperether.resources.AppLocale
import com.hyperether.resources.currentLanguage
import de.egril.defender.ui.ScreenshotTestUtils
import de.egril.defender.ui.settings.AppSettings
import org.junit.Rule
import org.junit.Test

class KeyboardShortcutsInfoTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun keyboardShortcutsIncludesCenterSelectedTowerShortcut() {
        AppSettings.resetToDefaults()
        try {
            currentLanguage.value = AppLocale.DEFAULT
            composeTestRule.setContent {
                KeyboardShortcutsInfo()
            }

            composeTestRule.waitForIdle()
            composeTestRule.onRoot().assertExists()
            composeTestRule.onNodeWithText("Center map on selected tower").assertExists()
            composeTestRule.onNodeWithText("Center map on next spawn point").assertExists()
            composeTestRule.onNodeWithText("???").assertDoesNotExist()

            ScreenshotTestUtils.captureScreenshot(
                composeTestRule = composeTestRule,
                filename = "keyboard-shortcuts-phase4-panzoom-assist",
                width = 1200,
                height = 900
            )
        } finally {
            AppSettings.resetToDefaults()
        }
    }

    @Test
    fun keyboardShortcutsUsesConfiguredRemapKeys() {
        AppSettings.resetToDefaults()
        try {
            AppSettings.saveShortcutCenterSelectedTower("T")
            AppSettings.saveShortcutCenterNextSpawnPoint("Y")

            composeTestRule.setContent {
                KeyboardShortcutsInfo()
            }

            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText("T").assertExists()
            composeTestRule.onNodeWithText("Y").assertExists()

            ScreenshotTestUtils.captureScreenshot(
                composeTestRule = composeTestRule,
                filename = "keyboard-shortcuts-remapped-keys",
                width = 1200,
                height = 900
            )
        } finally {
            AppSettings.resetToDefaults()
        }
    }
}
