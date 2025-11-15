package de.egril.defender.editor

import de.egril.defender.model.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SpiralMapTest {
    @Test
    fun testSpiralMapExists() {
        // Retrieve the spiral map
        val spiralMap = EditorStorage.getMap("map_spiral")
        
        assertNotNull(spiralMap, "Spiral map should exist")
        assertEquals("map_spiral", spiralMap.id)
        assertEquals("Spiral Challenge Map", spiralMap.name)
        assertEquals(40, spiralMap.width)
        assertEquals(40, spiralMap.height)
    }
    
    @Test
    fun testSpiralMapHasFourSpawnPoints() {
        val spiralMap = EditorStorage.getMap("map_spiral")
        assertNotNull(spiralMap, "Spiral map should exist")
        
        val spawnPoints = spiralMap.getSpawnPoints()
        assertEquals(4, spawnPoints.size, "Should have 4 spawn points")
        
        // Check corners
        assertTrue(spawnPoints.contains(Position(0, 0)), "Should have spawn at (0,0)")
        assertTrue(spawnPoints.contains(Position(39, 0)), "Should have spawn at (39,0)")
        assertTrue(spawnPoints.contains(Position(0, 39)), "Should have spawn at (0,39)")
        assertTrue(spawnPoints.contains(Position(39, 39)), "Should have spawn at (39,39)")
    }
    
    @Test
    fun testSpiralMapHasCenterTarget() {
        val spiralMap = EditorStorage.getMap("map_spiral")
        assertNotNull(spiralMap, "Spiral map should exist")
        
        val target = spiralMap.getTarget()
        assertNotNull(target, "Should have a target")
        assertEquals(Position(20, 20), target, "Target should be at center (20,20)")
    }
    
    @Test
    fun testSpiralMapHasPath() {
        val spiralMap = EditorStorage.getMap("map_spiral")
        assertNotNull(spiralMap, "Spiral map should exist")
        
        val pathCells = spiralMap.getPathCells()
        assertTrue(pathCells.isNotEmpty(), "Should have path cells")
        assertTrue(pathCells.size > 50, "Should have a substantial path (at least 50 cells)")
        
        println("Path cells count: ${pathCells.size}")
    }
    
    @Test
    fun testSpiralMapIsReadyToUse() {
        val spiralMap = EditorStorage.getMap("map_spiral")
        assertNotNull(spiralMap, "Spiral map should exist")
        
        assertTrue(spiralMap.readyToUse, "Spiral map should be ready to use")
    }
    
    @Test
    fun testSpiralLevelExists() {
        val level = EditorStorage.getLevel("level_7")
        assertNotNull(level, "Level 7 should exist")
        assertEquals("level_7", level.id)
        assertEquals("map_spiral", level.mapId)
        assertEquals("The Spiral Challenge", level.title)
        assertEquals(250, level.startCoins)
        assertEquals(10, level.startHealthPoints)
        assertTrue(level.enemySpawns.isNotEmpty(), "Should have enemy spawns")
    }
    
    @Test
    fun testSpiralMapStructure() {
        val spiralMap = EditorStorage.getMap("map_spiral")
        assertNotNull(spiralMap, "Spiral map should exist")
        
        // Count different tile types
        val tileCounts = mutableMapOf<TileType, Int>()
        for (x in 0 until spiralMap.width) {
            for (y in 0 until spiralMap.height) {
                val tileType = spiralMap.getTileType(x, y)
                tileCounts[tileType] = (tileCounts[tileType] ?: 0) + 1
            }
        }
        
        println("\n=== Spiral Map Structure ===")
        println("Map size: ${spiralMap.width}x${spiralMap.height}")
        println("Tile type distribution:")
        tileCounts.forEach { (type, count) ->
            println("  $type: $count tiles (${count * 100 / (spiralMap.width * spiralMap.height)}%)")
        }
        
        // Verify we have all expected tile types
        assertTrue(tileCounts[TileType.SPAWN_POINT]!! >= 4, "Should have at least 4 spawn points")
        assertTrue(tileCounts[TileType.TARGET]!! >= 1, "Should have at least 1 target")
        assertTrue(tileCounts[TileType.PATH]!! > 50, "Should have substantial path")
        assertTrue(tileCounts[TileType.BUILD_AREA]!! > 0, "Should have build areas")
        assertTrue(tileCounts[TileType.NO_PLAY]!! > 0, "Should have non-playable areas")
    }
    
    @Test
    fun testPlainsMapExists() {
        val plainsMap = EditorStorage.getMap("map_plains")
        
        assertNotNull(plainsMap, "Plains map should exist")
        assertEquals("map_plains", plainsMap.id)
        assertEquals("The Plains", plainsMap.name)
        assertEquals(40, plainsMap.width)
        assertEquals(40, plainsMap.height)
    }
    
    @Test
    fun testPlainsMapHasFourSpawnPoints() {
        val plainsMap = EditorStorage.getMap("map_plains")
        assertNotNull(plainsMap, "Plains map should exist")
        
        val spawnPoints = plainsMap.getSpawnPoints()
        assertEquals(4, spawnPoints.size, "Should have 4 spawn points")
        
        // Check corners
        assertTrue(spawnPoints.contains(Position(0, 0)), "Should have spawn at (0,0)")
        assertTrue(spawnPoints.contains(Position(39, 0)), "Should have spawn at (39,0)")
        assertTrue(spawnPoints.contains(Position(0, 39)), "Should have spawn at (0,39)")
        assertTrue(spawnPoints.contains(Position(39, 39)), "Should have spawn at (39,39)")
    }
    
    @Test
    fun testPlainsMapHasCenterTarget() {
        val plainsMap = EditorStorage.getMap("map_plains")
        assertNotNull(plainsMap, "Plains map should exist")
        
        val target = plainsMap.getTarget()
        assertNotNull(target, "Should have a target")
        assertEquals(Position(20, 20), target, "Target should be at center (20,20)")
    }
    
    @Test
    fun testPlainsMapHasFourIslands() {
        val plainsMap = EditorStorage.getMap("map_plains")
        assertNotNull(plainsMap, "Plains map should exist")
        
        val buildIslands = plainsMap.getBuildIslands()
        // Each island is 2x2 = 4 tiles, 4 islands = 16 tiles
        assertEquals(16, buildIslands.size, "Should have 16 island tiles (4 2x2 islands)")
    }
    
    @Test
    fun testPlainsMapIsReadyToUse() {
        val plainsMap = EditorStorage.getMap("map_plains")
        assertNotNull(plainsMap, "Plains map should exist")
        
        assertTrue(plainsMap.readyToUse, "Plains map should be ready to use")
    }
    
    @Test
    fun testPlainsLevelExists() {
        val level = EditorStorage.getLevel("level_8")
        assertNotNull(level, "Level 8 should exist")
        assertEquals("level_8", level.id)
        assertEquals("map_plains", level.mapId)
        assertEquals("The Plains", level.title)
        assertEquals(200, level.startCoins)
        assertEquals(10, level.startHealthPoints)
        assertTrue(level.enemySpawns.isNotEmpty(), "Should have enemy spawns")
    }
    
    @Test
    fun testPlainsMapStructure() {
        val plainsMap = EditorStorage.getMap("map_plains")
        assertNotNull(plainsMap, "Plains map should exist")
        
        // Count different tile types
        val tileCounts = mutableMapOf<TileType, Int>()
        for (x in 0 until plainsMap.width) {
            for (y in 0 until plainsMap.height) {
                val tileType = plainsMap.getTileType(x, y)
                tileCounts[tileType] = (tileCounts[tileType] ?: 0) + 1
            }
        }
        
        println("\n=== Plains Map Structure ===")
        println("Map size: ${plainsMap.width}x${plainsMap.height}")
        println("Tile type distribution:")
        tileCounts.forEach { (type, count) ->
            println("  $type: $count tiles (${count * 100 / (plainsMap.width * plainsMap.height)}%)")
        }
        
        // Verify we have all expected tile types
        assertEquals(4, tileCounts[TileType.SPAWN_POINT], "Should have 4 spawn points")
        assertEquals(1, tileCounts[TileType.TARGET], "Should have 1 target")
        assertEquals(16, tileCounts[TileType.ISLAND], "Should have 16 island tiles")
        // All other tiles should be PATH
        val totalTiles = plainsMap.width * plainsMap.height
        val pathTiles = totalTiles - 4 - 1 - 16  // Total - spawns - target - islands
        assertEquals(pathTiles, tileCounts[TileType.PATH], "All other tiles should be PATH")
    }
    
    @Test
    fun testLevelSequenceUpdated() {
        val sequence = EditorStorage.getLevelSequence()
        
        println("\n=== Level Sequence ===")
        println("Sequence: ${sequence.sequence}")
        
        // Check that level_7 and level_8 come before level_5 (The Final Stand)
        val level7Index = sequence.sequence.indexOf("level_7")
        val level8Index = sequence.sequence.indexOf("level_8")
        val level5Index = sequence.sequence.indexOf("level_5")
        
        assertTrue(level7Index >= 0, "Level 7 should be in sequence")
        assertTrue(level8Index >= 0, "Level 8 should be in sequence")
        assertTrue(level5Index >= 0, "Level 5 should be in sequence")
        
        assertTrue(level7Index < level5Index, "Level 7 should come before Level 5")
        assertTrue(level8Index < level5Index, "Level 8 should come before Level 5")
    }
}
