package de.egril.defender.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import de.egril.defender.ui.settings.SettingsDialog
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for the Settings Dialog.
 * 
 * These tests verify that the Settings Dialog renders correctly
 * and captures screenshots for visual verification.
 */
class SettingsDialogTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun testSettingsDialogRendersCorrectly() {
        var dismissClicked = false
        
        composeTestRule.setContent {
            SettingsDialog(
                onDismiss = { dismissClicked = true }
            )
        }
        
        composeTestRule.waitForIdle()
        
        // Verify the dialog title is displayed - use assertIsDisplayed instead of assertExists
        // to avoid matching both "Settings" title and "Reset Settings" button
        composeTestRule.onNodeWithText("Settings", substring = false, ignoreCase = false)
            .assertIsDisplayed()
        
        // Verify language section is displayed
        composeTestRule.onNodeWithText("Language", substring = true, ignoreCase = true)
            .assertExists()
        
        // Verify close button is displayed
        composeTestRule.onNodeWithText("Close", substring = true, ignoreCase = true)
            .assertExists()
            .assertHasClickAction()
        
        // Capture screenshot - wrap in try/catch as dialogs may have multiple roots
        try {
            ScreenshotTestUtils.captureScreenshot(
                composeTestRule,
                "settings-dialog",
                width = 600,
                height = 500
            )
        } catch (e: Throwable) {
            println("Note: Could not capture screenshot for dialog (expected): ${e.message}")
        }
    }
    
    @Test
    fun testSettingsDialogCloseButton() {
        var dismissClicked = false
        
        composeTestRule.setContent {
            SettingsDialog(
                onDismiss = { dismissClicked = true }
            )
        }
        
        composeTestRule.waitForIdle()
        
        // Click close button
        composeTestRule.onNodeWithText("Close", substring = true, ignoreCase = true)
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Verify callback was invoked
        assert(dismissClicked) { "Close button should trigger dismiss callback" }
    }
    
    @Test
    fun testSettingsDialogHasLanguageChooser() {
        composeTestRule.setContent {
            SettingsDialog(
                onDismiss = {}
            )
        }
        
        composeTestRule.waitForIdle()
        
        // Verify the dialog contains language-related text
        composeTestRule.onNodeWithText("Language", substring = true, ignoreCase = true)
            .assertExists()
        
        // The language chooser should display flags and language names
        // We verify the dialog is displayed by checking for the language text
        composeTestRule.onNodeWithText("Language", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }
}
