package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for trap activation during enemy movement.
 * According to requirements:
 * - Regular traps: Apply damage when enemy moves onto trap position
 * - Magical traps: Teleport enemy back to spawn when enemy moves onto trap position
 */
class TrapActivationDuringMovementTest {
    
    private fun createTestLevel(): Level {
        val pathCells = setOf(
            Position(0, 0), Position(1, 0), Position(2, 0), 
            Position(3, 0), Position(4, 0), Position(5, 0)
        )
        val buildIslands = setOf(Position(2, 2), Position(2, 3), Position(3, 2), Position(3, 3))
        
        return Level(
            id = 1,
            name = "Test Level",
            subtitle = "Test",
            gridWidth = 10,
            gridHeight = 10,
            startPositions = listOf(Position(0, 0)),
            targetPositions = listOf(Position(5, 0)),
            pathCells = pathCells,
            buildIslands = buildIslands,
            attackerWaves = emptyList(),
            initialCoins = 100,
            healthPoints = 10,
            availableTowers = setOf(DefenderType.SPIKE_TOWER)
        )
    }
    
    @Test
    fun testRegularTrapActivatesOnMovement() {
        val level = createTestLevel()
        val gameState = GameState(level)
        val gameEngine = GameEngine(gameState)
        
        // Place a dwarven trap at position (2, 0)
        val trapPosition = Position(2, 0)
        val trap = Trap(position = trapPosition, damage = 10, defenderId = 1, type = TrapType.DWARVEN)
        gameState.traps.add(trap)
        
        // Create an enemy at position (1, 0)
        val enemy = Attacker(
            id = 1,
            type = AttackerType.GOBLIN,
            position = mutableStateOf(Position(1, 0)),
            level = mutableStateOf(1),
            currentHealth = mutableStateOf(20)
        )
        gameState.attackers.add(enemy)
        
        val initialHealth = enemy.currentHealth.value
        
        // Move enemy to trap position
        gameEngine.applyMovement(enemy.id, trapPosition)
        
        // Verify enemy took damage
        assertEquals(initialHealth - 10, enemy.currentHealth.value, "Enemy should have taken 10 damage from trap during movement")
        
        // Verify enemy moved to trap position
        assertEquals(trapPosition, enemy.position.value, "Enemy should have moved to trap position")
        
        // Verify trap was removed
        assertTrue(gameState.traps.isEmpty(), "Trap should be removed after activation")
    }
    
    @Test
    fun testMagicalTrapTeleportsEnemyOnMovement() {
        val level = createTestLevel()
        val gameState = GameState(level)
        val gameEngine = GameEngine(gameState)
        
        // Place a magical trap at position (3, 0)
        val trapPosition = Position(3, 0)
        val trap = Trap(position = trapPosition, damage = 0, defenderId = 1, type = TrapType.MAGICAL)
        gameState.traps.add(trap)
        
        // Create an enemy at position (2, 0)
        val spawnPoint = Position(0, 0)
        val enemy = Attacker(
            id = 1,
            type = AttackerType.GOBLIN,
            position = mutableStateOf(Position(2, 0)),
            level = mutableStateOf(1)
        )
        gameState.attackers.add(enemy)
        
        // Move enemy to trap position
        gameEngine.applyMovement(enemy.id, trapPosition)
        
        // Verify enemy was teleported back to spawn
        assertEquals(spawnPoint, enemy.position.value, "Enemy should be teleported back to spawn point by magical trap")
        
        // Verify trap was removed
        assertTrue(gameState.traps.isEmpty(), "Magical trap should be removed after activation")
    }
    
    @Test
    fun testTrapDoesNotActivateWhenEnemyDoesNotMove() {
        val level = createTestLevel()
        val gameState = GameState(level)
        val gameEngine = GameEngine(gameState)
        
        // Place a trap at position (1, 0)
        val trapPosition = Position(1, 0)
        val trap = Trap(position = trapPosition, damage = 10, defenderId = 1, type = TrapType.DWARVEN)
        gameState.traps.add(trap)
        
        // Create an enemy at the same position (already on trap)
        val enemy = Attacker(
            id = 1,
            type = AttackerType.GOBLIN,
            position = mutableStateOf(trapPosition),
            level = mutableStateOf(1),
            currentHealth = mutableStateOf(20)
        )
        gameState.attackers.add(enemy)
        
        val initialHealth = enemy.currentHealth.value
        
        // Try to move enemy to same position (no movement)
        gameEngine.applyMovement(enemy.id, trapPosition)
        
        // Verify enemy did NOT take damage (trap only activates on movement TO the position)
        assertEquals(initialHealth, enemy.currentHealth.value, "Enemy should not take damage when already on trap position")
        
        // Trap should still be there
        assertEquals(1, gameState.traps.size, "Trap should not be removed if enemy doesn't move onto it")
    }
    
    @Test
    fun testLethalTrapDefeatsEnemy() {
        val level = createTestLevel()
        val gameState = GameState(level)
        val gameEngine = GameEngine(gameState)
        
        // Place a high-damage trap
        val trapPosition = Position(2, 0)
        val trap = Trap(position = trapPosition, damage = 100, defenderId = 1, type = TrapType.DWARVEN)
        gameState.traps.add(trap)
        
        // Create a weak enemy
        val enemy = Attacker(
            id = 1,
            type = AttackerType.GOBLIN,
            position = mutableStateOf(Position(1, 0)),
            level = mutableStateOf(1),
            currentHealth = mutableStateOf(10)
        )
        gameState.attackers.add(enemy)
        
        // Move enemy to trap position
        gameEngine.applyMovement(enemy.id, trapPosition)
        
        // Verify enemy was defeated
        assertTrue(enemy.isDefeated.value, "Enemy should be defeated when trap damage exceeds health")
        
        // Verify trap was removed
        assertTrue(gameState.traps.isEmpty(), "Trap should be removed after activation")
    }
}
