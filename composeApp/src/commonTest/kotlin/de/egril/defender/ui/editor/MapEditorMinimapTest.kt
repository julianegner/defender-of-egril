package de.egril.defender.ui.editor

import de.egril.defender.editor.EditorMap
import de.egril.defender.editor.TileType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * Tests for map editor minimap update logic
 * Verifies that when tiles are updated, the map reflects those changes
 */
class MapEditorMinimapTest {
    
    @Test
    fun testMapCopyWithUpdatedTiles() {
        // Create an initial map with some tiles
        val originalTiles = mapOf(
            "0,0" to TileType.SPAWN_POINT,
            "1,0" to TileType.PATH,
            "2,0" to TileType.TARGET
        )
        
        val originalMap = EditorMap(
            id = "test_map",
            name = "Test Map",
            width = 10,
            height = 10,
            tiles = originalTiles
        )
        
        // Simulate user painting a new tile (this is what happens in MapEditorView)
        val updatedTiles = originalTiles.toMutableMap().apply {
            this["3,0"] = TileType.BUILD_AREA
        }
        
        // Create updated map (this simulates what the currentMap derived state does)
        val updatedMap = originalMap.copy(tiles = updatedTiles.toMap())
        
        // Verify original map is unchanged
        assertEquals(3, originalMap.tiles.size)
        assertEquals(null, originalMap.tiles["3,0"])
        
        // Verify updated map has the new tile
        assertEquals(4, updatedMap.tiles.size)
        assertEquals(TileType.BUILD_AREA, updatedMap.tiles["3,0"])
        
        // Verify the maps are different objects
        assertNotEquals(originalMap.tiles, updatedMap.tiles)
    }
    
    @Test
    fun testMultipleTileUpdates() {
        // Create initial map
        val initialMap = EditorMap(
            id = "test_map",
            name = "Test Map",
            width = 10,
            height = 10,
            tiles = emptyMap()
        )
        
        // Simulate multiple tile updates (like brush painting)
        var tiles = initialMap.tiles.toMutableMap()
        
        // Paint first tile
        tiles = tiles.toMutableMap().apply {
            this["5,5"] = TileType.PATH
        }
        val map1 = initialMap.copy(tiles = tiles.toMap())
        assertEquals(1, map1.tiles.size)
        assertEquals(TileType.PATH, map1.getTileType(5, 5))
        
        // Paint second tile
        tiles = tiles.toMutableMap().apply {
            this["5,6"] = TileType.PATH
        }
        val map2 = initialMap.copy(tiles = tiles.toMap())
        assertEquals(2, map2.tiles.size)
        assertEquals(TileType.PATH, map2.getTileType(5, 5))
        assertEquals(TileType.PATH, map2.getTileType(5, 6))
        
        // Paint third tile
        tiles = tiles.toMutableMap().apply {
            this["5,7"] = TileType.BUILD_AREA
        }
        val map3 = initialMap.copy(tiles = tiles.toMap())
        assertEquals(3, map3.tiles.size)
        assertEquals(TileType.PATH, map3.getTileType(5, 5))
        assertEquals(TileType.PATH, map3.getTileType(5, 6))
        assertEquals(TileType.BUILD_AREA, map3.getTileType(5, 7))
    }
    
    @Test
    fun testTileOverwrite() {
        // Test that painting over an existing tile updates it
        val initialTiles = mapOf("5,5" to TileType.PATH)
        
        val map = EditorMap(
            id = "test_map",
            name = "Test Map",
            width = 10,
            height = 10,
            tiles = initialTiles
        )
        
        // Overwrite the tile with a different type
        val updatedTiles = map.tiles.toMutableMap().apply {
            this["5,5"] = TileType.BUILD_AREA
        }
        val updatedMap = map.copy(tiles = updatedTiles.toMap())
        
        // Verify the tile was updated, not added
        assertEquals(1, updatedMap.tiles.size)
        assertEquals(TileType.BUILD_AREA, updatedMap.getTileType(5, 5))
        assertEquals(TileType.PATH, map.getTileType(5, 5)) // Original unchanged
    }
}
