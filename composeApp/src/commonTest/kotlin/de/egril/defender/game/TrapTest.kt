package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for trap mechanics
 */
class TrapTest {
    
    // Helper function to create a minimal test level without loading files
    private fun createTestLevel(): Level {
        // Create a simple level with minimal required data
        val pathCells = setOf(Position(0, 0), Position(1, 0), Position(2, 0), Position(3, 0), Position(4, 0), Position(5, 5))
        val buildIslands = setOf(Position(2, 2), Position(2, 3), Position(3, 2), Position(3, 3))
        
        return Level(
            id = 1,
            name = "Test Level",
            subtitle = "Test",
            gridWidth = 10,
            gridHeight = 10,
            startPositions = listOf(Position(0, 0)),
            targetPositions = listOf(Position(5, 5)),
            pathCells = pathCells,
            buildIslands = buildIslands,
            attackerWaves = emptyList(),
            initialCoins = 100,
            healthPoints = 10,
            availableTowers = setOf(DefenderType.SPIKE_TOWER)
        )
    }
    
    @Test
    fun testTrapActivation() {
        // Create a simple game state with a trap
        val level = createTestLevel()
        val gameState = GameState(level)
        val gameEngine = GameEngine(gameState)
        
        // Add a trap at position (2, 3)
        val trapPosition = Position(2, 3)
        val trap = Trap(position = trapPosition, damage = 10, defenderId = 1)
        gameState.traps.add(trap)
        
        // Create an enemy at the trap position
        val enemy = Attacker(
            id = 1,
            type = AttackerType.GOBLIN,
            position = mutableStateOf(trapPosition),
            level = mutableStateOf(1)
        )
        gameState.attackers.add(enemy)
        
        val initialHealth = enemy.currentHealth.value
        
        // Activate traps
        gameEngine.checkAndActivateTraps()
        
        // Verify enemy took damage
        assertEquals(initialHealth - 10, enemy.currentHealth.value, "Enemy should have taken 10 damage from trap")
        
        // Verify trap was removed
        assertTrue(gameState.traps.isEmpty(), "Trap should be removed after activation")
    }
    
    @Test
    fun testTrapKillsEnemy() {
        // Create a simple game state with a trap
        val level = createTestLevel()
        val gameState = GameState(level)
        val gameEngine = GameEngine(gameState)
        
        // Add a trap with high damage
        val trapPosition = Position(2, 3)
        val trap = Trap(position = trapPosition, damage = 100, defenderId = 1)
        gameState.traps.add(trap)
        
        // Create a weak enemy at the trap position
        val enemy = Attacker(
            id = 1,
            type = AttackerType.GOBLIN,
            position = mutableStateOf(trapPosition),
            level = mutableStateOf(1),
            currentHealth = mutableStateOf(10)
        )
        gameState.attackers.add(enemy)
        
        // Activate traps
        gameEngine.checkAndActivateTraps()
        
        // Verify enemy was defeated
        assertTrue(enemy.isDefeated.value, "Enemy should be defeated when health drops to 0 or below")
        assertTrue(gameState.traps.isEmpty(), "Trap should be removed after activation")
    }
}
