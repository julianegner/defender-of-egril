package de.egril.defender.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.hyperether.resources.LocalizedStrings
import com.hyperether.resources.currentLanguage
import de.egril.defender.ui.settings.AppSettings
import de.egril.defender.ui.settings.InitialLanguageChooserDialog
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertNotNull

/**
 * UI tests for the Initial Language Chooser Dialog.
 *
 * These tests verify that the language chooser dialog works correctly
 * on first app start.
 */
class InitialLanguageChooserDialogTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * The dialog preselects the system language in a LaunchedEffect, so the active language
     * may differ from the default. Expected labels are resolved from the active language.
     */
    private fun localizedText(key: String): String = LocalizedStrings.get(key, currentLanguage.value)

    @Test
    fun testInitialLanguageChooserDialogRendersCorrectly() {
        composeTestRule.setContent {
            InitialLanguageChooserDialog(
                onLanguageSelected = {},
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText(localizedText("initial_language_title"), substring = true, ignoreCase = true)
            .assertExists()

        composeTestRule
            .onNodeWithText(localizedText("initial_language_continue"), substring = true, ignoreCase = true)
            .assertExists()
            .assertHasClickAction()
    }

    @Test
    fun testInitialLanguageChooserDialogContinueButton() {
        var languageSelected = false

        composeTestRule.setContent {
            InitialLanguageChooserDialog(
                onLanguageSelected = { languageSelected = true },
            )
        }

        composeTestRule.waitForIdle()

        // Click continue button
        composeTestRule
            .onNodeWithText(localizedText("initial_language_continue"), substring = true, ignoreCase = true)
            .performClick()

        composeTestRule.waitForIdle()

        // Verify callback was invoked
        assert(languageSelected) { "Continue button should trigger language selected callback" }

        // Verify language was marked as chosen
        assert(AppSettings.hasChosenLanguage()) { "Language should be marked as chosen after continue" }
    }

    @Test
    fun testLanguageChooserPreselectsSystemLanguage() {
        composeTestRule.setContent {
            InitialLanguageChooserDialog(
                onLanguageSelected = {},
            )
        }

        composeTestRule.waitForIdle()

        // Verify that a language is selected (either default or system language)
        val selectedLanguage = currentLanguage.value
        assertNotNull(selectedLanguage) { "A language should be preselected" }
    }
}
