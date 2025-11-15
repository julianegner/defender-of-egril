package de.egril.defender.editor

import de.egril.defender.model.Position
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WaypointValidationTest {
    
    @Test
    fun testNoWaypointsIsValid() {
        val level = EditorLevel(
            id = "test_level",
            mapId = "test_map",
            title = "Test Level",
            startCoins = 100,
            startHealthPoints = 10,
            enemySpawns = emptyList(),
            availableTowers = emptySet(),
            waypoints = emptyList()
        )
        
        val target = Position(20, 20)
        assertTrue(level.validateWaypoints(target), "Level with no waypoints should be valid")
    }
    
    @Test
    fun testSingleWaypointToTarget() {
        val target = Position(20, 20)
        val waypoint = EditorWaypoint(
            position = Position(10, 10),
            nextTargetPosition = target
        )
        
        val level = EditorLevel(
            id = "test_level",
            mapId = "test_map",
            title = "Test Level",
            startCoins = 100,
            startHealthPoints = 10,
            enemySpawns = emptyList(),
            availableTowers = emptySet(),
            waypoints = listOf(waypoint)
        )
        
        assertTrue(level.validateWaypoints(target), "Waypoint directly to target should be valid")
    }
    
    @Test
    fun testChainedWaypointsToTarget() {
        val target = Position(20, 20)
        val waypoint1 = EditorWaypoint(
            position = Position(5, 5),
            nextTargetPosition = Position(10, 10)
        )
        val waypoint2 = EditorWaypoint(
            position = Position(10, 10),
            nextTargetPosition = Position(15, 15)
        )
        val waypoint3 = EditorWaypoint(
            position = Position(15, 15),
            nextTargetPosition = target
        )
        
        val level = EditorLevel(
            id = "test_level",
            mapId = "test_map",
            title = "Test Level",
            startCoins = 100,
            startHealthPoints = 10,
            enemySpawns = emptyList(),
            availableTowers = emptySet(),
            waypoints = listOf(waypoint1, waypoint2, waypoint3)
        )
        
        assertTrue(level.validateWaypoints(target), "Chained waypoints to target should be valid")
    }
    
    @Test
    fun testWaypointLoopIsInvalid() {
        val target = Position(20, 20)
        val waypoint1 = EditorWaypoint(
            position = Position(5, 5),
            nextTargetPosition = Position(10, 10)
        )
        val waypoint2 = EditorWaypoint(
            position = Position(10, 10),
            nextTargetPosition = Position(5, 5)  // Points back to waypoint1 - creates a loop!
        )
        
        val level = EditorLevel(
            id = "test_level",
            mapId = "test_map",
            title = "Test Level",
            startCoins = 100,
            startHealthPoints = 10,
            enemySpawns = emptyList(),
            availableTowers = emptySet(),
            waypoints = listOf(waypoint1, waypoint2)
        )
        
        assertFalse(level.validateWaypoints(target), "Waypoint loop should be invalid")
    }
    
    @Test
    fun testWaypointToIntermediatePositionIsValid() {
        val target = Position(20, 20)
        val waypoint = EditorWaypoint(
            position = Position(10, 10),
            nextTargetPosition = Position(15, 15)  // Not a waypoint, not the target - just a position
        )
        
        val level = EditorLevel(
            id = "test_level",
            mapId = "test_map",
            title = "Test Level",
            startCoins = 100,
            startHealthPoints = 10,
            enemySpawns = emptyList(),
            availableTowers = emptySet(),
            waypoints = listOf(waypoint)
        )
        
        assertTrue(
            level.validateWaypoints(target), 
            "Waypoint to intermediate position should be valid (enemies will pathfind from there)"
        )
    }
    
    @Test
    fun testDanceLevelWaypoints() {
        // Test the actual Dance level waypoint configuration
        val danceCenter = Position(20, 20)
        val outerWaypoints = listOf(
            Position(24, 20),  // East
            Position(20, 16),  // North
            Position(16, 20),  // West
            Position(20, 24)   // South
        )
        val innerWaypoints = listOf(
            Position(22, 20),  // East
            Position(20, 18),  // North
            Position(18, 20),  // West
            Position(20, 22)   // South
        )
        
        val danceWaypoints = listOf(
            // Outer ring
            EditorWaypoint(outerWaypoints[0], outerWaypoints[1]),
            EditorWaypoint(outerWaypoints[1], outerWaypoints[2]),
            EditorWaypoint(outerWaypoints[2], outerWaypoints[3]),
            EditorWaypoint(outerWaypoints[3], innerWaypoints[0]),
            // Inner ring
            EditorWaypoint(innerWaypoints[0], innerWaypoints[1]),
            EditorWaypoint(innerWaypoints[1], innerWaypoints[2]),
            EditorWaypoint(innerWaypoints[2], innerWaypoints[3]),
            EditorWaypoint(innerWaypoints[3], danceCenter)
        )
        
        val level = EditorLevel(
            id = "level_9",
            mapId = "map_dance",
            title = "The Dance",
            startCoins = 220,
            startHealthPoints = 10,
            enemySpawns = emptyList(),
            availableTowers = emptySet(),
            waypoints = danceWaypoints
        )
        
        assertTrue(
            level.validateWaypoints(danceCenter),
            "Dance level waypoints should form valid chains to target"
        )
    }
}
