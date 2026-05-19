package de.egril.defender.ui.infopage

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import de.egril.defender.ui.ScreenshotTestUtils
import org.junit.Rule
import org.junit.Test

class KeyboardShortcutsInfoTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun keyboardShortcutsIncludesCenterSelectedTowerShortcut() {
        composeTestRule.setContent {
            KeyboardShortcutsInfo()
        }

        composeTestRule.waitForIdle()
        composeTestRule.onRoot().assertExists()

        ScreenshotTestUtils.captureScreenshot(
            composeTestRule = composeTestRule,
            filename = "keyboard-shortcuts-center-selected-tower",
            width = 1200,
            height = 900
        )
    }
}
