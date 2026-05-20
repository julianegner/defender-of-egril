package de.egril.defender.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.hyperether.resources.AppLocale
import com.hyperether.resources.currentLanguage
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

        // Verify additional settings tabs are displayed
        composeTestRule.onNodeWithText("Accessibility", substring = true, ignoreCase = true)
            .assertExists()
        composeTestRule.onNodeWithText("Shortcuts", substring = true, ignoreCase = true)
            .assertExists()
        
        // Verify close button is displayed (icon button with content description)
        composeTestRule.onNodeWithContentDescription("Close", substring = true, ignoreCase = true)
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
        
        // Click close button (icon button with content description)
        composeTestRule.onNodeWithContentDescription("Close", substring = true, ignoreCase = true)
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

    @Test
    fun testSettingsDialogShortcutRemapScreenshot() {
        currentLanguage.value = AppLocale.DEFAULT
        composeTestRule.setContent {
            SettingsDialog(onDismiss = {})
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Shortcuts", substring = true, ignoreCase = true)
            .performClick()
        composeTestRule.waitForIdle()

        try {
            ScreenshotTestUtils.captureScreenshot(
                composeTestRule,
                "settings-dialog-shortcut-remap",
                width = 700,
                height = 700
            )
        } catch (e: Throwable) {
            println("Note: Could not capture screenshot for dialog (expected): ${e.message}")
        }
    }

    @Test
    fun testSettingsDialogAccessibilityInfoTexts() {
        composeTestRule.setContent {
            SettingsDialog(onDismiss = {})
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Accessibility", substring = true, ignoreCase = true)
            .performClick()

        composeTestRule.onNodeWithText("Increases contrast", substring = true, ignoreCase = true)
            .assertExists()
        composeTestRule.onNodeWithText("Shows text hints", substring = true, ignoreCase = true)
            .assertExists()
        composeTestRule.onNodeWithText("Requires holding", substring = true, ignoreCase = true)
            .assertExists()

        try {
            ScreenshotTestUtils.captureScreenshot(
                composeTestRule,
                "settings-dialog-accessibility-tab",
                width = 700,
                height = 700
            )
        } catch (e: Throwable) {
            println("Note: Could not capture screenshot for dialog (expected): ${e.message}")
        }
    }
}
