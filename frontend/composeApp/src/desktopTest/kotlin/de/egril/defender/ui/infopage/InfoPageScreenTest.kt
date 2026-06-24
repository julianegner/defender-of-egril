package de.egril.defender.ui.infopage

import androidx.compose.ui.test.junit4.createComposeRule
import de.egril.defender.ui.ScreenshotTestUtils
import org.junit.Rule
import org.junit.Test

class InfoPageScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun infoPageRendersAndCapturesScreenshot() {
        composeTestRule.setContent {
            InfoPageScreen(
                onBack = {},
            )
        }

        composeTestRule.waitForIdle()

        ScreenshotTestUtils.captureScreenshot(
            composeTestRule,
            "info-page-screen",
            width = 1200,
            height = 800,
        )
    }
}
