package de.egril.defender.game

import de.egril.defender.model.*
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Integration test to verify that enemies follow waypoints correctly
 */
class WaypointFollowingTest {
    
    @Test
    fun testEnemiesFollowWaypointsWithMultipleTargets() {
        // Create a simplified test level with waypoints and multiple targets
        // Path: Spawn at (10,4) -> waypoint (7,4) -> waypoint (3,4) -> target (0,4)
        // Also target at (10,0) to test multiple targets
        
        val pathCells = mutableSetOf<Position>()
        // Create a simple horizontal path from x=0 to x=10 at y=4
        for (x in 0..10) {
            pathCells.add(Position(x, 4))
        }
        
        val testLevel = Level(
            id = 9999,
            name = "Waypoint Test Level",
            gridWidth = 15,
            gridHeight = 10,
            startPositions = listOf(
                Position(10, 4)  // Spawn point
            ),
            targetPositions = listOf(
                Position(0, 4),   // Target 1 (left) - main target via waypoints
                Position(10, 0)   // Target 2 (top) - alternate target
            ),
            pathCells = pathCells,
            buildAreas = emptySet(),
            attackerWaves = emptyList(),
            initialCoins = 100,
            healthPoints = 10,
            directSpawnPlan = listOf(
                PlannedEnemySpawn(AttackerType.GOBLIN, spawnTurn = 1, level = 1),  // Turn 1: 1 goblin
                PlannedEnemySpawn(AttackerType.GOBLIN, spawnTurn = 2, level = 1),  // Turn 2: 1 goblin
                PlannedEnemySpawn(AttackerType.GOBLIN, spawnTurn = 2, level = 1)   // Turn 2: another goblin
            ),
            availableTowers = setOf(DefenderType.SPIKE_TOWER),
            waypoints = listOf(
                Waypoint(
                    position = Position(10, 4),  // From spawn
                    nextTarget = Position(7, 4)   // To first intermediate waypoint
                ),
                Waypoint(
                    position = Position(7, 4),    // From first intermediate
                    nextTarget = Position(3, 4)   // To second intermediate waypoint
                ),
                Waypoint(
                    position = Position(3, 4),    // From second intermediate
                    nextTarget = Position(0, 4)   // To final target
                )
            )
        )
        
        println("=== WAYPOINT FOLLOWING TEST ===")
        println("Testing level: ${testLevel.name}")
        println("Initial health points: ${testLevel.healthPoints}")
        println("Target positions: ${testLevel.targetPositions}")
        println("Waypoints count: ${testLevel.waypoints.size}")
        
        // Create game state and engine
        val state = GameState(testLevel)
        val engine = GameEngine(state)
        
        // Start the game
        engine.startFirstPlayerTurn()
        
        val initialHealthPoints = state.healthPoints.value
        println("Initial HP: $initialHealthPoints")
        
        // Run 3 turns without placing any towers
        // This tests if enemies are following waypoints correctly
        // With waypoints: (10,4) -> (7,4) -> (3,4) -> (0,4), and goblin speed=2,
        // Total distance: ~10 tiles. Goblins move 2 tiles/turn = 6 tiles in 3 turns
        // So they should not reach the target yet
        for (turn in 1..3) {
            println("\n--- Turn $turn ---")
            
            // Start enemy turn
            engine.startEnemyTurn()
            
            // Spawn enemies for this turn
            engine.spawnEnemyTurnAttackers()
            
            // Complete the enemy turn
            engine.completeEnemyTurn()
            
            // Start next player turn
            engine.startFirstPlayerTurn()
            
            println("Turn $turn completed. HP: ${state.healthPoints.value}, Attackers alive: ${state.attackers.count { !it.isDefeated.value }}")
        }
        
        val finalHealthPoints = state.healthPoints.value
        println("\nFinal HP: $finalHealthPoints")
        
        // After 3 turns, health points should remain unchanged
        // because enemies should still be following waypoints and haven't reached targets yet
        assertEquals(
            initialHealthPoints, 
            finalHealthPoints,
            "Health points should remain unchanged after 3 turns as enemies follow waypoints and haven't reached targets yet"
        )
        
        println("=== TEST PASSED ===")
    }
}
