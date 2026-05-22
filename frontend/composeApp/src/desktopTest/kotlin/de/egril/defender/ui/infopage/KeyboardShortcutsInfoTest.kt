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
import kotlin.test.assertEquals

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
            composeTestRule.onNodeWithTag("shortcut-binding-upgrade-selected-tower").assertExists()
            composeTestRule.onNodeWithTag("shortcut-binding-undo-or-sell-selected-tower").assertExists()
            composeTestRule.onNodeWithTag("shortcut-binding-toggle-spell-menu").assertExists()
            composeTestRule.onNodeWithTag("shortcut-binding-switch-to-tower-mode").assertExists()
            composeTestRule.onNodeWithText("Up").assertExists()
            composeTestRule.onNodeWithText("Down").assertExists()
            composeTestRule.onNodeWithText("Left").assertExists()
            composeTestRule.onNodeWithText("Right").assertExists()
            composeTestRule.onAllNodesWithText("KEY:", substring = true, ignoreCase = false)
                .assertCountEquals(0)
            // Verify keyboard-only workflow guide section is present
            composeTestRule.onNodeWithText("Keyboard-only Gameplay Guide").assertExists()
            composeTestRule.onNodeWithText("Build Phase (before battle)").assertExists()
            composeTestRule.onNodeWithText("Battle Phase (your turn)").assertExists()
            composeTestRule.onNodeWithText("Map Navigation").assertExists()

            ScreenshotTestUtils.captureScreenshot(
                composeTestRule = composeTestRule,
                filename = "keyboard-shortcuts-workflow-guide",
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
            composeTestRule.runOnIdle {
                assertEquals("T", AppSettings.shortcutCenterSelectedTower.value)
                assertEquals("Y", AppSettings.shortcutCenterNextSpawnPoint.value)
            }

            try {
                ScreenshotTestUtils.captureScreenshot(
                    composeTestRule = composeTestRule,
                    filename = "keyboard-shortcuts-remapped-keys",
                    width = 1200,
                    height = 900
                )
            } catch (_: Throwable) {
                // screenshot capture can fail in multi-root desktop test environments
            }
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

            composeTestRule.onNodeWithTag("shortcut-capture-target").performKeyInput {
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
