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
        
        val targets = listOf(Position(20, 20))
        assertTrue(level.validateWaypoints(targets), "Level with no waypoints should be valid")
    }
    
    @Test
    fun testSingleWaypointToTarget() {
        val targets = listOf(Position(20, 20))
        val waypoint = EditorWaypoint(
            position = Position(10, 10),
            nextTargetPosition = targets[0]
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
        
        assertTrue(level.validateWaypoints(targets), "Waypoint directly to target should be valid")
    }
    
    @Test
    fun testChainedWaypointsToTarget() {
        val targets = listOf(Position(20, 20))
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
            nextTargetPosition = targets[0]
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
        
        assertTrue(level.validateWaypoints(targets), "Chained waypoints to target should be valid")
    }
    
    @Test
    fun testWaypointLoopIsInvalid() {
        val targets = listOf(Position(20, 20))
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
        
        assertFalse(level.validateWaypoints(targets), "Waypoint loop should be invalid")
    }
    
    @Test
    fun testWaypointToIntermediatePositionIsValid() {
        val targets = listOf(Position(20, 20))
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
            level.validateWaypoints(targets), 
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
            level.validateWaypoints(listOf(danceCenter)),
            "Dance level waypoints should form valid chains to target (outer -> middle -> inner -> center)"
        )
    }
    
    @Test
    fun testDetailedValidationNoWaypoints() {
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
        
        val targets = listOf(Position(20, 20))
        val spawnPoints = listOf(Position(5, 5))
        val result = level.validateWaypointsDetailed(targets, spawnPoints)
        
        assertTrue(result.isValid, "No waypoints should be valid")
        assertTrue(result.circularDependencies.isEmpty(), "No circular dependencies expected")
        assertTrue(result.unconnectedWaypoints.isEmpty(), "No unconnected waypoints expected")
    }
    
    @Test
    fun testDetailedValidationCircularDependency() {
        val targets = listOf(Position(20, 20))
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
        
        val spawnPoints = listOf(Position(0, 0))
        val result = level.validateWaypointsDetailed(targets, spawnPoints)
        
        assertFalse(result.isValid, "Circular dependency should make validation fail")
        assertTrue(result.circularDependencies.contains(Position(5, 5)), "Position (5,5) should be in circular deps")
        assertTrue(result.circularDependencies.contains(Position(10, 10)), "Position (10,10) should be in circular deps")
    }
    
    @Test
    fun testDetailedValidationUnconnectedWaypoint() {
        val targets = listOf(Position(20, 20))
        // Waypoint that has no incoming connection and is not a spawn point
        val waypoint1 = EditorWaypoint(
            position = Position(5, 5),
            nextTargetPosition = targets[0]
        )
        // Waypoint that points to a non-waypoint, non-target position
        val waypoint2 = EditorWaypoint(
            position = Position(10, 10),
            nextTargetPosition = Position(15, 15)  // Unconnected position
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
        
        val spawnPoints = listOf(Position(0, 0))  // waypoint1 is not a spawn point
        val result = level.validateWaypointsDetailed(targets, spawnPoints)
        
        assertFalse(result.isValid, "Unconnected waypoints should make validation fail")
        assertTrue(result.unconnectedWaypoints.contains(Position(5, 5)), "Position (5,5) should be unconnected")
        assertTrue(result.unconnectedWaypoints.contains(Position(15, 15)), "Position (15,15) should be unconnected")
    }
    
    @Test
    fun testDetailedValidationValidChain() {
        val targets = listOf(Position(20, 20))
        val spawn = Position(0, 0)
        val waypoint1 = EditorWaypoint(
            position = spawn,
            nextTargetPosition = Position(10, 10)
        )
        val waypoint2 = EditorWaypoint(
            position = Position(10, 10),
            nextTargetPosition = targets[0]
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
        
        val spawnPoints = listOf(spawn)
        val result = level.validateWaypointsDetailed(targets, spawnPoints)
        
        assertTrue(result.isValid, "Valid chain should pass validation")
        assertTrue(result.circularDependencies.isEmpty(), "No circular dependencies expected")
        assertTrue(result.unconnectedWaypoints.isEmpty(), "No unconnected waypoints expected")
        assertTrue(result.waypointChains.isNotEmpty(), "Should have at least one chain")
        
        val chain = result.waypointChains.first()
        assertTrue(chain.startPosition == spawn, "Chain should start from spawn point")
        assertTrue(chain.endPosition == targets[0], "Chain should end at target")
        assertFalse(chain.hasCircularDependency, "Chain should not have circular dependency")
    }
    
    @Test
    fun testMultipleTargetsSupport() {
        // Test that waypoints can lead to different targets
        val target1 = Position(20, 20)
        val target2 = Position(30, 30)
        val targets = listOf(target1, target2)
        
        val spawn1 = Position(0, 0)
        val spawn2 = Position(1, 1)
        
        val waypoint1 = EditorWaypoint(
            position = spawn1,
            nextTargetPosition = target1  // Points to first target
        )
        val waypoint2 = EditorWaypoint(
            position = spawn2,
            nextTargetPosition = target2  // Points to second target
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
        
        assertTrue(level.validateWaypoints(targets), "Waypoints pointing to different targets should be valid")
        
        val spawnPoints = listOf(spawn1, spawn2)
        val result = level.validateWaypointsDetailed(targets, spawnPoints)
        
        // Both waypoints should be valid even though they point to different targets
        assertTrue(result.isValid, "Multiple targets should be supported")
        assertTrue(result.circularDependencies.isEmpty(), "No circular dependencies expected")
        assertTrue(result.unconnectedWaypoints.isEmpty(), "No unconnected waypoints expected")
    }
}
