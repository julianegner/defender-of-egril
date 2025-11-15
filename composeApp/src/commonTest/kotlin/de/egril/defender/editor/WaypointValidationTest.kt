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
        // Test the actual Dance level waypoint configuration with 3 circles
        val danceCenter = Position(20, 20)
        // Outermost ring waypoints at distance ~18
        val outerWaypoints = listOf(
            Position(38, 20),  // East
            Position(20, 2),   // North
            Position(2, 20),   // West
            Position(20, 38)   // South
        )
        // Middle ring waypoints at distance ~10
        val middleWaypoints = listOf(
            Position(30, 20),  // East
            Position(20, 10),  // North
            Position(10, 20),  // West
            Position(20, 30)   // South
        )
        // Inner ring waypoints at distance ~6
        val innerWaypoints = listOf(
            Position(26, 20),  // East
            Position(20, 14),  // North
            Position(14, 20),  // West
            Position(20, 26)   // South
        )
        
        val danceWaypoints = listOf(
            // Outer ring - clockwise
            EditorWaypoint(outerWaypoints[0], outerWaypoints[1]),
            EditorWaypoint(outerWaypoints[1], outerWaypoints[2]),
            EditorWaypoint(outerWaypoints[2], outerWaypoints[3]),
            EditorWaypoint(outerWaypoints[3], middleWaypoints[0]),
            // Middle ring - clockwise
            EditorWaypoint(middleWaypoints[0], middleWaypoints[1]),
            EditorWaypoint(middleWaypoints[1], middleWaypoints[2]),
            EditorWaypoint(middleWaypoints[2], middleWaypoints[3]),
            EditorWaypoint(middleWaypoints[3], innerWaypoints[0]),
            // Inner ring - clockwise
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
            "Dance level waypoints should form valid chains to target (outer -> middle -> inner -> center)"
        )
    }
}
