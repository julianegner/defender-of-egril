package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.*
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Test specific dead-end scenario from the issue
 */
class SpecificDeadEndTest {
    
    /**
     * Reproduce the exact scenario from the issue:
     * - Enemy spawns in a dead end branch at the top
     * - There's a main path to the goal
     * - Enemy should navigate out of the dead end to reach the goal
     * 
     * Map layout (X = path, . = non-path/blocked):
     *   0 1 2 3 4 5 6 7 8 9
     * 0 X X X X . . . . . .  <- Dead end branch (only connects at position 3,0)
     * 1 . . . X . . . . . .
     * 2 . . . X X X X X X T  <- Main path to target
     * 
     * Expected: Enemy exits the dead end and progresses toward the target without oscillating.
     */
    @Test
    fun testIssueScenarioDeadEnd() {
        // Create a map similar to the issue description
        val pathCells = setOf(
            // Dead end at top (row 0)
            Position(0, 0), Position(1, 0), Position(2, 0), Position(3, 0),
            // Connector down
            Position(3, 1),
            // Main path to target (row 2)
            Position(3, 2), Position(4, 2), Position(5, 2), 
            Position(6, 2), Position(7, 2), Position(8, 2), Position(9, 2)
        )
        
        val level = Level(
            id = 1,
            name = "Issue Scenario",
            gridWidth = 10,
            gridHeight = 6,
            startPositions = listOf(Position(0, 0)),  // Spawn in dead end
            targetPositions = listOf(Position(9, 2)),
            pathCells = pathCells,
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
        
        // Spawn goblin at the dead end start
        val goblin = Attacker(
            id = 1,
            type = AttackerType.GOBLIN,
            position = mutableStateOf(Position(0, 0)),
            level = mutableStateOf(1)
        )
        state.attackers.add(goblin)
        
        // Track movement over several turns
        val path = mutableListOf(goblin.position.value)
        val maxTurns = 15
        
        repeat(maxTurns) { turnNum ->
            val movements = engine.calculateEnemyTurnMovements()
            
            for (movementStep in movements.allMovementSteps) {
                for ((attackerId, newPosition) in movementStep) {
                    engine.applyMovement(attackerId, newPosition)
                }
            }
            
            val newPos = goblin.position.value
            path.add(newPos)
            
            // Check no backtracking (distance should not increase)
            if (path.size >= 2) {
                val prevDist = path[path.size - 2].distanceTo(level.targetPositions.first())
                val currDist = newPos.distanceTo(level.targetPositions.first())
                
                assertTrue(
                    currDist <= prevDist,
                    "Turn $turnNum: Goblin should not move away from target. Moved from ${path[path.size - 2]} (dist=$prevDist) to $newPos (dist=$currDist)"
                )
            }
            
            // If reached target, stop
            if (newPos == level.targetPositions.first()) {
                return@repeat
            }
        }
        
        // Verify goblin made progress toward the goal
        val startDist = path.first().distanceTo(level.targetPositions.first())
        val endDist = path.last().distanceTo(level.targetPositions.first())
        
        assertTrue(
            endDist < startDist,
            "Goblin should have moved closer to target. Path: $path"
        )
        
        // Verify goblin eventually leaves row 0 (the dead end)
        val leftDeadEnd = path.any { it.y > 0 }
        assertTrue(
            leftDeadEnd,
            "Goblin should have left the dead end (row 0). Path: $path"
        )
    }
    
    /**
     * Test dragon movement in the dead-end scenario
     */
    @Test
    fun testDragonInDeadEnd() {
        val pathCells = setOf(
            // Dead end at top
            Position(0, 0), Position(1, 0), Position(2, 0),
            // Connector
            Position(2, 1),
            // Main path
            Position(2, 2), Position(3, 2), Position(4, 2), 
            Position(5, 2), Position(6, 2), Position(7, 2), Position(8, 2), Position(9, 2)
        )
        
        val level = Level(
            id = 1,
            name = "Dragon Dead End Test",
            gridWidth = 10,
            gridHeight = 6,
            startPositions = listOf(Position(0, 0)),
            targetPositions = listOf(Position(9, 2)),
            pathCells = pathCells,
            attackerWaves = listOf(
                AttackerWave(
                    attackers = listOf(AttackerType.DRAGON),
                    spawnDelay = 0
                )
            ),
            initialCoins = 100,
            healthPoints = 10
        )
        
        val state = GameState(level)
        val engine = GameEngine(state)
        
        // Spawn dragon at dead end
        val dragon = Attacker(
            id = 1,
            type = AttackerType.DRAGON,
            position = mutableStateOf(Position(1, 0)),  // In the dead end
            level = mutableStateOf(1)
        )
        state.attackers.add(dragon)
        
        // Simulate first turn (walking)
        val movements1 = engine.calculateEnemyTurnMovements()
        for (movementStep in movements1.allMovementSteps) {
            for ((attackerId, newPosition) in movementStep) {
                engine.applyMovement(attackerId, newPosition)
            }
        }
        
        val posAfterTurn1 = dragon.position.value
        
        // Dragon should have moved (speed 1 on first turn when walking)
        assertTrue(
            posAfterTurn1 != Position(1, 0),
            "Dragon should move on first turn. Position: $posAfterTurn1"
        )
        
        // Should move toward target (closer)
        val startDist = Position(1, 0).distanceTo(level.targetPositions.first())
        val endDist = posAfterTurn1.distanceTo(level.targetPositions.first())
        
        assertTrue(
            endDist <= startDist,
            "Dragon should move toward target. Start dist: $startDist, end dist: $endDist"
        )
    }
}
