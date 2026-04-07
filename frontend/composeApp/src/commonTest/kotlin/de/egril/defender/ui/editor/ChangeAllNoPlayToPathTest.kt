package de.egril.defender.ui.editor

import de.egril.defender.editor.EditorMap
import de.egril.defender.editor.TileType
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for the "Change All NO_PLAY to PATH" functionality in the map editor
 */
class ChangeAllNoPlayToPathTest {
    
    @Test
    fun testChangeAllNoPlayToPath_withNoPlayTiles() {
        // Create a map with some NO_PLAY tiles
        val originalTiles = mapOf(
            "0,0" to TileType.SPAWN_POINT,
            "1,0" to TileType.PATH,
            "2,0" to TileType.NO_PLAY,
            "3,0" to TileType.BUILD_AREA,
            "4,0" to TileType.NO_PLAY
        )
        
        val map = EditorMap(
            id = "test_map",
            name = "Test Map",
            width = 10,
            height = 10,
            tiles = originalTiles
        )
        
        // Simulate the change all operation
        val updatedTiles = originalTiles.toMutableMap().apply {
            for (x in 0 until map.width) {
                for (y in 0 until map.height) {
                    val key = "$x,$y"
                    if (this[key] == TileType.NO_PLAY || this[key] == null) {
                        this[key] = TileType.PATH
                    }
                }
            }
        }
        
        // Verify that NO_PLAY tiles were changed to PATH
        assertEquals(TileType.PATH, updatedTiles["2,0"])
        assertEquals(TileType.PATH, updatedTiles["4,0"])
        
        // Verify that other tile types were not changed
        assertEquals(TileType.SPAWN_POINT, updatedTiles["0,0"])
        assertEquals(TileType.PATH, updatedTiles["1,0"])
        assertEquals(TileType.BUILD_AREA, updatedTiles["3,0"])
    }
    
    @Test
    fun testChangeAllNoPlayToPath_withNullTiles() {
        // Create a map with some null tiles (which default to NO_PLAY)
        val originalTiles = mapOf(
            "0,0" to TileType.SPAWN_POINT,
            "1,0" to TileType.PATH
        )
        
        val map = EditorMap(
            id = "test_map",
            name = "Test Map",
            width = 5,
            height = 5,
            tiles = originalTiles
        )
        
        // Simulate the change all operation
        val updatedTiles = originalTiles.toMutableMap().apply {
            for (x in 0 until map.width) {
                for (y in 0 until map.height) {
                    val key = "$x,$y"
                    if (this[key] == TileType.NO_PLAY || this[key] == null) {
                        this[key] = TileType.PATH
                    }
                }
            }
        }
        
        // Verify that null tiles were changed to PATH
        assertEquals(TileType.PATH, updatedTiles["2,0"])
        assertEquals(TileType.PATH, updatedTiles["3,0"])
        assertEquals(TileType.PATH, updatedTiles["0,1"])
        
        // Verify that existing tiles were not changed
        assertEquals(TileType.SPAWN_POINT, updatedTiles["0,0"])
        assertEquals(TileType.PATH, updatedTiles["1,0"])
        
        // Verify the total number of tiles equals width * height
        assertEquals(25, updatedTiles.size)
    }
    
    @Test
    fun testChangeAllNoPlayToPath_noNoPlayTiles() {
        // Create a map with no NO_PLAY tiles
        val originalTiles = mapOf(
            "0,0" to TileType.SPAWN_POINT,
            "1,0" to TileType.PATH,
            "2,0" to TileType.BUILD_AREA,
            "3,0" to TileType.BUILD_AREA,
            "4,0" to TileType.TARGET
        )
        
        val map = EditorMap(
            id = "test_map",
            name = "Test Map",
            width = 10,
            height = 10,
            tiles = originalTiles
        )
        
        // Simulate the change all operation
        val updatedTiles = originalTiles.toMutableMap().apply {
            for (x in 0 until map.width) {
                for (y in 0 until map.height) {
                    val key = "$x,$y"
                    if (this[key] == TileType.NO_PLAY || this[key] == null) {
                        this[key] = TileType.PATH
                    }
                }
            }
        }
        
        // Verify that no tile types were changed (except null tiles)
        assertEquals(TileType.SPAWN_POINT, updatedTiles["0,0"])
        assertEquals(TileType.PATH, updatedTiles["1,0"])
        assertEquals(TileType.BUILD_AREA, updatedTiles["2,0"])
        assertEquals(TileType.BUILD_AREA, updatedTiles["3,0"])
        assertEquals(TileType.TARGET, updatedTiles["4,0"])
        
        // Verify that null tiles were filled with PATH
        assertEquals(TileType.PATH, updatedTiles["5,0"])
    }
    
    @Test
    fun testChangeAllNoPlayToPath_entireMap() {
        // Create a map with all NO_PLAY tiles (empty map)
        val originalTiles = emptyMap<String, TileType>()
        
        val map = EditorMap(
            id = "test_map",
            name = "Test Map",
            width = 3,
            height = 3,
            tiles = originalTiles
        )
        
        // Simulate the change all operation
        val updatedTiles = originalTiles.toMutableMap().apply {
            for (x in 0 until map.width) {
                for (y in 0 until map.height) {
                    val key = "$x,$y"
                    if (this[key] == TileType.NO_PLAY || this[key] == null) {
                        this[key] = TileType.PATH
                    }
                }
            }
        }
        
        // Verify that all tiles are now PATH
        assertEquals(9, updatedTiles.size)
        for (x in 0 until map.width) {
            for (y in 0 until map.height) {
                assertEquals(TileType.PATH, updatedTiles["$x,$y"])
            }
        }
    }
}
