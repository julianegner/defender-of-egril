package de.egril.defender.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.hyperether.resources.AppLocale
import com.hyperether.resources.currentLanguage
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for the Final Credits screen.
 *
 * Verifies that the credits screen renders with the expected sections
 * and captures a screenshot for visual verification.
 *
 * Note: Tests pass `animationsEnabled = false` to [FinalCreditsScreen].
 * The production screen contains a `while(true)` background-animation loop and a
 * 60-second auto-scroll animation. These animations keep requesting the next Compose
 * frame, so [androidx.compose.ui.test.ComposeTestRule.setContent] — which calls
 * waitForIdle() internally — would never return with animations enabled.
 * Disabling animations removes all infinite loops from the composition.
 */
class FinalCreditsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        currentLanguage.value = AppLocale.DEFAULT
    }

    @Test
    fun testFinalCreditsScreenRendersTitle() {
        composeTestRule.setContent {
            FinalCreditsScreen(onDismiss = {}, animationsEnabled = false)
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Defender of Egril", substring = true)
            .assertExists()
    }

    @Test
    fun testFinalCreditsScreenRendersDevelopersSection() {
        composeTestRule.setContent {
            FinalCreditsScreen(onDismiss = {}, animationsEnabled = false)
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Developers", substring = true, ignoreCase = true)
            .assertExists()

        FinalCreditsData.developers.forEach { dev ->
            composeTestRule.onNodeWithText(dev, substring = true)
                .assertExists()
        }
    }

    @Test
    fun testFinalCreditsScreenRendersSoundSection() {
        composeTestRule.setContent {
            FinalCreditsScreen(onDismiss = {}, animationsEnabled = false)
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Sound Effects", substring = true, ignoreCase = true)
            .assertExists()
    }

    @Test
    fun testFinalCreditsScreenRendersMusicSection() {
        composeTestRule.setContent {
            FinalCreditsScreen(onDismiss = {}, animationsEnabled = false)
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Background Music", substring = true, ignoreCase = true)
            .assertExists()
    }

    @Test
    fun testFinalCreditsScreenDismissCallback() {
        var dismissed = false

        composeTestRule.setContent {
            FinalCreditsScreen(onDismiss = { dismissed = true }, animationsEnabled = false)
        }

        composeTestRule.waitForIdle()

        composeTestRule.onRoot().performClick()

        composeTestRule.waitForIdle()

        assert(dismissed) { "Clicking the credits screen should invoke onDismiss" }
    }

    @Test
    fun testFinalCreditsScreenScreenshot() {
        composeTestRule.setContent {
            FinalCreditsScreen(onDismiss = {}, animationsEnabled = false)
        }

        composeTestRule.waitForIdle()

        ScreenshotTestUtils.captureScreenshot(
            composeTestRule,
            "final-credits-screen",
            width = 1200,
            height = 800
        )
    }
}
