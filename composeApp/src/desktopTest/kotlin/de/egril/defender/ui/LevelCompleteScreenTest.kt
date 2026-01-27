package de.egril.defender.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.hyperether.resources.AppLocale
import com.hyperether.resources.currentLanguage
import de.egril.defender.ui.ScreenshotTestUtils
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for the Level Complete screen.
 * 
 * These tests verify that the Level Complete screen renders correctly
 * in different states (victory, defeat, final victory) and captures
 * screenshots for visual verification.
 */
class LevelCompleteScreenTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setDefaultLanguage() {
        currentLanguage.value = AppLocale.DEFAULT
    }
    
    @Test
    fun testLevelCompleteScreenVictoryState() {
        var restartClicked = false
        var backToMapClicked = false
        
        // Set up victory state (not last level)
        composeTestRule.setContent {
            LevelCompleteScreen(
                levelId = 1,
                won = true,
                isLastLevel = false,
                onRestart = { restartClicked = true },
                onBackToMap = { backToMapClicked = true }
            )
        }
        
        composeTestRule.waitForIdle()
        
        // Verify victory elements are present
        composeTestRule.onNodeWithText("Battle Won", substring = true, ignoreCase = true)
            .assertExists()
        
        // Check for retry button
        composeTestRule.onNodeWithText("Retry", substring = true, ignoreCase = true)
            .assertExists()
            .assertHasClickAction()
        
        // Check for world map button
        composeTestRule.onNodeWithText("World Map", substring = true, ignoreCase = true)
            .assertExists()
            .assertHasClickAction()
        
        // Capture screenshot
        ScreenshotTestUtils.captureScreenshot(
            composeTestRule,
            "level-complete-victory",
            width = 1200,
            height = 800
        )
    }
    
    @Test
    fun testLevelCompleteScreenDefeatState() {
        var restartClicked = false
        var backToMapClicked = false
        
        // Set up defeat state
        composeTestRule.setContent {
            LevelCompleteScreen(
                levelId = 1,
                won = false,
                isLastLevel = false,
                onRestart = { restartClicked = true },
                onBackToMap = { backToMapClicked = true }
            )
        }
        
        composeTestRule.waitForIdle()
        
        // Verify defeat elements are present
        composeTestRule.onNodeWithText("Defeat", substring = true, ignoreCase = true)
            .assertExists()
        
        // Check for retry button
        composeTestRule.onNodeWithText("Retry", substring = true, ignoreCase = true)
            .assertExists()
            .assertHasClickAction()
        
        // Check for world map button
        composeTestRule.onNodeWithText("World Map", substring = true, ignoreCase = true)
            .assertExists()
            .assertHasClickAction()
        
        // Capture screenshot
        ScreenshotTestUtils.captureScreenshot(
            composeTestRule,
            "level-complete-defeat",
            width = 1200,
            height = 800
        )
    }
    
    @Test
    fun testLevelCompleteScreenFinalVictoryState() {
        var restartClicked = false
        var backToMapClicked = false
        
        // Set up final victory state (last level won)
        composeTestRule.setContent {
            LevelCompleteScreen(
                levelId = 5,
                won = true,
                isLastLevel = true,
                onRestart = { restartClicked = true },
                onBackToMap = { backToMapClicked = true }
            )
        }
        
        composeTestRule.waitForIdle()
        
        // Verify final victory elements are present
        composeTestRule.onNodeWithText("Victory", substring = true, ignoreCase = true)
            .assertExists()
        
        // Check for buttons
        composeTestRule.onNodeWithText("Retry", substring = true, ignoreCase = true)
            .assertExists()
        
        composeTestRule.onNodeWithText("World Map", substring = true, ignoreCase = true)
            .assertExists()
        
        // Capture screenshot
        ScreenshotTestUtils.captureScreenshot(
            composeTestRule,
            "level-complete-final-victory",
            width = 1200,
            height = 800
        )
    }
    
    @Test
    fun testLevelCompleteButtonInteractions() {
        var restartClicked = false
        var backToMapClicked = false
        
        composeTestRule.setContent {
            LevelCompleteScreen(
                levelId = 1,
                won = true,
                isLastLevel = false,
                onRestart = { restartClicked = true },
                onBackToMap = { backToMapClicked = true }
            )
        }
        
        composeTestRule.waitForIdle()
        
        // Test retry button
        composeTestRule.onNodeWithText("Retry", substring = true, ignoreCase = true)
            .performClick()
        
        composeTestRule.waitForIdle()
        assert(restartClicked) { "Restart button should trigger callback" }
        
        // Test world map button
        composeTestRule.onNodeWithText("World Map", substring = true, ignoreCase = true)
            .performClick()
        
        composeTestRule.waitForIdle()
        assert(backToMapClicked) { "World Map button should trigger callback" }
    }
}
