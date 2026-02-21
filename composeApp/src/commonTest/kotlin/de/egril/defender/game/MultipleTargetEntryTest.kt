package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Test that multiple enemies can reach the target in the same turn
 */
class MultipleTargetEntryTest {
    
    @Test
    fun testMultipleEnemiesCanReachTargetInSameTurn() {
        // Simpler test: enemies are 1 step from target
        val pathCells = setOf(
            Position(8, 2), Position(9, 1), Position(9, 3)
        )
        
        val level = Level(
            id = 1,
            name = "Multiple Target Entry Test",
            gridWidth = 10,
            gridHeight = 6,
            startPositions = listOf(Position(0, 2)),
            targetPositions = listOf(Position(9, 2)),
            pathCells = pathCells,
            attackerWaves = emptyList(),
            initialCoins = 100,
            healthPoints = 10
        )
        
        val state = GameState(level)
        val engine = GameEngine(state)
        
        // Place two goblins adjacent to target (speed 2, but only 1 move needed)
        val goblin1 = Attacker(
            id = 1,
            type = AttackerType.GOBLIN,
            position = mutableStateOf(Position(8, 2)),
            level = mutableStateOf(1)
        )
        val goblin2 = Attacker(
            id = 2,
            type = AttackerType.GOBLIN,
            position = mutableStateOf(Position(9, 1)),
            level = mutableStateOf(1)
        )
        
        state.attackers.add(goblin1)
        state.attackers.add(goblin2)
        
        val initialHealth = state.healthPoints.value
        
        // Calculate and apply movements
        val movements = engine.calculateEnemyTurnMovements()
        
        for (movementStep in movements.allMovementSteps) {
            for ((attackerId, newPosition) in movementStep) {
                engine.applyMovement(attackerId, newPosition)
            }
        }
        
        // Both goblins should have reached the target
        assertTrue(goblin1.isDefeated.value, "Goblin1 should reach target")
        assertTrue(goblin2.isDefeated.value, "Goblin2 should reach target")
        
        // Health should be reduced by 2
        assertEquals(
            initialHealth - 2,
            state.healthPoints.value,
            "Health should be reduced by 2 (one for each goblin)"
        )
    }
    
    @Test
    fun testAllAdjacentEnemiesCanEnterTarget() {
        // Simpler test: all enemies are 1 step from target
        val pathCells = setOf(
            Position(8, 2), Position(9, 1), Position(9, 3)
        )
        
        val level = Level(
            id = 1,
            name = "Adjacent Target Entry Test",
            gridWidth = 10,
            gridHeight = 6,
            startPositions = listOf(Position(0, 2)),
            targetPositions = listOf(Position(9, 2)),
            pathCells = pathCells,
            attackerWaves = emptyList(),
            initialCoins = 100,
            healthPoints = 10
        )
        
        val state = GameState(level)
        val engine = GameEngine(state)
        
        // Place three orcs (speed 1) adjacent to target
        val ork1 = Attacker(
            id = 1,
            type = AttackerType.ORK,
            position = mutableStateOf(Position(8, 2)),
            level = mutableStateOf(1)
        )
        val ork2 = Attacker(
            id = 2,
            type = AttackerType.ORK,
            position = mutableStateOf(Position(9, 1)),
            level = mutableStateOf(1)
        )
        val ork3 = Attacker(
            id = 3,
            type = AttackerType.ORK,
            position = mutableStateOf(Position(9, 3)),
            level = mutableStateOf(1)
        )
        
        state.attackers.add(ork1)
        state.attackers.add(ork2)
        state.attackers.add(ork3)
        
        val initialHealth = state.healthPoints.value
        
        // Calculate and apply movements
        val movements = engine.calculateEnemyTurnMovements()
        
        for (movementStep in movements.allMovementSteps) {
            for ((attackerId, newPosition) in movementStep) {
                engine.applyMovement(attackerId, newPosition)
            }
        }
        
        // All three orcs should have reached the target
        assertTrue(ork1.isDefeated.value, "Ork1 should reach target")
        assertTrue(ork2.isDefeated.value, "Ork2 should reach target")
        assertTrue(ork3.isDefeated.value, "Ork3 should reach target")
        
        // Health should be reduced by 3
        assertEquals(
            initialHealth - 3,
            state.healthPoints.value,
            "Health should be reduced by 3 (one for each ork)"
        )
    }
}
