package de.egril.defender.game

import de.egril.defender.editor.EditorMap
import de.egril.defender.editor.TileType
import de.egril.defender.model.Position
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for river tile functionality
 */
class RiverTileTest {
    
    /**
     * Test that a map with rivers and bridge-building enemies (ORK) is valid
     */
    @Test
    fun testMapWithRiverAndOrkIsValid() {
        // Create a simple map where river separates spawn and target
        // Path: (0,0) -> river at (1,0) -> (2,0)
        val tiles = mutableMapOf<String, TileType>()
        tiles["0,0"] = TileType.SPAWN_POINT
        tiles["1,0"] = TileType.RIVER  // River blocks the path
        tiles["2,0"] = TileType.TARGET
        
        val map = EditorMap(
            id = "test-river-map",
            name = "Test River Map",
            width = 3,
            height = 1,
            tiles = tiles
        )
        
        // Without bridge-building enemies, the map should be invalid
        assertFalse(map.validateReadyToUse(includeRiversAsWalkable = false))
        
        // With bridge-building enemies (rivers walkable), the map should be valid
        assertTrue(map.validateReadyToUse(includeRiversAsWalkable = true))
    }
    
    /**
     * Test that a map with only path (no rivers) is valid regardless of enemy types
     */
    @Test
    fun testMapWithoutRiverIsValid() {
        // Create a simple map with direct path from spawn to target
        val tiles = mutableMapOf<String, TileType>()
        tiles["0,0"] = TileType.SPAWN_POINT
        tiles["1,0"] = TileType.PATH
        tiles["2,0"] = TileType.TARGET
        
        val map = EditorMap(
            id = "test-path-map",
            name = "Test Path Map",
            width = 3,
            height = 1,
            tiles = tiles
        )
        
        // Should be valid regardless of river walkability setting
        assertTrue(map.validateReadyToUse(includeRiversAsWalkable = false))
        assertTrue(map.validateReadyToUse(includeRiversAsWalkable = true))
    }
    
    /**
     * Test that a map with rivers but no bridge-building enemies is invalid
     */
    @Test
    fun testMapWithRiverAndNoOrkIsInvalid() {
        // Create a simple map where river blocks the only path
        val tiles = mutableMapOf<String, TileType>()
        tiles["0,0"] = TileType.SPAWN_POINT
        tiles["1,0"] = TileType.RIVER  // River blocks the path
        tiles["2,0"] = TileType.TARGET
        
        val map = EditorMap(
            id = "test-river-no-ork-map",
            name = "Test River No Ork Map",
            width = 3,
            height = 1,
            tiles = tiles
        )
        
        // Without bridge-building enemies, the map should be invalid
        assertFalse(map.validateReadyToUse(includeRiversAsWalkable = false))
    }
    
    /**
     * Test that rivers are NOT included in pathCells (enemies can't walk on them in gameplay)
     */
    @Test
    fun testRiversNotInPathCells() {
        val tiles = mutableMapOf<String, TileType>()
        tiles["0,0"] = TileType.PATH
        tiles["1,0"] = TileType.RIVER
        tiles["2,0"] = TileType.PATH
        
        val map = EditorMap(
            id = "test-river-path-map",
            name = "Test River Path Map",
            width = 3,
            height = 1,
            tiles = tiles
        )
        
        val pathCells = map.getPathCells()
        val riverCells = map.getRiverCells()
        
        // Path cells should not include river cells
        assertTrue(pathCells.contains(Position(0, 0)))
        assertFalse(pathCells.contains(Position(1, 0)))
        assertTrue(pathCells.contains(Position(2, 0)))
        
        // River cells should be separate
        assertFalse(riverCells.contains(Position(0, 0)))
        assertTrue(riverCells.contains(Position(1, 0)))
        assertFalse(riverCells.contains(Position(2, 0)))
    }
    
    /**
     * Test that a map with river can have alternate path (river not required)
     */
    @Test
    fun testMapWithRiverAndAlternatePath() {
        // Create a map where there's a river, but also an alternate path
        // Layout:
        //   (0,0) spawn -> (1,0) path -> (2,0) target
        //                  (1,1) river (not needed for path)
        val tiles = mutableMapOf<String, TileType>()
        tiles["0,0"] = TileType.SPAWN_POINT
        tiles["1,0"] = TileType.PATH
        tiles["1,1"] = TileType.RIVER  // River exists but not blocking
        tiles["2,0"] = TileType.TARGET
        
        val map = EditorMap(
            id = "test-river-alternate-map",
            name = "Test River Alternate Map",
            width = 3,
            height = 2,
            tiles = tiles
        )
        
        // Should be valid even without rivers being walkable, because alternate path exists
        assertTrue(map.validateReadyToUse(includeRiversAsWalkable = false))
        assertTrue(map.validateReadyToUse(includeRiversAsWalkable = true))
    }
    
    /**
     * Test that a map with hexagonal river crossing works correctly
     * Uses hex neighbors for validation
     */
    @Test
    fun testHexagonalRiverCrossing() {
        // Create a hex map where river blocks direct path
        // But with river walkability, path exists through hex neighbors
        val tiles = mutableMapOf<String, TileType>()
        tiles["0,0"] = TileType.SPAWN_POINT
        tiles["1,0"] = TileType.RIVER
        tiles["2,0"] = TileType.RIVER
        tiles["3,0"] = TileType.TARGET
        
        val map = EditorMap(
            id = "test-hex-river-map",
            name = "Test Hex River Map",
            width = 4,
            height = 1,
            tiles = tiles
        )
        
        // Without river walkability, no path exists
        assertFalse(map.validateReadyToUse(includeRiversAsWalkable = false))
        
        // With river walkability, path exists
        assertTrue(map.validateReadyToUse(includeRiversAsWalkable = true))
    }
}
