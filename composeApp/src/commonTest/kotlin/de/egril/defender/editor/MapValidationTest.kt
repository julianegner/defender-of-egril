package de.egril.defender.editor

import de.egril.defender.model.Position
import de.egril.defender.model.getHexNeighbors
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MapValidationTest {
    
    @Test
    fun testMapWithAllSpawnPointsConnectedIsValid() {
        // Create a simple map where all spawn points can reach the target
        val tiles = mutableMapOf<String, TileType>()
        
        // 10x10 map with spawn points and target
        tiles["0,0"] = TileType.SPAWN_POINT
        tiles["9,0"] = TileType.SPAWN_POINT
        tiles["0,9"] = TileType.SPAWN_POINT
        tiles["9,9"] = TileType.SPAWN_POINT
        tiles["5,5"] = TileType.TARGET
        
        // Create paths connecting all corners to center
        // Connect top-left (0,0) to center
        for (i in 0..5) {
            for (j in 0..5) {
                if (!tiles.containsKey("$i,$j")) {
                    tiles["$i,$j"] = TileType.PATH
                }
            }
        }
        // Connect top-right (9,0) to center
        for (i in 5..9) {
            for (j in 0..5) {
                if (!tiles.containsKey("$i,$j")) {
                    tiles["$i,$j"] = TileType.PATH
                }
            }
        }
        // Connect bottom-left (0,9) to center
        for (i in 0..5) {
            for (j in 5..9) {
                if (!tiles.containsKey("$i,$j")) {
                    tiles["$i,$j"] = TileType.PATH
                }
            }
        }
        // Connect bottom-right (9,9) to center
        for (i in 5..9) {
            for (j in 5..9) {
                if (!tiles.containsKey("$i,$j")) {
                    tiles["$i,$j"] = TileType.PATH
                }
            }
        }
        
        val map = EditorMap(
            id = "test_all_connected",
            name = "Test All Connected",
            width = 10,
            height = 10,
            tiles = tiles,
            readyToUse = false
        )
        
        assertTrue(map.validateReadyToUse(), "Map with all spawn points connected should be valid")
    }
    
    @Test
    fun testMapWithDisconnectedSpawnPointIsInvalid() {
        // Create a map where one spawn point cannot reach the target
        val tiles = mutableMapOf<String, TileType>()
        
        // 10x10 map with spawn points in corners and target in center
        tiles["0,0"] = TileType.SPAWN_POINT  // Connected
        tiles["9,0"] = TileType.SPAWN_POINT  // NOT connected (isolated)
        tiles["0,9"] = TileType.SPAWN_POINT  // Connected
        tiles["9,9"] = TileType.SPAWN_POINT  // Connected
        tiles["5,5"] = TileType.TARGET
        
        // Create path connecting only 3 corners to center (not top-right)
        // Horizontal path on left half
        for (i in 0..5) {
            tiles["$i,5"] = TileType.PATH
        }
        // Vertical path
        for (i in 0..9) {
            tiles["5,$i"] = TileType.PATH
        }
        
        val map = EditorMap(
            id = "test_one_disconnected",
            name = "Test One Disconnected",
            width = 10,
            height = 10,
            tiles = tiles,
            readyToUse = false
        )
        
        assertFalse(map.validateReadyToUse(), "Map with disconnected spawn point should be invalid")
    }
    
    @Test
    fun testMapWithNoPathCellsIsInvalid() {
        // Create a map with spawn points and target but no connecting path
        val tiles = mutableMapOf<String, TileType>()
        
        tiles["0,0"] = TileType.SPAWN_POINT
        tiles["5,5"] = TileType.TARGET
        
        val map = EditorMap(
            id = "test_no_path",
            name = "Test No Path",
            width = 10,
            height = 10,
            tiles = tiles,
            readyToUse = false
        )
        
        assertFalse(map.validateReadyToUse(), "Map with no connecting path should be invalid")
    }
    
    @Test
    fun testMapWithNoSpawnPointsIsInvalid() {
        val tiles = mutableMapOf<String, TileType>()
        tiles["5,5"] = TileType.TARGET
        
        val map = EditorMap(
            id = "test_no_spawn",
            name = "Test No Spawn",
            width = 10,
            height = 10,
            tiles = tiles,
            readyToUse = false
        )
        
        assertFalse(map.validateReadyToUse(), "Map with no spawn points should be invalid")
    }
    
    @Test
    fun testMapWithNoTargetIsInvalid() {
        val tiles = mutableMapOf<String, TileType>()
        tiles["0,0"] = TileType.SPAWN_POINT
        
        val map = EditorMap(
            id = "test_no_target",
            name = "Test No Target",
            width = 10,
            height = 10,
            tiles = tiles,
            readyToUse = false
        )
        
        assertFalse(map.validateReadyToUse(), "Map with no target should be invalid")
    }
    
    @Test
    fun testSpiralMapAllSpawnPointsConnected() {
        // Test the actual spiral map to ensure all spawn points can reach the target
        val spiralMap = EditorStorage.getMap("map_spiral")
        if (spiralMap == null) {
            println("Spiral map not found, skipping test")
            return
        }
        
        val spawnPoints = spiralMap.getSpawnPoints()
        val target = spiralMap.getTarget()
        
        if (target == null) {
            assertFalse(spiralMap.readyToUse, "Spiral map without target should not be ready")
            return
        }
        
        // Build set of traversable cells
        val pathCells = spiralMap.getPathCells().toMutableSet()
        pathCells.addAll(spawnPoints)
        pathCells.add(target)
        
        // Check each spawn point individually to provide detailed diagnostics
        val unreachableSpawns = mutableListOf<Position>()
        spawnPoints.forEach { spawn ->
            // Implement BFS to check connectivity from spawn to target
            val visited = mutableSetOf(spawn)
            val queue = mutableListOf(spawn)
            var found = false
            
            while (queue.isNotEmpty() && !found) {
                val current = queue.removeAt(0)
                
                if (current == target) {
                    found = true
                    break
                }
                
                // Check hex neighbors using existing utility function
                val neighbors = current.getHexNeighbors()
                
                for (neighbor in neighbors) {
                    if (neighbor == target) {
                        found = true
                        break
                    }
                    
                    if (neighbor !in visited && neighbor in pathCells) {
                        visited.add(neighbor)
                        queue.add(neighbor)
                    }
                }
            }
            
            if (!found) {
                unreachableSpawns.add(spawn)
            }
        }
        
        if (unreachableSpawns.isEmpty()) {
            assertTrue(spiralMap.readyToUse, 
                "Spiral map with all spawn points connected should be ready to use")
        } else {
            assertFalse(spiralMap.readyToUse, 
                "Spiral map with disconnected spawn points should not be ready. Unreachable: $unreachableSpawns")
        }
    }
    
    @Test
    fun testMapWithPathButDisconnectedSpawnIsInvalid() {
        // Create a map where paths exist but one spawn point cannot reach them
        val tiles = mutableMapOf<String, TileType>()
        
        // 15x15 map with spawn points in corners and target in center
        tiles["0,0"] = TileType.SPAWN_POINT   // Connected via path
        tiles["14,0"] = TileType.SPAWN_POINT  // NOT connected (isolated)
        tiles["7,7"] = TileType.TARGET
        
        // Create path from top-left corner to center
        for (i in 0..7) {
            tiles["$i,7"] = TileType.PATH
        }
        
        // Note: Top-right corner (14,0) has NO path to the target
        
        val map = EditorMap(
            id = "test_path_disconnected",
            name = "Test Path with Disconnected Spawn",
            width = 15,
            height = 15,
            tiles = tiles,
            readyToUse = false
        )
        
        assertFalse(map.validateReadyToUse(), 
            "Map with path but disconnected spawn point should be invalid")
    }
}
