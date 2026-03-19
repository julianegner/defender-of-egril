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
 */
class FinalCreditsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setDefaultLanguage() {
        currentLanguage.value = AppLocale.DEFAULT
    }

    @Test
    fun testFinalCreditsScreenRendersTitle() {
        composeTestRule.setContent {
            FinalCreditsScreen(onDismiss = {})
        }

        composeTestRule.waitForIdle()

        // The game title should be visible
        composeTestRule.onNodeWithText("Defender of Egril", substring = true)
            .assertExists()
    }

    @Test
    fun testFinalCreditsScreenRendersDevelopersSection() {
        composeTestRule.setContent {
            FinalCreditsScreen(onDismiss = {})
        }

        composeTestRule.waitForIdle()

        // Developers section header should be present
        composeTestRule.onNodeWithText("Developers", substring = true, ignoreCase = true)
            .assertExists()

        // At least one developer name should appear
        FinalCreditsData.developers.forEach { dev ->
            composeTestRule.onNodeWithText(dev, substring = true)
                .assertExists()
        }
    }

    @Test
    fun testFinalCreditsScreenRendersSoundSection() {
        composeTestRule.setContent {
            FinalCreditsScreen(onDismiss = {})
        }

        composeTestRule.waitForIdle()

        // Sound Effects section header should be present
        composeTestRule.onNodeWithText("Sound Effects", substring = true, ignoreCase = true)
            .assertExists()
    }

    @Test
    fun testFinalCreditsScreenRendersMusicSection() {
        composeTestRule.setContent {
            FinalCreditsScreen(onDismiss = {})
        }

        composeTestRule.waitForIdle()

        // Background Music section header should be present
        composeTestRule.onNodeWithText("Background Music", substring = true, ignoreCase = true)
            .assertExists()
    }

    @Test
    fun testFinalCreditsScreenDismissCallback() {
        var dismissed = false

        composeTestRule.setContent {
            FinalCreditsScreen(onDismiss = { dismissed = true })
        }

        composeTestRule.waitForIdle()

        // Click the screen to dismiss
        composeTestRule.onRoot().performClick()

        composeTestRule.waitForIdle()

        assert(dismissed) { "Clicking the credits screen should invoke onDismiss" }
    }

    @Test
    fun testFinalCreditsScreenScreenshot() {
        composeTestRule.setContent {
            FinalCreditsScreen(onDismiss = {})
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
