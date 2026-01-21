package de.egril.defender.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import de.egril.defender.ui.settings.AppSettings
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * UI tests for the Settings Hint Box feature.
 * 
 * These tests verify that the settings hint box is shown on first run
 * and can be dismissed, and that it doesn't show on subsequent runs.
 */
class SettingsHintBoxTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Before
    fun setup() {
        // Reset settings hint shown state before each test
        AppSettings.settingsHintShown.value = false
    }
    
    @Test
    fun testSettingsHintBoxShowsOnFirstRun() {
        // Set the hint to not shown (first run)
        AppSettings.settingsHintShown.value = false
        
        composeTestRule.setContent {
            MainMenuScreen(
                onStartGame = {},
                onShowRules = {},
                onShowInstallationInfo = {},
                onSelectPlayer = {},
                onEditPlayerName = {},  // No player selection in tests
                currentPlayerName = null  // No player name in tests
            )
        }
        
        composeTestRule.waitForIdle()
        
        // Verify the settings hint box is shown
        composeTestRule.onNodeWithText("Settings Available", substring = true, ignoreCase = true)
            .assertExists()
        
        // Verify hint message is present
        composeTestRule.onNodeWithText("customize your experience", substring = true, ignoreCase = true)
            .assertExists()
        
        // Verify dismiss button exists
        composeTestRule.onNodeWithText("Got it!", substring = true, ignoreCase = true)
            .assertExists()
            .assertHasClickAction()
    }
    
    @Test
    fun testSettingsHintBoxCanBeDismissed() {
        // Set the hint to not shown (first run)
        AppSettings.settingsHintShown.value = false
        
        composeTestRule.setContent {
            MainMenuScreen(
                onStartGame = {},
                onShowRules = {},
                onShowInstallationInfo = {},
                onSelectPlayer = {},
                onEditPlayerName = {},  // No player selection in tests
                currentPlayerName = null  // No player name in tests
            )
        }
        
        composeTestRule.waitForIdle()
        
        // Verify the hint box is initially shown
        composeTestRule.onNodeWithText("Settings Available", substring = true, ignoreCase = true)
            .assertExists()
        
        // Click the dismiss button
        composeTestRule.onNodeWithText("Got it!", substring = true, ignoreCase = true)
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Verify the hint box is no longer shown
        composeTestRule.onNodeWithText("Settings Available", substring = true, ignoreCase = true)
            .assertDoesNotExist()
        
        // Verify the setting was persisted
        assertTrue(AppSettings.settingsHintShown.value, "Settings hint should be marked as shown after dismissal")
    }
    
    @Test
    fun testSettingsHintBoxDoesNotShowOnSubsequentRuns() {
        // Set the hint to already shown
        AppSettings.settingsHintShown.value = true
        
        composeTestRule.setContent {
            MainMenuScreen(
                onStartGame = {},
                onShowRules = {},
                onShowInstallationInfo = {},
                onSelectPlayer = {},
                onEditPlayerName = {},  // No player selection in tests
                currentPlayerName = null  // No player name in tests
            )
        }
        
        composeTestRule.waitForIdle()
        
        // Verify the settings hint box is NOT shown
        composeTestRule.onNodeWithText("Settings Available", substring = true, ignoreCase = true)
            .assertDoesNotExist()
    }
    
    @Test
    fun testMainMenuStillFunctionalWithHintBox() {
        // Set the hint to not shown (first run)
        AppSettings.settingsHintShown.value = false
        
        var startGameClicked = false
        var showRulesClicked = false
        
        composeTestRule.setContent {
            MainMenuScreen(
                onStartGame = { startGameClicked = true },
                onShowRules = { showRulesClicked = true },
                onShowInstallationInfo = {},
                onSelectPlayer = {},
                onEditPlayerName = {},  // No player selection in tests
                currentPlayerName = null  // No player name in tests
            )
        }
        
        composeTestRule.waitForIdle()
        
        // Verify hint box is shown
        composeTestRule.onNodeWithText("Settings Available", substring = true, ignoreCase = true)
            .assertExists()
        
        // Verify we can still interact with Start Game button
        composeTestRule.onNodeWithText("Start Game", substring = true, ignoreCase = true)
            .assertExists()
            .performClick()
        
        assertTrue(startGameClicked, "Start Game button should be clickable with hint box present")
        
        // Verify we can still interact with Rules button
        composeTestRule.onNodeWithText("Rules", substring = true, ignoreCase = true)
            .assertExists()
            .performClick()
        
        assertTrue(showRulesClicked, "Rules button should be clickable with hint box present")
    }
    
    @Test
    fun testSettingsHintBoxListsAllSettings() {
        // Set the hint to not shown (first run)
        AppSettings.settingsHintShown.value = false
        
        composeTestRule.setContent {
            MainMenuScreen(
                onStartGame = {},
                onShowRules = {},
                onShowInstallationInfo = {},
                onSelectPlayer = {},
                onEditPlayerName = {},  // No player selection in tests
                currentPlayerName = null  // No player name in tests
            )
        }
        
        composeTestRule.waitForIdle()
        
        // Verify all settings categories are listed
        composeTestRule.onNodeWithText("Appearance", substring = true, ignoreCase = true)
            .assertExists()
        composeTestRule.onNodeWithText("Sound", substring = true, ignoreCase = true)
            .assertExists()
        composeTestRule.onNodeWithText("Controls", substring = true, ignoreCase = true)
            .assertExists()
        composeTestRule.onNodeWithText("Language", substring = true, ignoreCase = true)
            .assertExists()
        composeTestRule.onNodeWithText("Difficulty", substring = true, ignoreCase = true)
            .assertExists()
    }
}
