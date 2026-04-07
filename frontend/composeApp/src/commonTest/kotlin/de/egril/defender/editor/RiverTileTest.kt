package de.egril.defender.editor

import de.egril.defender.model.Position
import de.egril.defender.model.RiverFlow
import de.egril.defender.model.RiverTile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RiverTileTest {
    
    @Test
    fun testRiverTileCreation() {
        val position = Position(5, 5)
        val riverTile = RiverTile(
            position = position,
            flowDirection = RiverFlow.EAST,
            flowSpeed = 2
        )
        
        assertEquals(position, riverTile.position)
        assertEquals(RiverFlow.EAST, riverTile.flowDirection)
        assertEquals(2, riverTile.flowSpeed)
    }
    
    @Test
    fun testRiverTileDefaultValues() {
        val position = Position(3, 3)
        val riverTile = RiverTile(position = position)
        
        assertEquals(RiverFlow.NONE, riverTile.flowDirection)
        assertEquals(1, riverTile.flowSpeed)
    }
    
    @Test
    fun testMapWithRiverTiles() {
        val tiles = mutableMapOf<String, TileType>()
        tiles["0,0"] = TileType.SPAWN_POINT
        tiles["5,5"] = TileType.TARGET
        tiles["2,2"] = TileType.RIVER
        tiles["3,3"] = TileType.RIVER
        
        // Add paths
        for (i in 0..5) {
            tiles["$i,2"] = if (i == 2) TileType.RIVER else TileType.PATH
        }
        
        val riverTiles = mutableMapOf<String, RiverTile>()
        riverTiles["2,2"] = RiverTile(
            position = Position(2, 2),
            flowDirection = RiverFlow.EAST,
            flowSpeed = 2
        )
        riverTiles["3,3"] = RiverTile(
            position = Position(3, 3),
            flowDirection = RiverFlow.SOUTH_EAST,
            flowSpeed = 1
        )
        
        val map = EditorMap(
            id = "test_river_map",
            name = "Test River Map",
            width = 10,
            height = 10,
            tiles = tiles,
            riverTiles = riverTiles
        )
        
        assertEquals(2, map.riverTiles.size)
        assertNotNull(map.getRiverTile(2, 2))
        assertNotNull(map.getRiverTile(3, 3))
        assertEquals(RiverFlow.EAST, map.getRiverTile(2, 2)?.flowDirection)
    }
    
    @Test
    fun testRiverTilesAreTraversable() {
        // Create a simple map with river tiles in a straight path
        val tiles = mutableMapOf<String, TileType>()
        tiles["0,0"] = TileType.SPAWN_POINT
        tiles["5,0"] = TileType.TARGET
        
        // Create a straight horizontal path with some river tiles
        for (i in 0..5) {
            if (i == 2 || i == 3) {
                // Two river tiles in the middle
                tiles["$i,0"] = TileType.RIVER
            } else if (i == 0 || i == 5) {
                // Spawn and target are already set
            } else {
                tiles["$i,0"] = TileType.PATH
            }
        }
        
        val map = EditorMap(
            id = "test_river_traversal",
            name = "Test River Traversal",
            width = 6,
            height = 1,
            tiles = tiles,
            readyToUse = false
        )
        
        // River tiles should be traversable, so validation should pass
        assertTrue(map.validateReadyToUse(), "Map with river tiles connecting spawn to target should be valid")
    }
    
    @Test
    fun testRiverTileSerialization() {
        // Test serialization and deserialization
        val tiles = mutableMapOf<String, TileType>()
        tiles["0,0"] = TileType.SPAWN_POINT
        tiles["5,5"] = TileType.TARGET
        tiles["2,2"] = TileType.RIVER
        
        val riverTiles = mutableMapOf<String, RiverTile>()
        riverTiles["2,2"] = RiverTile(
            position = Position(2, 2),
            flowDirection = RiverFlow.NORTH_EAST,
            flowSpeed = 2
        )
        
        val map = EditorMap(
            id = "test_serialization",
            name = "Test Serialization",
            width = 10,
            height = 10,
            tiles = tiles,
            riverTiles = riverTiles
        )
        
        // Serialize
        val json = EditorJsonSerializer.serializeMap(map)
        
        // Deserialize
        val deserializedMap = EditorJsonSerializer.deserializeMap(json)
        
        assertNotNull(deserializedMap)
        assertEquals(1, deserializedMap.riverTiles.size)
        val riverTile = deserializedMap.getRiverTile(2, 2)
        assertNotNull(riverTile)
        assertEquals(RiverFlow.NORTH_EAST, riverTile.flowDirection)
        assertEquals(2, riverTile.flowSpeed)
    }
    
    @Test
    fun testMultipleRiverTilesSerialization() {
        // Test serialization and deserialization with multiple river tiles
        val tiles = mutableMapOf<String, TileType>()
        tiles["0,0"] = TileType.SPAWN_POINT
        tiles["9,9"] = TileType.TARGET
        tiles["2,2"] = TileType.RIVER
        tiles["3,3"] = TileType.RIVER
        tiles["4,4"] = TileType.RIVER
        
        val riverTiles = mutableMapOf<String, RiverTile>()
        riverTiles["2,2"] = RiverTile(
            position = Position(2, 2),
            flowDirection = RiverFlow.EAST,
            flowSpeed = 2
        )
        riverTiles["3,3"] = RiverTile(
            position = Position(3, 3),
            flowDirection = RiverFlow.NORTH_EAST,
            flowSpeed = 1
        )
        riverTiles["4,4"] = RiverTile(
            position = Position(4, 4),
            flowDirection = RiverFlow.SOUTH_WEST,
            flowSpeed = 2
        )
        
        val map = EditorMap(
            id = "test_multiple_rivers",
            name = "Test Multiple Rivers",
            width = 10,
            height = 10,
            tiles = tiles,
            riverTiles = riverTiles
        )
        
        // Serialize
        val json = EditorJsonSerializer.serializeMap(map)
        
        // Deserialize
        val deserializedMap = EditorJsonSerializer.deserializeMap(json)
        
        assertNotNull(deserializedMap)
        assertEquals(3, deserializedMap.riverTiles.size, "Should deserialize all 3 river tiles")
        
        val river1 = deserializedMap.getRiverTile(2, 2)
        assertNotNull(river1)
        assertEquals(RiverFlow.EAST, river1.flowDirection)
        assertEquals(2, river1.flowSpeed)
        
        val river2 = deserializedMap.getRiverTile(3, 3)
        assertNotNull(river2)
        assertEquals(RiverFlow.NORTH_EAST, river2.flowDirection)
        assertEquals(1, river2.flowSpeed)
        
        val river3 = deserializedMap.getRiverTile(4, 4)
        assertNotNull(river3)
        assertEquals(RiverFlow.SOUTH_WEST, river3.flowDirection)
        assertEquals(2, river3.flowSpeed)
    }
}
