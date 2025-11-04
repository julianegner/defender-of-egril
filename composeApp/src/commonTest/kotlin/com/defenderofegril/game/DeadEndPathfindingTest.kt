package com.defenderofegril.game

import androidx.compose.runtime.mutableStateOf
import com.defenderofegril.model.*
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Test pathfinding with dead ends
 */
class DeadEndPathfindingTest {
    
    /**
     * Test that enemies avoid dead ends and find the correct path to the goal.
     * 
     * Map layout (X = path, O = build island/blocked, T = target):
     *   0 1 2 3 4 5 6 7 8 9
     * 0 X X X O O O O O O O
     * 1 O O X O O O O O O O
     * 2 O O X X X X X X X T
     * 
     * Enemy should go: (0,0) -> (1,0) -> (2,0) -> (2,1) -> (2,2) -> ... -> (9,2)
     * Enemy should NOT go into the dead end at (0,0)-(1,0)-(2,0) and stay there
     */
    @Test
    fun testEnemyAvoidsDeadEnd() {
        // Create path with a dead end at top
        val pathCells = setOf(
            // Dead end path at top
            Position(0, 0), Position(1, 0), Position(2, 0),
            // Connecting path down
            Position(2, 1),
            // Main path to target
            Position(2, 2), Position(3, 2), Position(4, 2), 
            Position(5, 2), Position(6, 2), Position(7, 2), 
            Position(8, 2), Position(9, 2)
        )
        
        val level = Level(
            id = 1,
            name = "Dead End Test",
            gridWidth = 10,
            gridHeight = 6,
            startPositions = listOf(Position(0, 0)),
            targetPosition = Position(9, 2),
            pathCells = pathCells,
            buildIslands = emptySet(),
            attackerWaves = listOf(
                AttackerWave(
                    attackers = listOf(AttackerType.GOBLIN),
                    spawnDelay = 0
                )
            ),
            initialCoins = 100,
            healthPoints = 10
        )
        
        val state = GameState(level)
        val engine = GameEngine(state)
        
        // Spawn enemy at start (in the dead end)
        val enemy = Attacker(
            id = 1,
            type = AttackerType.GOBLIN,
            position = mutableStateOf(Position(0, 0)),
            level = 1
        )
        state.attackers.add(enemy)
        
        // Track positions over multiple turns to ensure forward progress
        val positions = mutableListOf(enemy.position.value)
        
        // Simulate several movement turns
        repeat(10) {
            val movements = engine.calculateEnemyTurnMovements()
            
            for (movementStep in movements) {
                for ((attackerId, newPosition) in movementStep) {
                    engine.applyMovement(attackerId, newPosition)
                }
            }
            
            positions.add(enemy.position.value)
            
            // Enemy should continuously move closer to target
            if (positions.size >= 2) {
                val prevPos = positions[positions.size - 2]
                val currPos = positions[positions.size - 1]
                val prevDist = prevPos.distanceTo(level.targetPosition)
                val currDist = currPos.distanceTo(level.targetPosition)
                
                // Distance should decrease or stay same (if blocked), but not increase (backtracking)
                assertTrue(
                    currDist <= prevDist,
                    "Enemy should not move away from target. Turn ${positions.size - 1}: moved from $prevPos (dist=$prevDist) to $currPos (dist=$currDist)"
                )
            }
        }
        
        // Final position should be significantly closer to target than start
        val startDist = positions.first().distanceTo(level.targetPosition)
        val endDist = positions.last().distanceTo(level.targetPosition)
        
        assertTrue(
            endDist < startDist,
            "Enemy should have moved closer to target. Start: ${positions.first()} (dist=$startDist), End: ${positions.last()} (dist=$endDist)"
        )
        
        // Enemy should have left the dead end area (y should be > 0)
        assertTrue(
            enemy.position.value.y > 0 || enemy.position.value.x > 2,
            "Enemy should have left the dead end. Position: ${enemy.position.value}"
        )
    }
}
