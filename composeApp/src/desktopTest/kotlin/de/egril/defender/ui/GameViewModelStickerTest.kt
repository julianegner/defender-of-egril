package de.egril.defender.ui

import de.egril.defender.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Tests for GameViewModel sticker cheat code functionality.
 */
class GameViewModelStickerTest {
    
    @Test
    fun testStickerCheatCodeNavigatesToStickerScreen() = runBlocking {
        // Create a GameViewModel instance
        val viewModel = GameViewModel()
        
        // Verify initial screen is MainMenu
        assertEquals(Screen.MainMenu, viewModel.currentScreen.first())
        
        // Navigate to WorldMap first (since cheat code is for world map)
        viewModel.navigateToWorldMap()
        assertEquals(Screen.WorldMap, viewModel.currentScreen.first())
        
        // Apply the "sticker" cheat code
        val result = viewModel.applyWorldMapCheatCode("sticker")
        
        // Verify the cheat code was successful
        assertTrue(result, "Sticker cheat code should be successful")
        
        // Verify we navigated to the Sticker screen
        assertEquals(Screen.Sticker, viewModel.currentScreen.first())
    }
    
    @Test
    fun testStickerCheatCodeIsCaseInsensitive() = runBlocking {
        val viewModel = GameViewModel()
        
        // Navigate to WorldMap
        viewModel.navigateToWorldMap()
        
        // Try different case variations
        val testCases = listOf("sticker", "STICKER", "Sticker", "StIcKeR")
        
        for (testCase in testCases) {
            // Reset to WorldMap
            viewModel.navigateToWorldMap()
            
            // Apply the cheat code
            val result = viewModel.applyWorldMapCheatCode(testCase)
            
            // Verify the cheat code was successful
            assertTrue(result, "Cheat code '$testCase' should be successful")
            
            // Verify we navigated to the Sticker screen
            assertEquals(Screen.Sticker, viewModel.currentScreen.first(), 
                "Cheat code '$testCase' should navigate to Sticker screen")
        }
    }
    
    @Test
    fun testStickerCheatCodeWithExtraWhitespace() = runBlocking {
        val viewModel = GameViewModel()
        
        // Navigate to WorldMap
        viewModel.navigateToWorldMap()
        
        // Apply the cheat code with extra whitespace
        val result = viewModel.applyWorldMapCheatCode("  sticker  ")
        
        // Verify the cheat code was successful
        assertTrue(result, "Sticker cheat code with whitespace should be successful")
        
        // Verify we navigated to the Sticker screen
        assertEquals(Screen.Sticker, viewModel.currentScreen.first())
    }
    
    @Test
    fun testNavigateToSticker() = runBlocking {
        val viewModel = GameViewModel()
        
        // Navigate to Sticker screen directly
        viewModel.navigateToSticker()
        
        // Verify we're on the Sticker screen
        assertEquals(Screen.Sticker, viewModel.currentScreen.first())
    }
}
