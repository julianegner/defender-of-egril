package com.defenderofegril.game

import androidx.compose.runtime.mutableStateOf
import com.defenderofegril.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for dragon movement mechanics:
 * - Turn 1: 1 tile on path (walking)
 * - Turn 2+: Up to 5 tiles, flying over obstacles, but must end on path
 */
class DragonMovementTest {
    
    /**
     * Test that dragon moves 1 tile on first turn (walking)
     */
    @Test
    fun testDragonWalksOnFirstTurn() {
        // Create a simple level with a straight path
        val pathCells = (0..10).map { Position(it, 3) }.toSet()
        val level = Level(
            id = 1,
            name = "Test",
            gridWidth = 15,
            gridHeight = 8,
            startPositions = listOf(Position(0, 3)),
            targetPosition = Position(10, 3),
            pathCells = pathCells,
            buildIslands = emptySet(),
            attackerWaves = emptyList(),
            initialCoins = 100,
            healthPoints = 10
        )
        
        val state = GameState(level, mutableStateOf(GamePhase.ENEMY_TURN))
        val pathfinding = PathfindingSystem(state)
        val movementSystem = EnemyMovementSystem(state, pathfinding)
        
        // Spawn dragon at start
        val dragon = Attacker(
            id = 1,
            type = AttackerType.DRAGON,
            position = mutableStateOf(Position(0, 3)),
            level = 1
        )
        state.attackers.add(dragon)
        
        // Move dragon - first turn should walk 1 tile
        movementSystem.moveAttackers(
            findNearestActiveTower = { null },
            findPathPositionNearTower = { state.level.targetPosition }
        )
        
        // Check dragon moved exactly 1 tile and is on path
        assertEquals(Position(1, 3), dragon.position.value)
        assertTrue(state.level.isOnPath(dragon.position.value))
        assertFalse(dragon.isFlying.value)
        assertEquals(1, dragon.dragonTurnsSinceSpawned.value)
    }
    
    /**
     * Test that dragon flies up to 5 tiles on second turn and ends on path
     */
    @Test
    fun testDragonFliesOnSecondTurn() {
        // Create a level with path and some islands (obstacles)
        val pathCells = mutableSetOf<Position>()
        // Straight path along y=3
        for (x in 0..15) {
            pathCells.add(Position(x, 3))
        }
        
        val islands = setOf(
            Position(5, 1), Position(6, 1),
            Position(5, 2), Position(6, 2)
        )
        
        val level = Level(
            id = 1,
            name = "Test",
            gridWidth = 20,
            gridHeight = 8,
            startPositions = listOf(Position(0, 3)),
            targetPosition = Position(15, 3),
            pathCells = pathCells,
            buildIslands = islands,
            attackerWaves = emptyList(),
            initialCoins = 100,
            healthPoints = 10
        )
        
        val state = GameState(level, mutableStateOf(GamePhase.ENEMY_TURN))
        val pathfinding = PathfindingSystem(state)
        val movementSystem = EnemyMovementSystem(state, pathfinding)
        
        // Spawn dragon at start and simulate first turn
        val dragon = Attacker(
            id = 1,
            type = AttackerType.DRAGON,
            position = mutableStateOf(Position(1, 3)),
            level = 1,
            dragonTurnsSinceSpawned = mutableStateOf(1) // Already moved once
        )
        state.attackers.add(dragon)
        
        // Move dragon - second turn should fly up to 5 tiles
        movementSystem.moveAttackers(
            findNearestActiveTower = { null },
            findPathPositionNearTower = { state.level.targetPosition }
        )
        
        // Check dragon is flying and moved multiple tiles
        assertTrue(dragon.isFlying.value)
        assertEquals(2, dragon.dragonTurnsSinceSpawned.value)
        
        // Dragon should have moved towards target (at least 1 tile, up to 5)
        val distanceMoved = dragon.position.value.x - 1 // Started at x=1
        assertTrue(distanceMoved > 0, "Dragon should have moved forward")
        assertTrue(distanceMoved <= 5, "Dragon should not move more than 5 tiles")
        
        // Most importantly: dragon must end on a path tile
        assertTrue(state.level.isOnPath(dragon.position.value), 
            "Dragon must end on path after flying, but ended at ${dragon.position.value}")
    }
    
    /**
     * Test that flying dragon can move over obstacles (islands)
     */
    @Test
    fun testDragonFliesOverObstacles() {
        // Create a path that curves around an obstacle
        val pathCells = mutableSetOf<Position>()
        // Path goes: (0,3) -> (1,3) -> (2,3) -> (3,4) -> (4,4) -> (5,4) -> (6,3) -> (7,3)
        pathCells.add(Position(0, 3))
        pathCells.add(Position(1, 3))
        pathCells.add(Position(2, 3))
        pathCells.add(Position(3, 4))
        pathCells.add(Position(4, 4))
        pathCells.add(Position(5, 4))
        pathCells.add(Position(6, 3))
        pathCells.add(Position(7, 3))
        pathCells.add(Position(8, 3))
        pathCells.add(Position(9, 3))
        pathCells.add(Position(10, 3))
        
        // Island blocks direct route
        val islands = setOf(
            Position(3, 3), Position(4, 3), Position(5, 3)
        )
        
        val level = Level(
            id = 1,
            name = "Test",
            gridWidth = 15,
            gridHeight = 8,
            startPositions = listOf(Position(0, 3)),
            targetPosition = Position(10, 3),
            pathCells = pathCells,
            buildIslands = islands,
            attackerWaves = emptyList(),
            initialCoins = 100,
            healthPoints = 10
        )
        
        val state = GameState(level, mutableStateOf(GamePhase.ENEMY_TURN))
        val pathfinding = PathfindingSystem(state)
        val movementSystem = EnemyMovementSystem(state, pathfinding)
        
        // Dragon after first turn at position (1,3)
        val dragon = Attacker(
            id = 1,
            type = AttackerType.DRAGON,
            position = mutableStateOf(Position(1, 3)),
            level = 1,
            dragonTurnsSinceSpawned = mutableStateOf(1)
        )
        state.attackers.add(dragon)
        
        // Move dragon
        movementSystem.moveAttackers(
            findNearestActiveTower = { null },
            findPathPositionNearTower = { state.level.targetPosition }
        )
        
        // Dragon should be able to fly past the obstacle and land on path beyond it
        assertTrue(dragon.isFlying.value)
        assertTrue(state.level.isOnPath(dragon.position.value))
        
        // Dragon should have moved significantly (can fly over the obstacle)
        val finalX = dragon.position.value.x
        assertTrue(finalX > 3, "Dragon should fly over obstacle at x=3-5, ended at x=$finalX")
    }
    
    /**
     * Test that dragon ends on path even when direct path would be off-path
     */
    @Test
    fun testDragonMustEndOnPath() {
        // Create a path with a detour
        val pathCells = mutableSetOf<Position>()
        // Straight path at y=3
        for (x in 0..2) pathCells.add(Position(x, 3))
        // Path curves up
        pathCells.add(Position(3, 2))
        pathCells.add(Position(4, 2))
        pathCells.add(Position(5, 2))
        // Path curves back down
        pathCells.add(Position(6, 3))
        for (x in 7..12) pathCells.add(Position(x, 3))
        
        val level = Level(
            id = 1,
            name = "Test",
            gridWidth = 15,
            gridHeight = 8,
            startPositions = listOf(Position(0, 3)),
            targetPosition = Position(12, 3),
            pathCells = pathCells,
            buildIslands = emptySet(),
            attackerWaves = emptyList(),
            initialCoins = 100,
            healthPoints = 10
        )
        
        val state = GameState(level, mutableStateOf(GamePhase.ENEMY_TURN))
        val pathfinding = PathfindingSystem(state)
        val movementSystem = EnemyMovementSystem(state, pathfinding)
        
        // Dragon at position after first turn
        val dragon = Attacker(
            id = 1,
            type = AttackerType.DRAGON,
            position = mutableStateOf(Position(1, 3)),
            level = 1,
            dragonTurnsSinceSpawned = mutableStateOf(1)
        )
        state.attackers.add(dragon)
        
        // Move dragon multiple times to test consistent behavior
        for (turn in 1..3) {
            movementSystem.moveAttackers(
                findNearestActiveTower = { null },
                findPathPositionNearTower = { state.level.targetPosition }
            )
            
            // Each turn after first, dragon must end on path
            assertTrue(state.level.isOnPath(dragon.position.value),
                "Turn $turn: Dragon at ${dragon.position.value} must be on path")
            assertTrue(dragon.isFlying.value, "Turn $turn: Dragon should be flying")
        }
    }
    
    /**
     * Test that demonstrates the bug: dragon flies off-path when moving directly to target
     */
    @Test
    fun testDragonCannotEndOffPath() {
        // Create a narrow path that forces the dragon to stay on it
        val pathCells = mutableSetOf<Position>()
        // Path at y=3 from x=0 to x=5
        for (x in 0..5) pathCells.add(Position(x, 3))
        // Path turns at y=2 from x=6 to x=10
        for (x in 6..10) pathCells.add(Position(x, 2))
        // Connect the turn
        pathCells.add(Position(5, 2))
        pathCells.add(Position(6, 3))
        
        val level = Level(
            id = 1,
            name = "Test",
            gridWidth = 15,
            gridHeight = 8,
            startPositions = listOf(Position(0, 3)),
            targetPosition = Position(10, 2),
            pathCells = pathCells,
            buildIslands = emptySet(),
            attackerWaves = emptyList(),
            initialCoins = 100,
            healthPoints = 10
        )
        
        val state = GameState(level, mutableStateOf(GamePhase.ENEMY_TURN))
        val pathfinding = PathfindingSystem(state)
        val movementSystem = EnemyMovementSystem(state, pathfinding)
        
        // Dragon at position (2,3) having already walked one turn
        val dragon = Attacker(
            id = 1,
            type = AttackerType.DRAGON,
            position = mutableStateOf(Position(2, 3)),
            level = 1,
            dragonTurnsSinceSpawned = mutableStateOf(1) // Ready to fly
        )
        state.attackers.add(dragon)
        
        // Move dragon - it should fly but must end on path
        movementSystem.moveAttackers(
            findNearestActiveTower = { null },
            findPathPositionNearTower = { state.level.targetPosition }
        )
        
        // Dragon MUST be on path after flying
        assertTrue(state.level.isOnPath(dragon.position.value),
            "Dragon at ${dragon.position.value} MUST be on path after flying. Path cells: $pathCells")
        assertTrue(dragon.isFlying.value)
    }
}
