package de.egril.defender.ui.gameplay

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for the NarrativeMessageDialog composable.
 *
 * These tests verify that the dialog renders correctly for both EWHAD and STORY
 * message types, shows the correct title and body text, and that the dismiss
 * button works properly.
 */
class NarrativeMessageDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ── EWHAD type ──────────────────────────────────────────────────────────

    @Test
    fun testEwhadDialogShowsTitle() {
        composeTestRule.setContent {
            NarrativeMessageDialog(
                type = NarrativeMessageType.EWHAD,
                title = "Ewhad enters the battlefield",
                text = "Ewhad has entered the Battlefield!",
                onDismiss = {}
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Ewhad enters the battlefield", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun testEwhadDialogShowsBodyText() {
        composeTestRule.setContent {
            NarrativeMessageDialog(
                type = NarrativeMessageType.EWHAD,
                title = "Ewhad retreats",
                text = "You have forced Ewhad to retreat!",
                onDismiss = {}
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("You have forced Ewhad to retreat!", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun testEwhadDialogDismissButtonExists() {
        composeTestRule.setContent {
            NarrativeMessageDialog(
                type = NarrativeMessageType.EWHAD,
                title = "Ewhad is defeated",
                text = "You defeated Ewhad!",
                onDismiss = {}
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("OK", substring = true, ignoreCase = true)
            .assertExists()
            .assertHasClickAction()
    }

    @Test
    fun testEwhadDialogCallsOnDismissWhenButtonClicked() {
        var dismissed = false

        composeTestRule.setContent {
            NarrativeMessageDialog(
                type = NarrativeMessageType.EWHAD,
                title = "Ewhad is defeated",
                text = "You defeated Ewhad!",
                onDismiss = { dismissed = true }
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("OK", substring = true, ignoreCase = true)
            .performClick()

        composeTestRule.waitForIdle()

        assert(dismissed) { "onDismiss should be called when OK is clicked" }
    }

    // ── STORY type ──────────────────────────────────────────────────────────

    @Test
    fun testStoryDialogShowsTitle() {
        composeTestRule.setContent {
            NarrativeMessageDialog(
                type = NarrativeMessageType.STORY,
                title = "A Tale Begins",
                text = "Once upon a time in Egril...",
                onDismiss = {}
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("A Tale Begins", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun testStoryDialogShowsBodyText() {
        composeTestRule.setContent {
            NarrativeMessageDialog(
                type = NarrativeMessageType.STORY,
                title = "A Tale Begins",
                text = "Once upon a time in Egril...",
                onDismiss = {}
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Once upon a time in Egril...", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun testStoryDialogDismissButtonExists() {
        composeTestRule.setContent {
            NarrativeMessageDialog(
                type = NarrativeMessageType.STORY,
                title = "A Tale Begins",
                text = "Once upon a time in Egril...",
                onDismiss = {}
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("OK", substring = true, ignoreCase = true)
            .assertExists()
            .assertHasClickAction()
    }

    @Test
    fun testStoryDialogCallsOnDismissWhenButtonClicked() {
        var dismissed = false

        composeTestRule.setContent {
            NarrativeMessageDialog(
                type = NarrativeMessageType.STORY,
                title = "A Tale Begins",
                text = "Once upon a time in Egril...",
                onDismiss = { dismissed = true }
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("OK", substring = true, ignoreCase = true)
            .performClick()

        composeTestRule.waitForIdle()

        assert(dismissed) { "onDismiss should be called when OK is clicked" }
    }
}
