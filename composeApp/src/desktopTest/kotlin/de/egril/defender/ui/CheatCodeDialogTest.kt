package de.egril.defender.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for the Cheat Code Dialog.
 * 
 * These tests verify that the Cheat Code Dialog renders correctly
 * and that the input field is automatically focused when the dialog opens.
 */
class CheatCodeDialogTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun testCheatCodeDialogRendersCorrectly() {
        var dismissClicked = false
        var appliedCheatCode = ""
        
        composeTestRule.setContent {
            CheatCodeDialog(
                onDismiss = { dismissClicked = true },
                onApplyCheatCode = { code ->
                    appliedCheatCode = code
                    code == "cash" // Return true for "cash" cheat code
                }
            )
        }
        
        composeTestRule.waitForIdle()
        
        // Verify the dialog title is displayed
        composeTestRule.onNodeWithText("Cheat Code", substring = false, ignoreCase = false)
            .assertIsDisplayed()
        
        // Verify instruction text is displayed
        composeTestRule.onNodeWithText("Enter cheat code:", substring = true, ignoreCase = true)
            .assertExists()
        
        // Verify the text field exists
        composeTestRule.onNode(hasSetTextAction())
            .assertExists()
        
        // Verify apply button is displayed
        composeTestRule.onNodeWithText("Apply", substring = true, ignoreCase = true)
            .assertExists()
            .assertHasClickAction()
        
        // Verify cancel button is displayed
        composeTestRule.onNodeWithText("Cancel", substring = true, ignoreCase = true)
            .assertExists()
            .assertHasClickAction()
    }
    
    @Test
    fun testCheatCodeDialogInputFieldIsFocused() {
        composeTestRule.setContent {
            CheatCodeDialog(
                onDismiss = {},
                onApplyCheatCode = { true }
            )
        }
        
        composeTestRule.waitForIdle()
        
        // The text field should be focused, which means it should accept text input
        // We can verify this by checking that the text field exists and is ready for input
        val textField = composeTestRule.onNode(hasSetTextAction())
        textField.assertExists()
        textField.assertIsDisplayed()
        
        // Verify we can type into the field without needing to click it first
        // If autofocus works, this should succeed
        textField.performTextInput("cash")
        
        composeTestRule.waitForIdle()
        
        // Verify the text was entered
        textField.assertTextEquals("cash")
    }
    
    @Test
    fun testCheatCodeDialogApplyButton() {
        var appliedCheatCode = ""
        var dismissClicked = false
        
        composeTestRule.setContent {
            CheatCodeDialog(
                onDismiss = { dismissClicked = true },
                onApplyCheatCode = { code ->
                    appliedCheatCode = code
                    true // Return true to indicate success
                }
            )
        }
        
        composeTestRule.waitForIdle()
        
        // Type a cheat code
        composeTestRule.onNode(hasSetTextAction())
            .performTextInput("cash")
        
        composeTestRule.waitForIdle()
        
        // Click apply button
        composeTestRule.onNodeWithText("Apply", substring = true, ignoreCase = true)
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Verify the cheat code was applied
        assert(appliedCheatCode == "cash") { "Expected 'cash', got '$appliedCheatCode'" }
        
        // Verify dialog was dismissed after successful application
        assert(dismissClicked) { "Dialog should be dismissed after applying valid cheat code" }
    }
    
    @Test
    fun testCheatCodeDialogCancelButton() {
        var dismissClicked = false
        var appliedCheatCode = ""
        
        composeTestRule.setContent {
            CheatCodeDialog(
                onDismiss = { dismissClicked = true },
                onApplyCheatCode = { code ->
                    appliedCheatCode = code
                    false
                }
            )
        }
        
        composeTestRule.waitForIdle()
        
        // Click cancel button
        composeTestRule.onNodeWithText("Cancel", substring = true, ignoreCase = true)
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Verify callback was invoked
        assert(dismissClicked) { "Cancel button should trigger dismiss callback" }
        
        // Verify no cheat code was applied
        assert(appliedCheatCode.isEmpty()) { "No cheat code should be applied when cancel is clicked" }
    }
    
    @Test
    fun testCheatCodeDialogInvalidCheatCode() {
        var dismissClicked = false
        
        composeTestRule.setContent {
            CheatCodeDialog(
                onDismiss = { dismissClicked = true },
                onApplyCheatCode = { false } // Always return false to simulate invalid code
            )
        }
        
        composeTestRule.waitForIdle()
        
        // Type an invalid cheat code
        composeTestRule.onNode(hasSetTextAction())
            .performTextInput("invalid")
        
        composeTestRule.waitForIdle()
        
        // Click apply button
        composeTestRule.onNodeWithText("Apply", substring = true, ignoreCase = true)
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Verify error message is displayed
        composeTestRule.onNodeWithText("Invalid cheat code", substring = true, ignoreCase = true)
            .assertExists()
            .assertIsDisplayed()
        
        // Verify dialog was not dismissed
        assert(!dismissClicked) { "Dialog should remain open when invalid cheat code is entered" }
    }
}
