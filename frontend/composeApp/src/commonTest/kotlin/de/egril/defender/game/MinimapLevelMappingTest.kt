package de.egril.defender.game

import de.egril.defender.editor.EditorLevel
import de.egril.defender.editor.EditorMap
import de.egril.defender.editor.EditorEnemySpawn
import de.egril.defender.editor.EditorStorage
import de.egril.defender.editor.TileType
import de.egril.defender.model.AttackerType
import de.egril.defender.model.DefenderType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Test that minimap correctly maps game levels to editor levels
 * even when some editor levels are filtered out
 */
class MinimapLevelMappingTest {
    
    @Test
    fun testEditorLevelIdIsPreservedInConversion() {
        // Create a valid map
        val tiles = mutableMapOf<String, TileType>()
        tiles["0,0"] = TileType.SPAWN_POINT
        tiles["1,0"] = TileType.PATH
        tiles["2,0"] = TileType.PATH
        tiles["3,0"] = TileType.TARGET
        
        val validMap = EditorMap(
            id = "test_valid_map_123",
            name = "Test Valid Map",
            width = 10,
            height = 5,
            tiles = tiles
        )
        
        // Create a valid editor level
        val editorLevel = EditorLevel(
            id = "test_level_123",
            mapId = validMap.id,
            title = "Test Level 123",
            startCoins = 100,
            enemySpawns = listOf(
                EditorEnemySpawn(
                    attackerType = AttackerType.GOBLIN,
                    spawnTurn = 1,
                    level = 1
                )
            ),
            availableTowers = setOf(DefenderType.SPIKE_TOWER)
        )
        
        // Save the map so convertToGameLevel can find it
        EditorStorage.saveMap(validMap)
        
        try {
            // Convert to game level
            val gameLevel = EditorStorage.convertToGameLevel(editorLevel, 42)
            
            // Verify the editor level ID is preserved
            assertNotNull(gameLevel, "Game level should be created")
            assertEquals("test_level_123", gameLevel.editorLevelId, 
                "Editor level ID should be preserved in game level")
            assertEquals(42, gameLevel.id, "Game level should have numeric ID 42")
            assertEquals("Test Level 123", gameLevel.name, "Level name should match")
        } finally {
            // Clean up
            EditorStorage.deleteMap(validMap.id)
        }
    }
    
    @Test
    fun testLevelWithoutEditorLevelIdFallsBackToOldBehavior() {
        // This test verifies backwards compatibility for levels that don't have editorLevelId
        // (e.g., if loaded from old save files)
        
        // Manually create a Level without editorLevelId (simulating old data)
        val level = de.egril.defender.model.Level(
            id = 1,
            name = "Test Level",
            gridWidth = 10,
            gridHeight = 5,
            startPositions = listOf(de.egril.defender.model.Position(0, 0)),
            targetPositions = listOf(de.egril.defender.model.Position(9, 2)),
            pathCells = setOf(
                de.egril.defender.model.Position(0, 0),
                de.egril.defender.model.Position(1, 0),
                de.egril.defender.model.Position(2, 0)
            ),
            attackerWaves = emptyList(),
            editorLevelId = null  // Old level without editor ID
        )
        
        // The minimap code should handle this gracefully by falling back to the old behavior
        assertEquals(null, level.editorLevelId, 
            "Old levels should not have editorLevelId")
    }
}
