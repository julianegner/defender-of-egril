package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.*
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Test that enemies can reach and enter the target position
 */
class TargetEntryTest {
    
    @Test
    fun testEnemyCanReachTarget() {
        // Create a simple straight path to target
        val pathCells = setOf(
            Position(0, 2), Position(1, 2), Position(2, 2),
            Position(3, 2), Position(4, 2), Position(5, 2),
            Position(6, 2), Position(7, 2), Position(8, 2)
            // Note: target at (9, 2) is NOT in pathCells - this is the bug!
        )
        
        val level = Level(
            id = 1,
            name = "Target Entry Test",
            gridWidth = 10,
            gridHeight = 6,
            startPositions = listOf(Position(0, 2)),
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
        
        // Spawn goblin at start
        val goblin = Attacker(
            id = 1,
            type = AttackerType.GOBLIN,
            position = mutableStateOf(Position(0, 2)),
            level = mutableStateOf(1)
        )
        state.attackers.add(goblin)
        
        // Move goblin until it reaches target or max turns
        var turnCount = 0
        val maxTurns = 20
        
        while (goblin.position.value != level.targetPositions.first() && 
               !goblin.isDefeated.value && 
               turnCount < maxTurns) {
            
            val movements = engine.calculateEnemyTurnMovements()
            
            for (movementStep in movements.allMovementSteps) {
                for ((attackerId, newPosition) in movementStep) {
                    engine.applyMovement(attackerId, newPosition)
                }
            }
            
            turnCount++
        }
        
        // Verify goblin reached the target (which should mark it as defeated)
        assertTrue(
            goblin.isDefeated.value,
            "Goblin should reach target and be defeated. Position: ${goblin.position.value}, Target: ${level.targetPositions.first()}"
        )
        
        // Verify health was reduced
        assertTrue(
            state.healthPoints.value < level.healthPoints,
            "Health should be reduced when enemy reaches target"
        )
    }
}
