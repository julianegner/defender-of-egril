package com.defenderofegril.game

import androidx.compose.runtime.mutableStateOf
import com.defenderofegril.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for trap mechanics
 */
class TrapTest {
    
    @Test
    fun testTrapActivation() {
        // Create a simple game state with a trap
        val level = LevelData.createLevels()[0]  // Use first level
        val gameState = GameState(level)
        val gameEngine = GameEngine(gameState)
        
        // Add a trap at position (2, 3)
        val trapPosition = Position(2, 3)
        val trap = Trap(position = trapPosition, damage = 10, mineId = 1)
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
        val level = LevelData.createLevels()[0]
        val gameState = GameState(level)
        val gameEngine = GameEngine(gameState)
        
        // Add a trap with high damage
        val trapPosition = Position(2, 3)
        val trap = Trap(position = trapPosition, damage = 100, mineId = 1)
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
