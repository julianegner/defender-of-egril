package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for dragon movement using the actual game flow:
 * calculateEnemyTurnMovements() -> applyMovement()
 * 
 * This tests the production code path that the game actually uses.
 */
class DragonMovementIntegrationTest {
    
    @Test
    fun testDragonAlternatesWalkingAndFlyingViaGameEngine() {
        // Create a simple level with a straight path
        val pathCells = (0..20).map { Position(it, 3) }.toSet()
        val level = Level(
            id = 1,
            name = "Test",
            gridWidth = 25,
            gridHeight = 8,
            startPositions = listOf(Position(0, 3)),
            targetPositions = listOf(Position(20, 3)),
            pathCells = pathCells,
            buildIslands = emptySet(),
            attackerWaves = emptyList(),
            initialCoins = 100,
            healthPoints = 10
        )
        
        val state = GameState(level, mutableStateOf(GamePhase.ENEMY_TURN))
        val engine = GameEngine(state)
        
        // Spawn dragon at start
        val dragon = Attacker(
            id = 1,
            type = AttackerType.DRAGON,
            position = mutableStateOf(Position(0, 3)),
            level = mutableStateOf(1)
        )
        state.attackers.add(dragon)
        
        // Turn 1: Walk (2 tiles based on new speed)
        val movements1 = engine.calculateEnemyTurnMovements()
        assertTrue(movements1.allMovementSteps.isNotEmpty(), "Turn 1: Should have movements")
        for (stepMovements in movements1.allMovementSteps) {
            for ((attackerId, newPosition) in stepMovements) {
                engine.applyMovement(attackerId, newPosition)
            }
        }
        assertEquals(Position(2, 3), dragon.position.value, "Turn 1: Should walk 2 tiles")
        assertFalse(dragon.isFlying.value, "Turn 1: Should be walking")
        assertEquals(1, dragon.dragonTurnsSinceSpawned.value)
        
        // Turn 2: Fly (up to 10 tiles)
        val positionAfterTurn1 = dragon.position.value
        val movements2 = engine.calculateEnemyTurnMovements()
        assertTrue(movements2.allMovementSteps.isNotEmpty(), "Turn 2: Should have movements")
        for (stepMovements in movements2.allMovementSteps) {
            for ((attackerId, newPosition) in stepMovements) {
                engine.applyMovement(attackerId, newPosition)
            }
        }
        assertTrue(dragon.isFlying.value, "Turn 2: Should be flying")
        val distanceMoved = dragon.position.value.x - positionAfterTurn1.x
        assertTrue(distanceMoved > 2, "Turn 2: Should fly more than 2 tiles, moved $distanceMoved")
        assertTrue(distanceMoved <= 10, "Turn 2: Should fly at most 10 tiles, moved $distanceMoved")
        assertTrue(state.level.isOnPath(dragon.position.value), "Turn 2: Must end on path")
        assertEquals(2, dragon.dragonTurnsSinceSpawned.value)
        
        // Turn 3: Walk (2 tiles)
        val positionAfterTurn2 = dragon.position.value
        val movements3 = engine.calculateEnemyTurnMovements()
        assertTrue(movements3.allMovementSteps.isNotEmpty(), "Turn 3: Should have movements")
        for (stepMovements in movements3.allMovementSteps) {
            for ((attackerId, newPosition) in stepMovements) {
                engine.applyMovement(attackerId, newPosition)
            }
        }
        assertFalse(dragon.isFlying.value, "Turn 3: Should be walking")
        assertEquals(positionAfterTurn2.x + 2, dragon.position.value.x, "Turn 3: Should walk 2 tiles")
        assertTrue(state.level.isOnPath(dragon.position.value), "Turn 3: Must be on path")
        assertEquals(3, dragon.dragonTurnsSinceSpawned.value)
        
        // Turn 4: Fly (up to 10 tiles)
        val positionAfterTurn3 = dragon.position.value
        val movements4 = engine.calculateEnemyTurnMovements()
        assertTrue(movements4.allMovementSteps.isNotEmpty(), "Turn 4: Should have movements")
        for (stepMovements in movements4.allMovementSteps) {
            for ((attackerId, newPosition) in stepMovements) {
                engine.applyMovement(attackerId, newPosition)
            }
        }
        assertTrue(dragon.isFlying.value, "Turn 4: Should be flying")
        val distanceMovedTurn4 = dragon.position.value.x - positionAfterTurn3.x
        assertTrue(distanceMovedTurn4 > 2, "Turn 4: Should fly more than 2 tiles, moved $distanceMovedTurn4")
        assertTrue(distanceMovedTurn4 <= 10, "Turn 4: Should fly at most 10 tiles, moved $distanceMovedTurn4")
        assertTrue(state.level.isOnPath(dragon.position.value), "Turn 4: Must end on path")
        assertEquals(4, dragon.dragonTurnsSinceSpawned.value)
    }
    
    @Test
    fun testDragonEatsUnitsWhenLanding() {
        // Create path with some positions
        val pathCells = (0..15).map { Position(it, 3) }.toSet()
        val level = Level(
            id = 1,
            name = "Test",
            gridWidth = 20,
            gridHeight = 8,
            startPositions = listOf(Position(0, 3)),
            targetPositions = listOf(Position(15, 3)),
            pathCells = pathCells,
            buildIslands = emptySet(),
            attackerWaves = emptyList(),
            initialCoins = 100,
            healthPoints = 10
        )
        
        val state = GameState(level, mutableStateOf(GamePhase.ENEMY_TURN))
        val engine = GameEngine(state)
        
        // Spawn dragon
        val dragon = Attacker(
            id = 1,
            type = AttackerType.DRAGON,
            position = mutableStateOf(Position(0, 3)),
            level = mutableStateOf(1),
            currentHealth = mutableStateOf(500)
        )
        state.attackers.add(dragon)
        
        // Spawn a goblin at position (2, 3) - where dragon will walk on turn 1
        val goblin = Attacker(
            id = 2,
            type = AttackerType.GOBLIN,
            position = mutableStateOf(Position(2, 3)),
            level = mutableStateOf(1),
            currentHealth = mutableStateOf(20)
        )
        state.attackers.add(goblin)
        
        val initialDragonHealth = dragon.currentHealth.value
        
        // Turn 1: Dragon walks to (2,3) and should eat the goblin
        val movements = engine.calculateEnemyTurnMovements()
        for (stepMovements in movements.allMovementSteps) {
            for ((attackerId, newPosition) in stepMovements) {
                engine.applyMovement(attackerId, newPosition)
            }
        }
        
        assertEquals(Position(2, 3), dragon.position.value, "Dragon should move to goblin's position")
        assertTrue(goblin.isDefeated.value, "Goblin should be eaten")
        assertEquals(initialDragonHealth + 20, dragon.currentHealth.value, "Dragon should gain goblin's HP")
    }
    
    @Test
    fun testDragonCannotEatEwhad() {
        // Create path
        val pathCells = (0..15).map { Position(it, 3) }.toSet()
        val level = Level(
            id = 1,
            name = "Test",
            gridWidth = 20,
            gridHeight = 8,
            startPositions = listOf(Position(0, 3)),
            targetPositions = listOf(Position(15, 3)),
            pathCells = pathCells,
            buildIslands = emptySet(),
            attackerWaves = emptyList(),
            initialCoins = 100,
            healthPoints = 10
        )
        
        val state = GameState(level, mutableStateOf(GamePhase.ENEMY_TURN))
        val engine = GameEngine(state)
        
        // Spawn dragon
        val dragon = Attacker(
            id = 1,
            type = AttackerType.DRAGON,
            position = mutableStateOf(Position(0, 3)),
            level = mutableStateOf(1)
        )
        state.attackers.add(dragon)
        
        // Spawn Ewhad at position (2, 3)
        val ewhad = Attacker(
            id = 2,
            type = AttackerType.EWHAD,
            position = mutableStateOf(Position(2, 3)),
            level = mutableStateOf(1)
        )
        state.attackers.add(ewhad)
        
        // Turn 1: Dragon tries to walk to (2,3) but Ewhad blocks
        val movements = engine.calculateEnemyTurnMovements()
        for (stepMovements in movements.allMovementSteps) {
            for ((attackerId, newPosition) in stepMovements) {
                engine.applyMovement(attackerId, newPosition)
            }
        }
        
        // Dragon should either stay at (0,3) or move to alternate position
        assertFalse(ewhad.isDefeated.value, "Ewhad should not be eaten")
        assertTrue(dragon.position.value != ewhad.position.value, "Dragon should not be on same position as Ewhad")
    }
    
    @Test
    fun testDragonFlyingOverObstacles() {
        // Create a path with obstacles (islands) nearby
        val pathCells = mutableSetOf<Position>()
        for (x in 0..12) pathCells.add(Position(x, 3))
        
        // Add some islands that would block walking but not flying
        val buildIslands = setOf(
            Position(2, 2),
            Position(3, 2),
            Position(2, 4),
            Position(3, 4)
        )
        
        val level = Level(
            id = 1,
            name = "Test",
            gridWidth = 15,
            gridHeight = 8,
            startPositions = listOf(Position(0, 3)),
            targetPositions = listOf(Position(12, 3)),
            pathCells = pathCells,
            buildIslands = buildIslands,
            attackerWaves = emptyList(),
            initialCoins = 100,
            healthPoints = 10
        )
        
        val state = GameState(level, mutableStateOf(GamePhase.ENEMY_TURN))
        val engine = GameEngine(state)
        
        // Spawn dragon
        val dragon = Attacker(
            id = 1,
            type = AttackerType.DRAGON,
            position = mutableStateOf(Position(0, 3)),
            level = mutableStateOf(1)
        )
        state.attackers.add(dragon)
        
        // Turn 1: Walk
        val movements1 = engine.calculateEnemyTurnMovements()
        for (stepMovements in movements1.allMovementSteps) {
            for ((attackerId, newPosition) in stepMovements) {
                engine.applyMovement(attackerId, newPosition)
            }
        }
        assertEquals(Position(2, 3), dragon.position.value)
        
        // Turn 2: Fly - should be able to fly up to 10 tiles even with islands nearby
        val positionBeforeFly = dragon.position.value
        val movements2 = engine.calculateEnemyTurnMovements()
        for (stepMovements in movements2.allMovementSteps) {
            for ((attackerId, newPosition) in stepMovements) {
                engine.applyMovement(attackerId, newPosition)
            }
        }
        
        assertTrue(dragon.isFlying.value, "Should be flying on turn 2")
        val flyDistance = dragon.position.value.x - positionBeforeFly.x
        assertTrue(flyDistance > 2, "Should fly more than 2 tiles even with obstacles nearby")
        assertTrue(state.level.isOnPath(dragon.position.value), "Must land on path")
    }
}
