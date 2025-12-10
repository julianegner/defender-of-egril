package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.editor.EditorStorage
import de.egril.defender.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for "the_cross" level to diagnose pathfinding issues
 */
class TheCrossLevelTest {
    
    @Test
    fun testTheCrossLevelLoads() {
        // Load the level from editor storage
        val editorLevel = EditorStorage.getLevel("the_cross")
        assertNotNull(editorLevel, "the_cross level should exist in editor storage")
        
        // Convert to game level
        val gameLevel = EditorStorage.convertToGameLevel(editorLevel, 999)
        assertNotNull(gameLevel, "the_cross level should convert to game level")
        
        println("=== THE CROSS LEVEL DEBUG ===")
        println("Level name: ${gameLevel.name}")
        println("Grid size: ${gameLevel.gridWidth}x${gameLevel.gridHeight}")
        println("Start positions: ${gameLevel.startPositions}")
        println("Target positions: ${gameLevel.targetPositions}")
        println("Waypoints: ${gameLevel.waypoints}")
        println("Path cells count: ${gameLevel.pathCells.size}")
        
        // Verify basic structure
        assertEquals(4, gameLevel.startPositions.size, "Should have 4 spawn points")
        assertEquals(4, gameLevel.targetPositions.size, "Should have 4 targets")
        assertEquals(4, gameLevel.waypoints.size, "Should have 4 waypoints")
        
        // Verify waypoints are at spawn points
        val waypointPositions = gameLevel.waypoints.map { it.position }.toSet()
        val spawnPositions = gameLevel.startPositions.toSet()
        assertEquals(spawnPositions, waypointPositions, "Waypoints should be at spawn points")
        
        // Verify waypoints point to targets
        val waypointTargets = gameLevel.waypoints.map { it.nextTarget }.toSet()
        val targets = gameLevel.targetPositions.toSet()
        assertEquals(targets, waypointTargets, "Waypoint targets should match level targets")
        
        println("=== END DEBUG ===")
    }
    
    @Test
    fun testPathfindingFromSpawnToTarget() {
        // Load and convert level
        val editorLevel = EditorStorage.getLevel("the_cross")
        assertNotNull(editorLevel)
        val gameLevel = EditorStorage.convertToGameLevel(editorLevel, 999)
        assertNotNull(gameLevel)
        
        val state = GameState(gameLevel)
        val pathfinding = PathfindingSystem(state)
        
        println("=== PATHFINDING TEST ===")
        
        // Test pathfinding from each spawn point to its corresponding target
        // Build pairs from the level's waypoints (spawn points with their targets)
        val spawnToTargetPairs = gameLevel.waypoints.map { waypoint ->
            waypoint.position to waypoint.nextTarget
        }
        
        for ((spawn, target) in spawnToTargetPairs) {
            println("Testing path from $spawn to $target")
            val path = pathfinding.findPath(spawn, target)
            println("  Path length: ${path.size}")
            println("  First 5 positions: ${path.take(5)}")
            println("  Last 5 positions: ${path.takeLast(5)}")
            
            assertTrue(path.isNotEmpty(), "Path from $spawn to $target should not be empty")
            assertEquals(spawn, path.first(), "Path should start at spawn point")
            assertEquals(target, path.last(), "Path should end at target")
            
            // Verify path is continuous (each position is adjacent to next)
            for (i in 0 until path.size - 1) {
                val current = path[i]
                val next = path[i + 1]
                val neighbors = current.getHexNeighbors()
                assertTrue(
                    neighbors.contains(next),
                    "Position $next should be a hex neighbor of $current"
                )
            }
        }
        
        println("=== END PATHFINDING TEST ===")
    }
    
    @Test
    fun testEnemyMovementWithWaypoints() {
        // Load and convert level
        val editorLevel = EditorStorage.getLevel("the_cross")
        assertNotNull(editorLevel)
        val gameLevel = EditorStorage.convertToGameLevel(editorLevel, 999)
        assertNotNull(gameLevel)
        
        val state = GameState(gameLevel)
        val engine = GameEngine(state)
        val movementSystem = EnemyMovementSystem(state, PathfindingSystem(state))
        
        println("=== ENEMY MOVEMENT TEST ===")
        
        // Test initial target assignment for each spawn point
        for (spawnPoint in gameLevel.startPositions) {
            println("Testing spawn point: $spawnPoint")
            val initialTarget = movementSystem.getInitialTarget(spawnPoint)
            println("  Initial target: $initialTarget")
            
            // The initial target should be a valid target position
            assertTrue(
                gameLevel.targetPositions.contains(initialTarget),
                "Initial target $initialTarget should be one of the level targets"
            )
            
            // Verify the waypoint exists at spawn point
            val waypoint = gameLevel.getWaypointAt(spawnPoint)
            assertNotNull(waypoint, "Waypoint should exist at spawn point $spawnPoint")
            assertEquals(
                initialTarget,
                waypoint.nextTarget,
                "Initial target should match waypoint's next target"
            )
        }
        
        println("=== END ENEMY MOVEMENT TEST ===")
    }
    
    @Test
    fun testSpawnPointsAreWalkable() {
        // Load and convert level
        val editorLevel = EditorStorage.getLevel("the_cross")
        assertNotNull(editorLevel)
        val gameLevel = EditorStorage.convertToGameLevel(editorLevel, 999)
        assertNotNull(gameLevel)
        
        println("=== SPAWN POINT WALKABILITY TEST ===")
        
        // Verify spawn points are in pathCells (because they're waypoints)
        for (spawnPoint in gameLevel.startPositions) {
            val isOnPath = gameLevel.isOnPath(spawnPoint)
            println("Spawn point $spawnPoint - isOnPath: $isOnPath")
            assertTrue(
                isOnPath,
                "Spawn point $spawnPoint should be walkable (in pathCells)"
            )
        }
        
        // Verify targets are recognized as target positions
        for (target in gameLevel.targetPositions) {
            val isTarget = gameLevel.isTargetPosition(target)
            println("Target $target - isTargetPosition: $isTarget")
            assertTrue(isTarget, "Target $target should be recognized as target position")
        }
        
        println("=== END SPAWN POINT WALKABILITY TEST ===")
    }
}
