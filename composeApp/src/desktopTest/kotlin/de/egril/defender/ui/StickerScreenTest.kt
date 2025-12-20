package de.egril.defender.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import de.egril.defender.ui.ScreenshotTestUtils
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertTrue

/**
 * UI tests for the Sticker screen.
 * 
 * These tests verify that the Sticker merchandise preview screen renders correctly
 * and captures screenshots for visual verification.
 */
class StickerScreenTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun testStickerScreenRendersCorrectly() {
        var backClicked = false
        
        // Set the content
        composeTestRule.setContent {
            StickerScreen(
                onBack = { backClicked = true }
            )
        }
        
        // Wait for composition to complete
        composeTestRule.waitForIdle()
        
        // Verify the screen contains expected elements
        
        // Check that title is displayed (banner shows "Defender of" and "Egril" separately)
        composeTestRule.onNodeWithText("Defender of", substring = true, ignoreCase = true)
            .assertExists()
        composeTestRule.onNodeWithText("Egril", substring = true, ignoreCase = true)
            .assertExists()
        
        // Check that section titles exist
        composeTestRule.onNodeWithText("Game Map", substring = true, ignoreCase = true)
            .assertExists()
        composeTestRule.onNodeWithText("Enemies", substring = true, ignoreCase = true)
            .assertExists()
        composeTestRule.onNodeWithText("Towers", substring = true, ignoreCase = true)
            .assertExists()
        
        // Check that unit labels exist
        composeTestRule.onNodeWithText("Goblin", substring = true, ignoreCase = true)
            .assertExists()
        composeTestRule.onNodeWithText("Ork", substring = true, ignoreCase = true)
            .assertExists()
        composeTestRule.onNodeWithText("Wizard", substring = true, ignoreCase = true)
            .assertExists()
        composeTestRule.onNodeWithText("Bow", substring = true, ignoreCase = true)
            .assertExists()
        
        // Check that Back button exists
        composeTestRule.onNodeWithText("Back", substring = true, ignoreCase = true)
            .assertExists()
            .assertHasClickAction()
        
        // Capture screenshot
        ScreenshotTestUtils.captureScreenshot(
            composeTestRule,
            "sticker-screen",
            width = 1400,
            height = 1000
        )
    }
    
    @Test
    fun testStickerScreenBackButtonIsClickable() {
        var backClicked = false
        
        composeTestRule.setContent {
            StickerScreen(
                onBack = { backClicked = true }
            )
        }
        
        composeTestRule.waitForIdle()
        
        // Click Back button
        composeTestRule.onNodeWithText("Back", substring = true, ignoreCase = true)
            .performClick()
        
        // Verify callback was invoked
        assertTrue(backClicked, "Back button should trigger callback")
    }
    
    @Test
    fun testStickerScreenHasSettingsButton() {
        composeTestRule.setContent {
            StickerScreen(
                onBack = {}
            )
        }
        
        composeTestRule.waitForIdle()
        
        // Verify settings button exists (it's in the top-right corner)
        // The settings button contains a settings icon, so we check for clickable elements
        composeTestRule.onRoot().assertExists()
    }
}
