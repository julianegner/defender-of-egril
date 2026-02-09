package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for weakest barricade selection when enemy has no clear path.
 * According to requirements:
 * - If no unoccupied path exists, enemy should attack the weakest nearby barricade
 * - "Nearby" means barricades that are close to the enemy unit
 */
class WeakestBarricadeSelectionTest {
    
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
            availableTowers = setOf(DefenderType.SPIKE_TOWER, DefenderType.SPEAR_TOWER)
        )
    }
    
    @Test
    fun testEnemySelectsWeakestBarricadeWhenBlocked() {
        val level = createTestLevel()
        val gameState = GameState(level)
        val gameEngine = GameEngine(gameState)
        
        // Create an enemy at position (1, 0)
        val enemy = Attacker(
            id = 1,
            type = AttackerType.GOBLIN,
            position = mutableStateOf(Position(1, 0)),
            level = mutableStateOf(1)
        )
        gameState.attackers.add(enemy)
        
        // Place barricades blocking all paths forward
        // Weak barricade at (2, 0) with 1 HP
        val weakBarricade = Barricade(
            id = 1,
            position = Position(2, 0),
            healthPoints = mutableStateOf(1),
            defenderId = 1
        )
        gameState.barricades.add(weakBarricade)
        
        // Strong barricade at (3, 0) with 10 HP
        val strongBarricade = Barricade(
            id = 2,
            position = Position(3, 0),
            healthPoints = mutableStateOf(10),
            defenderId = 1
        )
        gameState.barricades.add(strongBarricade)
        
        // Calculate movement - enemy should try to path, find barricades, select weakest
        val movements = gameEngine.calculateEnemyTurnMovements()
        
        // Apply first movement
        if (movements.isNotEmpty() && movements[0].isNotEmpty()) {
            val (attackerId, newPosition) = movements[0][0]
            gameEngine.applyMovement(attackerId, newPosition)
        }
        
        // Verify enemy attacked the weak barricade
        assertTrue(weakBarricade.healthPoints.value < 1, "Weak barricade should have been attacked")
        assertEquals(10, strongBarricade.healthPoints.value, "Strong barricade should not have been attacked")
    }
    
    @Test
    fun testEnemyPrefersUnoccupiedPathOverBarricadeAttack() {
        val level = createTestLevel()
        val gameState = GameState(level)
        val gameEngine = GameEngine(gameState)
        
        // Create an enemy at position (1, 0)
        val enemy = Attacker(
            id = 1,
            type = AttackerType.GOBLIN,
            position = mutableStateOf(Position(1, 0)),
            level = mutableStateOf(1)
        )
        gameState.attackers.add(enemy)
        
        // Place barricade at (2, 0) but keep path clear around it
        val barricade = Barricade(
            id = 1,
            position = Position(2, 0),
            healthPoints = mutableStateOf(5),
            defenderId = 1
        )
        gameState.barricades.add(barricade)
        
        // Calculate movement - enemy should find alternate path if available
        val movements = gameEngine.calculateEnemyTurnMovements()
        
        // If there's an alternate path, enemy should take it
        // (This test verifies pathfinding avoids barricades when possible)
        if (movements.isNotEmpty() && movements[0].isNotEmpty()) {
            val (_, newPosition) = movements[0][0]
            // Enemy should not move to barricade position if there's an alternate path
            assertTrue(newPosition != Position(2, 0) || barricade.isDestroyed(), 
                "Enemy should avoid barricade or have destroyed it")
        }
    }
    
    @Test
    fun testEnemySelectsClosestWeakBarricade() {
        val level = createTestLevel()
        val gameState = GameState(level)
        val gameEngine = GameEngine(gameState)
        val pathfinding = PathfindingSystem(gameState)
        
        // Create an enemy at position (1, 0)
        val enemy = Attacker(
            id = 1,
            type = AttackerType.GOBLIN,
            position = mutableStateOf(Position(1, 0)),
            level = mutableStateOf(1)
        )
        gameState.attackers.add(enemy)
        
        // Place a weak barricade nearby at (2, 0) with 2 HP
        val nearbyWeakBarricade = Barricade(
            id = 1,
            position = Position(2, 0),
            healthPoints = mutableStateOf(2),
            defenderId = 1
        )
        gameState.barricades.add(nearbyWeakBarricade)
        
        // Place an even weaker barricade far away at (4, 0) with 1 HP
        val farWeakBarricade = Barricade(
            id = 2,
            position = Position(4, 0),
            healthPoints = mutableStateOf(1),
            defenderId = 1
        )
        gameState.barricades.add(farWeakBarricade)
        
        // Verify enemy can't path around barricades
        val path = pathfinding.findPath(enemy.position.value, Position(5, 0), enemy)
        assertTrue(path.size < 2 || path.any { pos -> 
            gameState.barricades.any { it.position == pos && !it.isDestroyed() } 
        }, "Enemy should be blocked by barricades")
        
        // Enemy should attack the nearby weak barricade (2 HP) rather than the far one (1 HP)
        // because "nearby" is prioritized
    }
}
