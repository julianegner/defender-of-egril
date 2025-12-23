package de.egril.defender.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import de.egril.defender.game.LevelData
import de.egril.defender.model.LevelStatus
import de.egril.defender.model.WorldLevel
import de.egril.defender.ui.worldmap.WorldMapScreen
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for the World Map screen.
 * 
 * These tests verify that the World Map screen renders correctly
 * with different level states and captures screenshots for visual verification.
 */
class WorldMapScreenTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    /**
     * Helper function to create sample world levels for testing
     */
    private fun createSampleWorldLevels(): List<WorldLevel> {
        val allLevels = LevelData.createLevels()
        return allLevels.map { level ->
            WorldLevel(
                level = level,
                status = when (level.id) {
                    1 -> LevelStatus.UNLOCKED
                    2 -> LevelStatus.WON
                    else -> LevelStatus.LOCKED
                }
            )
        }
    }
    
    @Test
    fun testWorldMapScreenRendersCorrectly() {
        val worldLevels = createSampleWorldLevels()
        var selectedLevel: Int? = null
        var backToMenuClicked = false
        var showRulesClicked = false
        var openEditorClicked = false
        var loadGameClicked = false
        
        composeTestRule.setContent {
            WorldMapScreen(
                worldLevels = worldLevels,
                onLevelSelected = { selectedLevel = it },
                onBackToMenu = { backToMenuClicked = true },
                onShowRules = { showRulesClicked = true },
                onOpenEditor = { openEditorClicked = true },
                onLoadGame = { loadGameClicked = true },
                checkForNewRepositoryData = false,  // Disable repository check in tests
                onSwitchPlayer = null,  // No player switching in tests
                currentPlayerName = null  // No player name in tests
            )
        }
        
        composeTestRule.waitForIdle()
        
        // Verify title is displayed
        composeTestRule.onNodeWithText("World Map", substring = true, ignoreCase = true)
            .assertExists()
        
        // Verify back button exists
        composeTestRule.onNodeWithText("Back", substring = true, ignoreCase = true)
            .assertExists()
            .assertHasClickAction()
        
        // Verify rules button exists
        composeTestRule.onNodeWithText("Rules", substring = true, ignoreCase = true)
            .assertExists()
            .assertHasClickAction()
        
        // Verify load game button exists
        composeTestRule.onNodeWithText("Load", substring = true, ignoreCase = true)
            .assertExists()
            .assertHasClickAction()
        
        // Capture screenshot
        ScreenshotTestUtils.captureScreenshot(
            composeTestRule,
            "world-map-screen",
            width = 1200,
            height = 800
        )
    }
    
    @Test
    fun testWorldMapScreenWithAllLevelsLocked() {
        val allLevels = LevelData.createLevels()
        val worldLevels = allLevels.map { level ->
            WorldLevel(level = level, status = LevelStatus.LOCKED)
        }
        
        composeTestRule.setContent {
            WorldMapScreen(
                worldLevels = worldLevels,
                onLevelSelected = {},
                onBackToMenu = {},
                onShowRules = {},
                onOpenEditor = {},
                onLoadGame = {},
                checkForNewRepositoryData = false,  // Disable repository check in tests
                onSwitchPlayer = null,  // No player switching in tests
                currentPlayerName = null  // No player name in tests
            )
        }
        
        composeTestRule.waitForIdle()
        
        // Verify the screen renders
        composeTestRule.onRoot().assertExists()
        
        // Capture screenshot showing all locked levels
        ScreenshotTestUtils.captureScreenshot(
            composeTestRule,
            "world-map-all-locked",
            width = 1200,
            height = 800
        )
    }
    
    @Test
    fun testWorldMapScreenWithAllLevelsUnlocked() {
        val allLevels = LevelData.createLevels()
        val worldLevels = allLevels.map { level ->
            WorldLevel(level = level, status = LevelStatus.UNLOCKED)
        }
        
        composeTestRule.setContent {
            WorldMapScreen(
                worldLevels = worldLevels,
                onLevelSelected = {},
                onBackToMenu = {},
                onShowRules = {},
                onOpenEditor = {},
                onLoadGame = {},
                checkForNewRepositoryData = false,  // Disable repository check in tests
                onSwitchPlayer = null,  // No player switching in tests
                currentPlayerName = null  // No player name in tests
            )
        }
        
        composeTestRule.waitForIdle()
        
        // Verify the screen renders
        composeTestRule.onRoot().assertExists()
        
        // Capture screenshot showing all unlocked levels
        ScreenshotTestUtils.captureScreenshot(
            composeTestRule,
            "world-map-all-unlocked",
            width = 1200,
            height = 800
        )
    }
    
    @Test
    fun testWorldMapScreenWithAllLevelsWon() {
        val allLevels = LevelData.createLevels()
        val worldLevels = allLevels.map { level ->
            WorldLevel(level = level, status = LevelStatus.WON)
        }
        
        composeTestRule.setContent {
            WorldMapScreen(
                worldLevels = worldLevels,
                onLevelSelected = {},
                onBackToMenu = {},
                onShowRules = {},
                onOpenEditor = {},
                onLoadGame = {},
                checkForNewRepositoryData = false,  // Disable repository check in tests
                onSwitchPlayer = null,  // No player switching in tests
                currentPlayerName = null  // No player name in tests
            )
        }
        
        composeTestRule.waitForIdle()
        
        // Verify the screen renders
        composeTestRule.onRoot().assertExists()
        
        // Capture screenshot showing all won levels
        ScreenshotTestUtils.captureScreenshot(
            composeTestRule,
            "world-map-all-won",
            width = 1200,
            height = 800
        )
    }
    
    @Test
    fun testWorldMapButtonInteractions() {
        val worldLevels = createSampleWorldLevels()
        var backToMenuClicked = false
        var showRulesClicked = false
        var loadGameClicked = false
        
        composeTestRule.setContent {
            WorldMapScreen(
                worldLevels = worldLevels,
                onLevelSelected = {},
                onBackToMenu = { backToMenuClicked = true },
                onShowRules = { showRulesClicked = true },
                onOpenEditor = {},
                onLoadGame = { loadGameClicked = true },
                checkForNewRepositoryData = false,  // Disable repository check in tests
                onSwitchPlayer = null,  // No player switching in tests
                currentPlayerName = null  // No player name in tests
            )
        }
        
        composeTestRule.waitForIdle()
        
        // Test back button
        composeTestRule.onNodeWithText("Back", substring = true, ignoreCase = true)
            .performClick()
        composeTestRule.waitForIdle()
        assert(backToMenuClicked) { "Back button should trigger callback" }
        
        // Test rules button
        composeTestRule.onNodeWithText("Rules", substring = true, ignoreCase = true)
            .performClick()
        composeTestRule.waitForIdle()
        assert(showRulesClicked) { "Rules button should trigger callback" }
        
        // Test load game button
        composeTestRule.onNodeWithText("Load", substring = true, ignoreCase = true)
            .performClick()
        composeTestRule.waitForIdle()
        assert(loadGameClicked) { "Load Game button should trigger callback" }
    }
}
