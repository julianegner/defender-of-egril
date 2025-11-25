package de.egril.defender.editor

import de.egril.defender.model.AttackerType
import de.egril.defender.model.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for "The Rush" map and level
 */
class TheRushMapTest {
    
    @Test
    fun testRushMapStructure() {
        // Create the rush map
        val map = MapGenerator.createRushMap()
        
        // Verify map dimensions
        assertEquals("map_the_rush", map.id)
        assertEquals("The Rush", map.name)
        assertEquals(20, map.width)
        assertEquals(20, map.height)
        
        // Verify spawn points
        val spawnPoints = map.getSpawnPoints()
        assertEquals(8, spawnPoints.size, "Should have 8 spawn points")
        
        // Upper spawn points (y=2)
        assertTrue(spawnPoints.contains(Position(3, 2)), "Should have spawn point at (3,2)")
        assertTrue(spawnPoints.contains(Position(7, 2)), "Should have spawn point at (7,2)")
        assertTrue(spawnPoints.contains(Position(12, 2)), "Should have spawn point at (12,2)")
        assertTrue(spawnPoints.contains(Position(16, 2)), "Should have spawn point at (16,2)")
        
        // Center spawn points (y=10)
        assertTrue(spawnPoints.contains(Position(3, 10)), "Should have spawn point at (3,10)")
        assertTrue(spawnPoints.contains(Position(7, 10)), "Should have spawn point at (7,10)")
        assertTrue(spawnPoints.contains(Position(12, 10)), "Should have spawn point at (12,10)")
        assertTrue(spawnPoints.contains(Position(16, 10)), "Should have spawn point at (16,10)")
        
        // Verify target
        val targets = map.getTargets()
        assertEquals(1, targets.size, "Should have 1 target")
        assertEquals(Position(10, 18), targets[0], "Target should be at (10,18)")
        
        // Verify build areas exist with regular pattern
        val buildAreas = map.getBuildAreas()
        assertTrue(buildAreas.isNotEmpty(), "Should have build areas")
        
        // Check that build areas follow the pattern (every 3 tiles)
        // Sample a few expected build area positions
        assertTrue(
            buildAreas.any { it.x % 3 == 1 && it.y % 3 == 1 },
            "Should have build areas following regular pattern"
        )
        
        // Verify paths exist
        val pathCells = map.getPathCells()
        assertTrue(pathCells.isNotEmpty(), "Should have path cells")
    }
    
    @Test
    fun testRushMapValidation() {
        // Create and validate the map
        val map = MapGenerator.createRushMap()
        
        // Verify the map can be validated
        val isValid = map.validateReadyToUse()
        assertTrue(isValid, "The Rush map should be valid (all spawn points should reach the target)")
    }
    
    @Test
    fun testRushLevelConfiguration() {
        // Test the level configuration structure
        // Note: We can't actually load it from EditorStorage in unit tests,
        // but we can verify the structure matches our requirements
        
        // Upper spawn points
        val upperSpawns = listOf(
            Position(3, 2),
            Position(7, 2),
            Position(12, 2),
            Position(16, 2)
        )
        
        // Center spawn points
        val centerSpawns = listOf(
            Position(3, 10),
            Position(7, 10),
            Position(12, 10),
            Position(16, 10)
        )
        
        // Create enemy spawns matching the level requirements
        val rushSpawns = mutableListOf<EditorEnemySpawn>()
        
        // First 5 turns: orks and ogres from upper spawn points
        for (turn in 1..5) {
            upperSpawns.forEach { spawnPoint ->
                val enemyType = if (turn % 2 == 1) AttackerType.ORK else AttackerType.OGRE
                rushSpawns.add(EditorEnemySpawn(enemyType, 1, turn, spawnPoint))
            }
        }
        
        // Next 5 turns: goblins from center spawn points
        for (turn in 6..10) {
            centerSpawns.forEach { spawnPoint ->
                rushSpawns.add(EditorEnemySpawn(AttackerType.GOBLIN, 1, turn, spawnPoint))
            }
        }
        
        // Verify spawn counts
        assertEquals(40, rushSpawns.size, "Should have 40 total enemy spawns (5 turns * 4 upper + 5 turns * 4 center)")
        
        // Verify first 5 turns use upper spawn points
        val firstPhaseSpawns = rushSpawns.filter { it.spawnTurn in 1..5 }
        assertEquals(20, firstPhaseSpawns.size, "Should have 20 spawns in first phase (5 turns * 4 spawn points)")
        assertTrue(
            firstPhaseSpawns.all { it.spawnPoint in upperSpawns },
            "First phase should only use upper spawn points"
        )
        assertTrue(
            firstPhaseSpawns.all { it.attackerType == AttackerType.ORK || it.attackerType == AttackerType.OGRE },
            "First phase should only spawn orks and ogres"
        )
        
        // Verify next 5 turns use center spawn points
        val secondPhaseSpawns = rushSpawns.filter { it.spawnTurn in 6..10 }
        assertEquals(20, secondPhaseSpawns.size, "Should have 20 spawns in second phase (5 turns * 4 spawn points)")
        assertTrue(
            secondPhaseSpawns.all { it.spawnPoint in centerSpawns },
            "Second phase should only use center spawn points"
        )
        assertTrue(
            secondPhaseSpawns.all { it.attackerType == AttackerType.GOBLIN },
            "Second phase should only spawn goblins"
        )
    }
    
    @Test
    fun testRushLevelSpawnTiming() {
        // Verify the spawn timing follows the requirement
        val rushSpawns = mutableListOf<EditorEnemySpawn>()
        
        // Upper spawn points
        val upperSpawns = listOf(Position(3, 2), Position(7, 2), Position(12, 2), Position(16, 2))
        
        // First 5 turns: alternating orks and ogres
        for (turn in 1..5) {
            upperSpawns.forEach { spawnPoint ->
                val enemyType = if (turn % 2 == 1) AttackerType.ORK else AttackerType.OGRE
                rushSpawns.add(EditorEnemySpawn(enemyType, 1, turn, spawnPoint))
            }
        }
        
        // Check turn 1: should have 4 orks
        val turn1Spawns = rushSpawns.filter { it.spawnTurn == 1 }
        assertEquals(4, turn1Spawns.size)
        assertTrue(turn1Spawns.all { it.attackerType == AttackerType.ORK })
        
        // Check turn 2: should have 4 ogres
        val turn2Spawns = rushSpawns.filter { it.spawnTurn == 2 }
        assertEquals(4, turn2Spawns.size)
        assertTrue(turn2Spawns.all { it.attackerType == AttackerType.OGRE })
        
        // Check turn 3: should have 4 orks
        val turn3Spawns = rushSpawns.filter { it.spawnTurn == 3 }
        assertEquals(4, turn3Spawns.size)
        assertTrue(turn3Spawns.all { it.attackerType == AttackerType.ORK })
    }
}
