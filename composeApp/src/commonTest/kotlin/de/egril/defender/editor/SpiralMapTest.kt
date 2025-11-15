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
}
