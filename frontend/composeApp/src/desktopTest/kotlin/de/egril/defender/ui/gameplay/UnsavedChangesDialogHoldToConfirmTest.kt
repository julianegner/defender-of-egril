package de.egril.defender.ui.gameplay

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import de.egril.defender.ui.settings.AppSettings
import org.junit.After
import org.junit.Rule
import org.junit.Test

class UnsavedChangesDialogHoldToConfirmTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @After
    fun tearDown() {
        AppSettings.saveHoldToConfirmEnabled(false)
    }

    @Test
    fun showsHoldToConfirmDiscardButtonWhenEnabled() {
        AppSettings.saveHoldToConfirmEnabled(true)

        composeTestRule.setContent {
            UnsavedChangesDialog(
                onSaveAndExit = {},
                onDiscardChanges = {},
                onCancel = {},
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("Hold to Confirm", substring = true, ignoreCase = true)
            .assertExists()
        composeTestRule
            .onNodeWithText("Save and Exit", substring = true, ignoreCase = true)
            .assertExists()
            .assertHasClickAction()
    }

    @Test
    fun showsDirectDiscardButtonWhenHoldToConfirmDisabled() {
        AppSettings.saveHoldToConfirmEnabled(false)

        composeTestRule.setContent {
            UnsavedChangesDialog(
                onSaveAndExit = {},
                onDiscardChanges = {},
                onCancel = {},
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("Discard Changes", substring = true, ignoreCase = true)
            .assertExists()
            .assertHasClickAction()
    }
}
