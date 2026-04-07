package de.egril.defender.game

import de.egril.defender.editor.EditorLevel
import de.egril.defender.editor.EditorMap
import de.egril.defender.editor.EditorEnemySpawn
import de.egril.defender.editor.EditorStorage
import de.egril.defender.editor.TileType
import de.egril.defender.model.AttackerType
import de.egril.defender.model.DefenderType
import de.egril.defender.model.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration test for tower availability end-to-end from editor to game
 */
class TowerAvailabilityIntegrationTest {
    
    @Test
    fun testEditorToGameLevelConversion() {
        // Create a simple map
        val tiles = mutableMapOf<String, TileType>()
        tiles["0,0"] = TileType.SPAWN_POINT
        tiles["1,0"] = TileType.PATH
        tiles["2,0"] = TileType.PATH
        tiles["3,0"] = TileType.TARGET
        tiles["0,1"] = TileType.BUILD_AREA
        tiles["1,1"] = TileType.BUILD_AREA
        tiles["2,1"] = TileType.BUILD_AREA
        
        val map = EditorMap(
            id = "test_map_integration",
            name = "Test Map",
            width = 10,
            height = 5,
            tiles = tiles,
            readyToUse = true
        )
        
        // Save the map so convertToGameLevel can find it
        EditorStorage.saveMap(map)
        
        // Create a level with limited towers
        val editorLevel = EditorLevel(
            id = "test_level_integration",
            mapId = "test_map_integration",
            title = "Test Level",
            subtitle = "Limited Towers",
            startCoins = 100,
            startHealthPoints = 10,
            enemySpawns = listOf(
                EditorEnemySpawn(AttackerType.GOBLIN, 1, 1),
                EditorEnemySpawn(AttackerType.GOBLIN, 1, 2)
            ),
            availableTowers = setOf(
                DefenderType.SPIKE_TOWER,
                DefenderType.BOW_TOWER
            )
        )
        
        // Save the level
        EditorStorage.saveLevel(editorLevel)
        
        // Convert to game level
        val gameLevel = EditorStorage.convertToGameLevel(editorLevel, 1)
        
        assertNotNull(gameLevel, "Game level should be created")
        assertEquals(2, gameLevel.availableTowers.size, "Should have 2 available towers")
        assertTrue(
            gameLevel.availableTowers.contains(DefenderType.SPIKE_TOWER),
            "Should contain SPIKE_TOWER"
        )
        assertTrue(
            gameLevel.availableTowers.contains(DefenderType.BOW_TOWER),
            "Should contain BOW_TOWER"
        )
        assertTrue(
            !gameLevel.availableTowers.contains(DefenderType.WIZARD_TOWER),
            "Should NOT contain WIZARD_TOWER"
        )
    }
    
    @Test
    fun testEditorLevelWithNoTowersIsNotReady() {
        val editorLevel = EditorLevel(
            id = "no_towers",
            mapId = "test_map",
            title = "No Towers",
            startCoins = 100,
            enemySpawns = listOf(EditorEnemySpawn(AttackerType.GOBLIN, 1, 1)),
            availableTowers = emptySet()
        )
        
        assertTrue(
            !editorLevel.isReadyToPlay(),
            "Level with no available towers should not be ready to play"
        )
    }
    
    @Test
    fun testEditorLevelWithOneTowerIsReady() {
        val editorLevel = EditorLevel(
            id = "one_tower",
            mapId = "test_map",
            title = "One Tower",
            startCoins = 100,
            enemySpawns = listOf(EditorEnemySpawn(AttackerType.GOBLIN, 1, 1)),
            availableTowers = setOf(DefenderType.SPIKE_TOWER)
        )
        
        assertTrue(
            editorLevel.isReadyToPlay(),
            "Level with one available tower should be ready to play"
        )
    }
}
