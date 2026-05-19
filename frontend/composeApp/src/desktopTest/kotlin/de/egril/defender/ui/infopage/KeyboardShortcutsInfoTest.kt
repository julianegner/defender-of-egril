package de.egril.defender.ui.infopage

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.hyperether.resources.AppLocale
import com.hyperether.resources.currentLanguage
import de.egril.defender.ui.ScreenshotTestUtils
import org.junit.Rule
import org.junit.Test

class KeyboardShortcutsInfoTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun keyboardShortcutsIncludesCenterSelectedTowerShortcut() {
        currentLanguage.value = AppLocale.DEFAULT
        composeTestRule.setContent {
            KeyboardShortcutsInfo()
        }

        composeTestRule.waitForIdle()
        composeTestRule.onRoot().assertExists()
        composeTestRule.onNodeWithText("Center map on selected tower").assertExists()
        composeTestRule.onNodeWithText("???").assertDoesNotExist()

        ScreenshotTestUtils.captureScreenshot(
            composeTestRule = composeTestRule,
            filename = "keyboard-shortcuts-center-selected-tower",
            width = 1200,
            height = 900
        )
    }
}
