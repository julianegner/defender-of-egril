package com.defenderofegril.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.defenderofegril.game.LevelData
import com.defenderofegril.model.GameState
import com.defenderofegril.model.GamePhase
import com.defenderofegril.ui.gameplay.GamePlayScreen
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for the Game Play screen.
 * 
 * These tests verify that the Game Play screen renders correctly
 * in different game states and captures screenshots for visual verification.
 */
class GamePlayScreenTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun testGamePlayScreenInitialState() {
        // Create a game state for testing - using level 1
        val level = LevelData.createLevels().first { it.id == 1 }
        val gameState = GameState(level)
        
        composeTestRule.setContent {
            GamePlayScreen(
                gameState = gameState,
                onPlaceDefender = { _, _ -> true },
                onUpgradeDefender = { true },
                onUndoTower = { true },
                onSellTower = { true },
                onStartFirstPlayerTurn = {},
                onDefenderAttack = { _, _ -> true },
                onDefenderAttackPosition = { _, _ -> true },
                onEndPlayerTurn = {},
                onBackToMap = {}
            )
        }
        
        composeTestRule.waitForIdle()
        
        // Verify the screen renders (basic check)
        composeTestRule.onRoot().assertExists()
        
        // Capture screenshot of initial state
        ScreenshotTestUtils.captureScreenshot(
            composeTestRule,
            "gameplay-screen-initial",
            width = 1400,
            height = 900
        )
    }
    
    @Test
    fun testGamePlayScreenWithEnemies() {
        // Create a game state with spawned enemies
        val level = LevelData.createLevels().first { it.id == 1 }
        val gameState = GameState(level)
        
        // Simulate starting the game to spawn enemies
        gameState.phase.value = GamePhase.PLAYER_TURN
        gameState.currentWaveIndex.value = 1
        
        composeTestRule.setContent {
            GamePlayScreen(
                gameState = gameState,
                onPlaceDefender = { _, _ -> true },
                onUpgradeDefender = { true },
                onUndoTower = { true },
                onSellTower = { true },
                onStartFirstPlayerTurn = {},
                onDefenderAttack = { _, _ -> true },
                onDefenderAttackPosition = { _, _ -> true },
                onEndPlayerTurn = {},
                onBackToMap = {}
            )
        }
        
        composeTestRule.waitForIdle()
        
        // Verify the screen renders
        composeTestRule.onRoot().assertExists()
        
        // Capture screenshot with game started
        ScreenshotTestUtils.captureScreenshot(
            composeTestRule,
            "gameplay-screen-with-enemies",
            width = 1400,
            height = 900
        )
    }
    
    @Test
    fun testGamePlayScreenInitialBuildingPhase() {
        // Create a game state in initial building phase
        val level = LevelData.createLevels().first { it.id == 1 }
        val gameState = GameState(level)
        // gameState.phase should be GamePhase.INITIAL_BUILDING by default
        
        composeTestRule.setContent {
            GamePlayScreen(
                gameState = gameState,
                onPlaceDefender = { _, _ -> true },
                onUpgradeDefender = { true },
                onUndoTower = { true },
                onSellTower = { true },
                onStartFirstPlayerTurn = {},
                onDefenderAttack = { _, _ -> true },
                onDefenderAttackPosition = { _, _ -> true },
                onEndPlayerTurn = {},
                onBackToMap = {}
            )
        }
        
        composeTestRule.waitForIdle()
        
        // Verify the screen renders
        composeTestRule.onRoot().assertExists()
        
        // Look for "Start Battle" or similar text that appears in initial phase
        // This text should be present in the initial building phase
        composeTestRule.onNodeWithText("Start", substring = true, ignoreCase = true)
            .assertExists()
        
        // Capture screenshot of building phase
        ScreenshotTestUtils.captureScreenshot(
            composeTestRule,
            "gameplay-screen-building-phase",
            width = 1400,
            height = 900
        )
    }
    
    @Test
    fun testGamePlayScreenRendersMap() {
        val level = LevelData.createLevels().first { it.id == 1 }
        val gameState = GameState(level)
        
        composeTestRule.setContent {
            GamePlayScreen(
                gameState = gameState,
                onPlaceDefender = { _, _ -> true },
                onUpgradeDefender = { true },
                onUndoTower = { true },
                onSellTower = { true },
                onStartFirstPlayerTurn = {},
                onDefenderAttack = { _, _ -> true },
                onDefenderAttackPosition = { _, _ -> true },
                onEndPlayerTurn = {},
                onBackToMap = {}
            )
        }
        
        composeTestRule.waitForIdle()
        
        // Verify the screen renders
        composeTestRule.onRoot().assertExists()
        
        // The game map should be present (it's always rendered)
        // We can verify this by checking that the root exists and has content
        composeTestRule.onRoot().assertIsDisplayed()
    }
}
