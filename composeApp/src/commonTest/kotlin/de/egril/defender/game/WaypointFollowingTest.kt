package de.egril.defender.game

import de.egril.defender.model.GameState
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Integration test to verify that enemies follow waypoints correctly in actual levels
 */
class WaypointFollowingTest {
    
    @Test
    fun testTheWoodsFirstIncursionEnemiesFollowWaypoints() {
        // Load the levels (EditorStorage will be initialized automatically)
        val levels = LevelData.createLevels()
        
        println("=== Available levels: ===")
        levels.forEach { println("- ${it.name}") }
        
        val theWoodsLevel = levels.firstOrNull { it.name == "The Woods - First Incursion" }
        
        if (theWoodsLevel == null) {
            println("ERROR: Level 'The Woods - First Incursion' not found among ${levels.size} loaded levels")
            // Try case-insensitive match
            val caseInsensitiveMatch = levels.firstOrNull { 
                it.name.equals("The Woods - First Incursion", ignoreCase = true) 
            }
            if (caseInsensitiveMatch != null) {
                println("Found case-insensitive match: '${caseInsensitiveMatch.name}'")
            }
            throw AssertionError("Level 'The Woods - First Incursion' not found")
        }
        
        println("=== WAYPOINT FOLLOWING TEST ===")
        println("Testing level: ${theWoodsLevel.name}")
        println("Initial health points: ${theWoodsLevel.healthPoints}")
        println("Target positions: ${theWoodsLevel.targetPositions}")
        println("Waypoints count: ${theWoodsLevel.waypoints.size}")
        
        // Create game state and engine
        val state = GameState(theWoodsLevel)
        val engine = GameEngine(state)
        
        // Start the game
        engine.startFirstPlayerTurn()
        
        val initialHealthPoints = state.healthPoints.value
        println("Initial HP: $initialHealthPoints")
        
        // Run 3 turns without placing any towers
        // This tests if enemies are following waypoints correctly
        // If they reach targets too early, health will decrease
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
        // because enemies should still be following waypoints to their targets
        assertEquals(
            initialHealthPoints, 
            finalHealthPoints,
            "Health points should remain unchanged after 3 turns as enemies follow waypoints and haven't reached targets yet"
        )
        
        println("=== TEST PASSED ===")
    }
}
