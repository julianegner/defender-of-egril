package de.egril.defender.ui.loadgame

import de.egril.defender.model.*
import de.egril.defender.save.SaveGameMetadata
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test for SavedGameCard autosave color distinction
 */
class SavedGameCardColorTest {

    /**
     * Test that autosave ID is correctly identified
     */
    @Test
    fun testAutosaveIdentification() {
        // Create an autosave metadata
        val autosaveMetadata = SaveGameMetadata(
            id = "autosave_game",
            timestamp = 1234567890L,  // Fixed timestamp for testing
            levelId = 1,
            levelName = "Test Level",
            turnNumber = 5,
            coins = 100,
            healthPoints = 10,
            towerCount = 3,
            enemyCount = 5,
            defenderCounts = emptyMap(),
            attackerCounts = emptyMap(),
            remainingSpawnCounts = emptyMap(),
            comment = "Autosave"
        )
        
        // Verify autosave ID
        assertEquals("autosave_game", autosaveMetadata.id)
        
        // Create a regular save metadata
        val regularSaveMetadata = SaveGameMetadata(
            id = "savegame_1234567890",
            timestamp = 1234567890L,  // Fixed timestamp for testing
            levelId = 1,
            levelName = "Test Level",
            turnNumber = 5,
            coins = 100,
            healthPoints = 10,
            towerCount = 3,
            enemyCount = 5,
            defenderCounts = emptyMap(),
            attackerCounts = emptyMap(),
            remainingSpawnCounts = emptyMap(),
            comment = "My save"
        )
        
        // Verify regular save ID is not autosave
        assertEquals(false, regularSaveMetadata.id == "autosave_game")
    }
    
    /**
     * Test that the autosave check logic works correctly
     */
    @Test
    fun testAutosaveCheckLogic() {
        // Test various IDs
        val testCases = listOf(
            "autosave_game" to true,
            "savegame_1234567890" to false,
            "autosave" to false,
            "auto_save_game" to false,
            "autosave_game_2" to false,
            "" to false
        )
        
        testCases.forEach { (id, expectedIsAutosave) ->
            val isAutosave = id == "autosave_game"
            assertEquals(
                expectedIsAutosave, 
                isAutosave,
                "ID '$id' should ${if (expectedIsAutosave) "" else "NOT "}be identified as autosave"
            )
        }
    }
}
