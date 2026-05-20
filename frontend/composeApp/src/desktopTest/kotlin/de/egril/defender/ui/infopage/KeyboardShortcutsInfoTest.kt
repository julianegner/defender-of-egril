package de.egril.defender.ui.infopage

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.ExperimentalTestApi
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
                KeyboardShortcutsInfo(enableBindingEdit = true)
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
                KeyboardShortcutsInfo(enableBindingEdit = true, showResetButton = true)
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

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun keyboardShortcutsSupportsKeyboardOnlyRebindingAndReset() {
        AppSettings.resetToDefaults()
        try {
            composeTestRule.setContent {
                KeyboardShortcutsInfo(enableBindingEdit = true, showResetButton = true)
            }

            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("shortcut-binding-center-selected")
                .performClick()

            composeTestRule.onAllNodes(isRoot()).onFirst().performKeyInput {
                keyDown(androidx.compose.ui.input.key.Key.CtrlLeft)
                pressKey(androidx.compose.ui.input.key.Key.S)
                keyUp(androidx.compose.ui.input.key.Key.CtrlLeft)
            }

            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText("Ctrl+S").assertExists()

            composeTestRule.onNodeWithText("Reset all shortcut bindings", substring = true, ignoreCase = true)
                .performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText("R").assertExists()
        } finally {
            AppSettings.resetToDefaults()
        }
    }
}
