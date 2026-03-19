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
 * Note: Clock auto-advance is disabled and waitForIdle() is intentionally NOT used.
 * FinalCreditsScreen contains a while(true) background-animation loop and a 60-second
 * scroll animation. Running animations continuously register for the next frame, so
 * waitForIdle() never returns – even with autoAdvance=false.
 *
 * Instead, we advance the clock by a small fixed amount (advanceTimeBy) after setContent.
 * setContent already performs the initial composition synchronously, so all text nodes
 * are available immediately. The small clock advance flushes any pending coroutine
 * dispatch without entering the infinite animation loop.
 */
class FinalCreditsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        currentLanguage.value = AppLocale.DEFAULT
        // Disable automatic clock advancement. Combined with advanceTimeBy(100) below,
        // this lets us settle the initial composition without spinning through the
        // infinite background-image loop or 60-second scroll animation.
        composeTestRule.mainClock.autoAdvance = false
    }

    @Test
    fun testFinalCreditsScreenRendersTitle() {
        composeTestRule.setContent {
            FinalCreditsScreen(onDismiss = {})
        }

        // Advance 100 ms to flush initial effects without entering the infinite loop.
        composeTestRule.mainClock.advanceTimeBy(100)

        // The game title should be visible
        composeTestRule.onNodeWithText("Defender of Egril", substring = true)
            .assertExists()
    }

    @Test
    fun testFinalCreditsScreenRendersDevelopersSection() {
        composeTestRule.setContent {
            FinalCreditsScreen(onDismiss = {})
        }

        composeTestRule.mainClock.advanceTimeBy(100)

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

        composeTestRule.mainClock.advanceTimeBy(100)

        // Sound Effects section header should be present
        composeTestRule.onNodeWithText("Sound Effects", substring = true, ignoreCase = true)
            .assertExists()
    }

    @Test
    fun testFinalCreditsScreenRendersMusicSection() {
        composeTestRule.setContent {
            FinalCreditsScreen(onDismiss = {})
        }

        composeTestRule.mainClock.advanceTimeBy(100)

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

        composeTestRule.mainClock.advanceTimeBy(100)

        // Click the screen to dismiss
        composeTestRule.onRoot().performClick()

        // Advance time to allow the coroutineScope.launch { onDismiss() } to execute.
        composeTestRule.mainClock.advanceTimeBy(100)

        assert(dismissed) { "Clicking the credits screen should invoke onDismiss" }
    }

    @Test
    fun testFinalCreditsScreenScreenshot() {
        composeTestRule.setContent {
            FinalCreditsScreen(onDismiss = {})
        }

        composeTestRule.mainClock.advanceTimeBy(100)

        ScreenshotTestUtils.captureScreenshot(
            composeTestRule,
            "final-credits-screen",
            width = 1200,
            height = 800
        )
    }
}
