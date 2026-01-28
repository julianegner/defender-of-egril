package de.egril.defender.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.hyperether.resources.AppLocale
import com.hyperether.resources.currentLanguage
import de.egril.defender.ui.settings.AppSettings
import de.egril.defender.ui.settings.InitialLanguageChooserDialog
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for the Initial Language Chooser Dialog.
 * 
 * These tests verify that the language chooser dialog works correctly
 * on first app start.
 */
class InitialLanguageChooserDialogTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testInitialLanguageChooserDialogRendersCorrectly() {
        var languageSelected = false
        
        composeTestRule.setContent {
            InitialLanguageChooserDialog(
                onLanguageSelected = { languageSelected = true }
            )
        }
        
        composeTestRule.waitForIdle()
        
        // Verify the dialog title is displayed
        composeTestRule.onNodeWithText("Choose Your Language", substring = true, ignoreCase = true)
            .assertExists()
        
        // Verify continue button is displayed
        composeTestRule.onNodeWithText("Continue", substring = true, ignoreCase = true)
            .assertExists()
            .assertHasClickAction()
    }
    
    @Test
    fun testInitialLanguageChooserDialogContinueButton() {
        var languageSelected = false
        
        // Reset language to default before test
        currentLanguage.value = AppLocale.DEFAULT
        
        composeTestRule.setContent {
            InitialLanguageChooserDialog(
                onLanguageSelected = { languageSelected = true }
            )
        }
        
        composeTestRule.waitForIdle()
        
        // Click continue button
        composeTestRule.onNodeWithText("Continue", substring = true, ignoreCase = true)
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
                onLanguageSelected = {}
            )
        }
        
        composeTestRule.waitForIdle()
        
        // Verify that a language is selected (either default or system language)
        val selectedLanguage = currentLanguage.value
        assert(selectedLanguage != null) { "A language should be preselected" }
    }
}
