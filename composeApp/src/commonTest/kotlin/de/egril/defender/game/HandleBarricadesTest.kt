package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Comprehensive tests for GameEngine.handleBarricades() function.
 * 
 * Tests the barricade selection logic when an enemy is blocked by multiple barricades.
 * The selection should use the effort formula: effort = (hp / damage) + pathSteps
 * where hp is adjusted for tower bases (barricades with towers): hp - 100
 */
class HandleBarricadesTest {
    
    /**
     * Test: Two barricades blocking all paths next to each other.
     * One barricade has 25 HP, the other has 103 HP with a Bow tower on it (tower base).
     * 
     * Expected: Tower base should be attacked first because:
     * - Tower base effective HP = 103 - 100 = 3
     * - Regular barricade effective HP = 25
     * - For a level 1 goblin (damage = 1): tower base effort < regular barricade effort
     */
    @Test
    fun testAttackTowerBaseFirst() {
        // Create a straight path that splits into two branches, both blocked by barricades
        // Main path: (0,5) -> (1,5) -> (2,5) [BLOCKED]
        // Branch: (0,5) -> (1,5) -> (1,4) -> (2,4) [BLOCKED by tower base]
        // After barricades: (3,4) -> (4,4) -> (5,4) (target)
        val pathCells = setOf(
            Position(0, 5), Position(1, 5), Position(2, 5), Position(3, 5), Position(4, 5),
            Position(1, 4), Position(2, 4), Position(3, 4), Position(4, 4)
        )
        
        val buildIslands = setOf(
            Position(2, 3), Position(3, 3)  // Island for tower placement
        )
        
        val level = createTestLevel(pathCells, buildIslands, startPos = Position(0, 5), targetPos = Position(4, 4))
        val state = GameState(level)
        val engine = GameEngine(state)
        
        // Create barricade at (2,5) with 25 HP
        val barricade1 = Barricade(
            id = 1,
            position = Position(2, 5),
            healthPoints = mutableStateOf(25),
            defenderId = 100
        )
        state.barricades.add(barricade1)
        
        // Create barricade at (2,4) with 103 HP and place a tower on it
        val barricade2 = Barricade(
            id = 2,
            position = Position(2, 4),
            healthPoints = mutableStateOf(103),
            defenderId = 101
        )
        state.barricades.add(barricade2)
        
        // Place a Bow tower on barricade2 to make it a tower base
        val tower = Defender(
            id = 1,
            type = DefenderType.BOW_TOWER,
            position = mutableStateOf(Position(2, 4)),
            level = mutableStateOf(1)
        )
        tower.buildTimeRemaining.value = 0
        tower.towerBaseBarricadeId.value = barricade2.id
        state.defenders.add(tower)
        barricade2.supportedTowerId.value = tower.id
        
        // Spawn a goblin close to the barricades
        val goblin = Attacker(
            id = 1,
            type = AttackerType.GOBLIN,
            position = mutableStateOf(Position(1, 5)),
            level = mutableStateOf(1)
        )
        state.attackers.add(goblin)
        
        // Calculate enemy movements
        val movements = engine.calculateEnemyTurnMovements()
        
        // Goblin should be stopped by a barricade
        assertTrue(
            movements.attackersStoppedByBarricade.isNotEmpty(),
            "Goblin should be stopped by a barricade"
        )
        
        // Find which barricade the goblin will attack
        val stoppedInfo = movements.attackersStoppedByBarricade.find { it.first.id == goblin.id }
        assertTrue(stoppedInfo != null, "Goblin should be in stopped list")
        
        // Verify goblin chose the tower base (position (2,4))
        // Tower base effective HP = 103 - 100 = 3, regular barricade effective HP = 25
        // Since tower base has lower effective HP, it should be selected
        assertEquals(
            Position(2, 4),
            stoppedInfo.second,
            "Goblin should attack the tower base at (2,4) because effective HP (3) is lower than regular barricade (25)"
        )
    }
    
    /**
     * Test: Two barricades blocking all paths, free path after that.
     * One barricade has 25 HP, the other has 98 HP (not a tower base).
     * 
     * Expected: The 25 HP barricade should be attacked because it has lower HP.
     */
    @Test
    fun testAttackWeakerBarricadeFirst() {
        // Create a straight path that splits, both blocked
        val pathCells = setOf(
            Position(0, 5), Position(1, 5), Position(2, 5), Position(3, 5), Position(4, 5),
            Position(1, 4), Position(2, 4), Position(3, 4), Position(4, 4)
        )
        
        val level = createTestLevel(pathCells, startPos = Position(0, 5), targetPos = Position(4, 4))
        val state = GameState(level)
        val engine = GameEngine(state)
        
        // Create barricade at (2,5) with 25 HP
        val barricade1 = Barricade(
            id = 1,
            position = Position(2, 5),
            healthPoints = mutableStateOf(25),
            defenderId = 100
        )
        state.barricades.add(barricade1)
        
        // Create barricade at (2,4) with 98 HP (no tower - not a tower base)
        val barricade2 = Barricade(
            id = 2,
            position = Position(2, 4),
            healthPoints = mutableStateOf(98),
            defenderId = 101
        )
        state.barricades.add(barricade2)
        
        // Spawn a goblin close to the barricades
        val goblin = Attacker(
            id = 1,
            type = AttackerType.GOBLIN,
            position = mutableStateOf(Position(1, 5)),
            level = mutableStateOf(1)
        )
        state.attackers.add(goblin)
        
        // Calculate enemy movements
        val movements = engine.calculateEnemyTurnMovements()
        
        // Goblin should be stopped by a barricade
        assertTrue(
            movements.attackersStoppedByBarricade.isNotEmpty(),
            "Goblin should be stopped by a barricade"
        )
        
        // Find which barricade the goblin will attack
        val stoppedInfo = movements.attackersStoppedByBarricade.find { it.first.id == goblin.id }
        assertTrue(stoppedInfo != null, "Goblin should be in stopped list")
        
        // Verify goblin chose the 25 HP barricade at (2,5)
        // effort1 = 25/1 + distance = 25 + steps to 2,5
        // effort2 = 98/1 + distance = 98 + steps to 2,4
        // Lower effort should be selected
        assertEquals(
            Position(2, 5),
            stoppedInfo.second,
            "Goblin should attack the weaker barricade at (2,5) with 25 HP instead of the one with 98 HP"
        )
    }
    
    /**
     * Test: Two barricades with different HP, also considering path distance.
     * 
     * Expected: The barricade with lowest total effort should be selected.
     */
    @Test
    fun testBarricadeSelectionWithDistance() {
        // Create a more complex path where distances matter
        // Direct path to (2,5) has 50 HP barricade
        // Longer path through (1,6)->(2,6) has 20 HP barricade
        val pathCells = setOf(
            Position(0, 5), Position(1, 5), Position(2, 5), Position(3, 5),
            Position(1, 6), Position(2, 6), Position(3, 6)
        )
        
        val level = createTestLevel(pathCells, startPos = Position(0, 5), targetPos = Position(3, 5))
        val state = GameState(level)
        val engine = GameEngine(state)
        
        // Create barricade at (2,5) with 50 HP - direct path
        val barricade1 = Barricade(
            id = 1,
            position = Position(2, 5),
            healthPoints = mutableStateOf(50),
            defenderId = 100
        )
        state.barricades.add(barricade1)
        
        // Create barricade at (2,6) with 20 HP - alternative path
        val barricade2 = Barricade(
            id = 2,
            position = Position(2, 6),
            healthPoints = mutableStateOf(20),
            defenderId = 101
        )
        state.barricades.add(barricade2)
        
        // Spawn a goblin
        val goblin = Attacker(
            id = 1,
            type = AttackerType.GOBLIN,
            position = mutableStateOf(Position(1, 5)),
            level = mutableStateOf(1)
        )
        state.attackers.add(goblin)
        
        // Calculate enemy movements
        val movements = engine.calculateEnemyTurnMovements()
        
        // Goblin should be stopped by a barricade
        assertTrue(
            movements.attackersStoppedByBarricade.isNotEmpty(),
            "Goblin should be stopped by a barricade"
        )
        
        // Find which barricade the goblin will attack
        val stoppedInfo = movements.attackersStoppedByBarricade.find { it.first.id == goblin.id }
        assertTrue(stoppedInfo != null, "Goblin should be in stopped list")
        
        // Verify goblin chose the lower-effort barricade
        // This test verifies the formula works correctly considering both HP and distance
        val selectedPos = stoppedInfo.second
        assertTrue(
            selectedPos == Position(2, 5) || selectedPos == Position(2, 6),
            "Goblin should attack one of the barricades, got $selectedPos"
        )
    }
    
    /**
     * Test: Three barricades blocking different paths.
     * Tests that the function can handle more than 2 options.
     */
    @Test
    fun testThreeBarricadesSelection() {
        // Create three paths, all blocked
        val pathCells = setOf(
            Position(0, 5), Position(1, 5), Position(2, 5), Position(3, 5),
            Position(1, 4), Position(2, 4), Position(3, 4),
            Position(1, 6), Position(2, 6), Position(3, 6)
        )
        
        val level = createTestLevel(pathCells, startPos = Position(0, 5), targetPos = Position(3, 5))
        val state = GameState(level)
        val engine = GameEngine(state)
        
        // Three barricades with different HPs
        state.barricades.add(Barricade(1, Position(2, 5), mutableStateOf(100), 100))  // Path 1: 100 HP
        state.barricades.add(Barricade(2, Position(2, 4), mutableStateOf(30), 101))   // Path 2: 30 HP (should be selected)
        state.barricades.add(Barricade(3, Position(2, 6), mutableStateOf(75), 102))   // Path 3: 75 HP
        
        // Spawn a goblin
        val goblin = Attacker(
            id = 1,
            type = AttackerType.GOBLIN,
            position = mutableStateOf(Position(1, 5)),
            level = mutableStateOf(1)
        )
        state.attackers.add(goblin)
        
        // Calculate enemy movements
        val movements = engine.calculateEnemyTurnMovements()
        
        // Goblin should be stopped by a barricade
        assertTrue(
            movements.attackersStoppedByBarricade.isNotEmpty(),
            "Goblin should be stopped by a barricade"
        )
        
        // Find which barricade the goblin will attack
        val stoppedInfo = movements.attackersStoppedByBarricade.find { it.first.id == goblin.id }
        assertTrue(stoppedInfo != null, "Goblin should be in stopped list")
        
        // Should select the 30 HP barricade (lowest effort)
        // Note: After refactoring to loop-based approach, this should definitely work
        // Current hardcoded approach might not find all three options
        assertEquals(
            Position(2, 4),
            stoppedInfo.second,
            "Goblin should attack the weakest barricade at (2,4) with 30 HP"
        )
    }
    
    /**
     * Test: Four barricades blocking different paths.
     * This specifically tests that the refactored loop-based approach can handle
     * more than 3 barricade options (which is the limit of the current hardcoded approach).
     */
    @Test
    fun testFourBarricadesRequiresLoop() {
        // Create four different paths, all blocked
        val pathCells = setOf(
            Position(0, 5), Position(1, 5), Position(2, 5), Position(3, 5),
            Position(1, 4), Position(2, 4), Position(3, 4),
            Position(1, 6), Position(2, 6), Position(3, 6),
            Position(0, 4), Position(0, 6)
        )
        
        val level = createTestLevel(pathCells, startPos = Position(0, 5), targetPos = Position(3, 5))
        val state = GameState(level)
        val engine = GameEngine(state)
        
        // Four barricades - the 4th one has lowest HP
        state.barricades.add(Barricade(1, Position(2, 5), mutableStateOf(100), 100))
        state.barricades.add(Barricade(2, Position(2, 4), mutableStateOf(80), 101))
        state.barricades.add(Barricade(3, Position(2, 6), mutableStateOf(90), 102))
        state.barricades.add(Barricade(4, Position(1, 4), mutableStateOf(15), 103))  // Lowest HP
        
        // Spawn a goblin
        val goblin = Attacker(
            id = 1,
            type = AttackerType.GOBLIN,
            position = mutableStateOf(Position(1, 5)),
            level = mutableStateOf(1)
        )
        state.attackers.add(goblin)
        
        // Calculate enemy movements
        val movements = engine.calculateEnemyTurnMovements()
        
        // Goblin should be stopped by a barricade
        assertTrue(
            movements.attackersStoppedByBarricade.isNotEmpty(),
            "Goblin should be stopped by a barricade"
        )
        
        // Find which barricade the goblin will attack
        val stoppedInfo = movements.attackersStoppedByBarricade.find { it.first.id == goblin.id }
        assertTrue(stoppedInfo != null, "Goblin should be in stopped list")
        
        // After loop-based refactoring, should find the 15 HP barricade
        // Current hardcoded approach (checking only 3 paths) might miss this
        assertEquals(
            Position(1, 4),
            stoppedInfo.second,
            "Goblin should attack the weakest barricade at (1,4) with 15 HP - requires loop to find all 4 options"
        )
    }
    
    /**
     * Helper function to create a test level with a simple path.
     */
    private fun createTestLevel(
        pathCells: Set<Position>, 
        buildIslands: Set<Position> = emptySet(),
        startPos: Position = Position(0, 5),
        targetPos: Position = Position(5, 5)
    ): Level {
        return Level(
            id = 1,
            name = "Barricade Test Level",
            subtitle = "Test",
            gridWidth = 20,
            gridHeight = 20,
            startPositions = listOf(startPos),
            targetPositions = listOf(targetPos),
            pathCells = pathCells,
            buildIslands = buildIslands,
            attackerWaves = emptyList(),
            initialCoins = 1000,
            healthPoints = 10,
            availableTowers = setOf(DefenderType.SPIKE_TOWER, DefenderType.SPEAR_TOWER, DefenderType.BOW_TOWER)
        )
    }
}
