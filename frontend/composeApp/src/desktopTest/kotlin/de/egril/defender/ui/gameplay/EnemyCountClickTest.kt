package de.egril.defender.ui.gameplay

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import de.egril.defender.editor.EditorStorage
import de.egril.defender.game.LevelData
import de.egril.defender.model.GameState
import de.egril.defender.model.GamePhase
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import com.hyperether.resources.AppLocale
import com.hyperether.resources.currentLanguage

/**
 * UI test for enemy count click functionality in the game header.
 * 
 * Verifies that clicking the enemy count display in the header toggles
 * the enemy list panel visibility.
 */
class EnemyCountClickTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setDefaultLanguage() {
        currentLanguage.value = AppLocale.DEFAULT
        EditorStorage.ensureInitialized()
    }
    
    @Test
    fun testEnemyCountClickTogglesEnemyList() {
        // Create a game state for testing
        val level = LevelData.createLevels().first { it.id == 1 }
        val gameState = GameState(level)
        
        // Set game to player turn phase so enemies are spawned
        gameState.phase.value = GamePhase.PLAYER_TURN
        
        // Track if showOverlay is toggled
        var showOverlayClicked = false
        
        composeTestRule.setContent {
            var showOverlay by remember { mutableStateOf(false) }
            
            androidx.compose.foundation.layout.Column {
                GameHeader(
                    gameState = gameState,
                    showOverlay = showOverlay,
                    onShowOverlayChange = { showOverlay = it },
                    onBackToMap = {},
                    onSaveGame = null,
                    onCheatCode = null,
                    onEnemyCountClick = { 
                        showOverlayClicked = true
                        showOverlay = !showOverlay
                    }
                )
            }
        }
        
        composeTestRule.waitForIdle()
        
        // Find the enemy count display by looking for text with pattern "number | number"
        // The enemy count shows "activeCount | remainingCount"
        val activeCount = gameState.getActiveEnemyCount()
        val remainingCount = gameState.getRemainingEnemyCount()
        val enemyCountPattern = "$activeCount | $remainingCount"
        
        // Click on the enemy count
        composeTestRule.onNodeWithText(enemyCountPattern, useUnmergedTree = true)
            .assertExists("Enemy count should be visible in header")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Verify that the click handler was called
        assert(showOverlayClicked) { "Enemy count click should trigger overlay toggle" }
    }
    
    @Test
    fun testEnemyCountClickTogglesEnemyListMultipleTimes() {
        // Create a game state for testing
        val level = LevelData.createLevels().first { it.id == 1 }
        val gameState = GameState(level)
        
        // Set game to player turn phase
        gameState.phase.value = GamePhase.PLAYER_TURN
        
        composeTestRule.setContent {
            var showOverlay by remember { mutableStateOf(false) }
            
            androidx.compose.foundation.layout.Column {
                GameHeader(
                    gameState = gameState,
                    showOverlay = showOverlay,
                    onShowOverlayChange = { showOverlay = it },
                    onBackToMap = {},
                    onSaveGame = null,
                    onCheatCode = null,
                    onEnemyCountClick = { showOverlay = !showOverlay }
                )
                
                // Show text to indicate overlay state
                androidx.compose.material3.Text(
                    text = if (showOverlay) "Overlay Open" else "Overlay Closed"
                )
            }
        }
        
        composeTestRule.waitForIdle()
        
        // Initial state should be closed
        composeTestRule.onNodeWithText("Overlay Closed")
            .assertExists("Overlay should start closed")
        
        // Get the enemy count text
        val activeCount = gameState.getActiveEnemyCount()
        val remainingCount = gameState.getRemainingEnemyCount()
        val enemyCountPattern = "$activeCount | $remainingCount"
        
        // First click - should open overlay
        composeTestRule.onNodeWithText(enemyCountPattern, useUnmergedTree = true)
            .performClick()
        
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithText("Overlay Open")
            .assertExists("Overlay should be open after first click")
        
        // Second click - should close overlay
        composeTestRule.onNodeWithText(enemyCountPattern, useUnmergedTree = true)
            .performClick()
        
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithText("Overlay Closed")
            .assertExists("Overlay should be closed after second click")
        
        // Third click - should open overlay again
        composeTestRule.onNodeWithText(enemyCountPattern, useUnmergedTree = true)
            .performClick()
        
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithText("Overlay Open")
            .assertExists("Overlay should be open after third click")
    }
}
