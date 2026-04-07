package de.egril.defender.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import de.egril.defender.AppBuildInfo
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for the Commit Info Dialog.
 * 
 * These tests verify that the CommitInfoDialog renders correctly
 * and displays commit information properly.
 */
class CommitInfoDialogTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun testCommitInfoDialogRendersCorrectly() {
        // Set the content
        composeTestRule.setContent {
            CommitInfoDialog(
                onDismiss = {}
            )
        }
        
        // Wait for composition to complete
        composeTestRule.waitForIdle()
        
        // Verify the dialog title is displayed
        composeTestRule.onNodeWithText("Build Information", substring = true, ignoreCase = true)
            .assertExists()
        
        // Verify commit hash label and value are displayed
        composeTestRule.onNodeWithText("Commit Hash", substring = true, ignoreCase = true)
            .assertExists()
        composeTestRule.onNodeWithText(AppBuildInfo.COMMIT_HASH, substring = true)
            .assertExists()
        
        // Verify commit date label and value are displayed
        composeTestRule.onNodeWithText("Commit Date", substring = true, ignoreCase = true)
            .assertExists()
        composeTestRule.onNodeWithText(AppBuildInfo.COMMIT_DATE, substring = true)
            .assertExists()
        
        // Verify commit message label and value are displayed
        composeTestRule.onNodeWithText("Commit Message", substring = true, ignoreCase = true)
            .assertExists()
        composeTestRule.onNodeWithText(AppBuildInfo.COMMIT_MESSAGE, substring = true)
            .assertExists()
        
        // Verify close button exists
        composeTestRule.onNodeWithText("Close", substring = true, ignoreCase = true)
            .assertExists()
            .assertHasClickAction()
    }
    
    @Test
    fun testCommitInfoDialogCanBeDismissed() {
        var dismissed = false
        
        // Set the content
        composeTestRule.setContent {
            CommitInfoDialog(
                onDismiss = { dismissed = true }
            )
        }
        
        composeTestRule.waitForIdle()
        
        // Click the close button
        composeTestRule.onNodeWithText("Close", substring = true, ignoreCase = true)
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Verify callback was invoked
        assert(dismissed) { "Close button should trigger onDismiss callback" }
    }
}
