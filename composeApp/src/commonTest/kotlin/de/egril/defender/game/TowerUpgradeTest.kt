package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for tower upgrade mechanics
 */
class TowerUpgradeTest {
    
    @Test
    fun testUpgradeDoesNotResetUsedActions() {
        // Create a simple level for testing
        val level = Level(
            id = 1,
            name = "Test Level",
            gridWidth = 10,
            gridHeight = 6,
            startPositions = listOf(Position(0, 3)),
            targetPositions = listOf(Position(9, 3)),
            pathCells = (0..9).map { Position(it, 3) }.toSet(),
            buildAreas = setOf(Position(2, 1), Position(2, 2)),  // Create build areas
            attackerWaves = listOf(
                AttackerWave(listOf(AttackerType.GOBLIN))
            ),
            initialCoins = 1000,
            healthPoints = 10
        )
        
        val state = GameState(level)
        val engine = GameEngine(state)
        
        // Place a tower at level 1 with 1 action on a build island
        assertTrue(engine.placeDefender(DefenderType.SPEAR_TOWER, Position(2, 1)))
        val tower = state.defenders.first()
        
        // Tower starts with 0 actions because it's in INITIAL_BUILDING phase
        assertEquals(GamePhase.INITIAL_BUILDING, state.phase.value)
        assertEquals(1, tower.actionsPerTurnCalculated, "Level 1 tower should have 1 action per turn")
        
        // Start the game to give the tower its actions
        engine.startFirstPlayerTurn()
        assertEquals(GamePhase.PLAYER_TURN, state.phase.value)
        assertEquals(1, tower.actionsRemaining.value, "Tower should have 1 action after turn starts")
        
        // Use the tower's action
        tower.actionsRemaining.value = 0
        assertEquals(0, tower.actionsRemaining.value, "Tower should have 0 actions after use")
        
        // Upgrade the tower (level 1 -> level 2)
        // Level 2 SPEAR_TOWER still has 1 action per turn (no change)
        val initialLevel = tower.level.value
        assertTrue(engine.upgradeDefender(tower.id))
        assertEquals(initialLevel + 1, tower.level.value, "Tower level should increase")
        assertEquals(1, tower.actionsPerTurnCalculated, "Level 2 tower should still have 1 action per turn")
        
        // BUG: Currently actionsRemaining gets reset to 1
        // EXPECTED: actionsRemaining should stay at 0 (no change in actionsPerTurn)
        assertEquals(0, tower.actionsRemaining.value, "Tower should still have 0 actions (used before upgrade)")
    }
    
    @Test
    fun testUpgradeIncrementsActionsWhenActionsPerTurnIncreases() {
        // Create a simple level for testing
        val level = Level(
            id = 1,
            name = "Test Level",
            gridWidth = 10,
            gridHeight = 6,
            startPositions = listOf(Position(0, 3)),
            targetPositions = listOf(Position(9, 3)),
            pathCells = (0..9).map { Position(it, 3) }.toSet(),
            buildAreas = setOf(Position(2, 1), Position(2, 2)),
            attackerWaves = listOf(
                AttackerWave(listOf(AttackerType.GOBLIN))
            ),
            initialCoins = 10000,
            healthPoints = 10
        )
        
        val state = GameState(level)
        val engine = GameEngine(state)
        
        // Place a pike (spike) tower at level 1 with 1 action
        assertTrue(engine.placeDefender(DefenderType.SPIKE_TOWER, Position(2, 1)))
        val tower = state.defenders.first()
        
        // Start the game
        engine.startFirstPlayerTurn()
        
        // Upgrade tower to level 4 (still 1 action per turn)
        tower.level.value = 4
        tower.resetActions()
        assertEquals(1, tower.actionsPerTurnCalculated, "Level 4 pike tower should have 1 action")
        assertEquals(1, tower.actionsRemaining.value, "Tower should have 1 action")
        
        // Use the action
        tower.actionsRemaining.value = 0
        assertEquals(0, tower.actionsRemaining.value, "Tower should have 0 actions")
        
        // Upgrade from level 4 to level 5 (gains +1 action per turn: 1 -> 2)
        assertTrue(engine.upgradeDefender(tower.id))
        assertEquals(5, tower.level.value, "Tower should be level 5")
        assertEquals(2, tower.actionsPerTurnCalculated, "Level 5 pike tower should have 2 actions")
        
        // EXPECTED: Should gain +1 action (0 -> 1) because actionsPerTurn increased by 1
        assertEquals(1, tower.actionsRemaining.value, "Tower should gain 1 action (the increase in actionsPerTurn)")
    }
    
    @Test
    fun testUpgradeWithPartialActionsUsed() {
        // Create a simple level for testing
        val level = Level(
            id = 1,
            name = "Test Level",
            gridWidth = 10,
            gridHeight = 6,
            startPositions = listOf(Position(0, 3)),
            targetPositions = listOf(Position(9, 3)),
            pathCells = (0..9).map { Position(it, 3) }.toSet(),
            buildAreas = setOf(Position(2, 1), Position(2, 2)),
            attackerWaves = listOf(
                AttackerWave(listOf(AttackerType.GOBLIN))
            ),
            initialCoins = 10000,
            healthPoints = 10
        )
        
        val state = GameState(level)
        val engine = GameEngine(state)
        
        // Place a pike tower and upgrade it to level 5 (2 actions per turn)
        assertTrue(engine.placeDefender(DefenderType.SPIKE_TOWER, Position(2, 1)))
        val tower = state.defenders.first()
        tower.level.value = 5
        
        // Start the game
        engine.startFirstPlayerTurn()
        tower.resetActions()
        
        assertEquals(2, tower.actionsPerTurnCalculated, "Level 5 pike tower should have 2 actions")
        assertEquals(2, tower.actionsRemaining.value, "Tower should have 2 actions")
        
        // Use 1 action (1 remaining)
        tower.actionsRemaining.value = 1
        
        // Upgrade to level 6 (still 2 actions per turn, no change)
        assertTrue(engine.upgradeDefender(tower.id))
        assertEquals(6, tower.level.value, "Tower should be level 6")
        assertEquals(2, tower.actionsPerTurnCalculated, "Level 6 pike tower should still have 2 actions")
        
        // EXPECTED: Should still have 1 action (no change in actionsPerTurn)
        assertEquals(1, tower.actionsRemaining.value, "Tower should still have 1 action (no change in actionsPerTurn)")
    }
    
    @Test
    fun testUpgradeFromLevel9ToLevel10() {
        // This is the key case: pike tower goes from 2 actions to 3 actions
        val level = Level(
            id = 1,
            name = "Test Level",
            gridWidth = 10,
            gridHeight = 6,
            startPositions = listOf(Position(0, 3)),
            targetPositions = listOf(Position(9, 3)),
            pathCells = (0..9).map { Position(it, 3) }.toSet(),
            buildAreas = setOf(Position(2, 1), Position(2, 2)),
            attackerWaves = listOf(
                AttackerWave(listOf(AttackerType.GOBLIN))
            ),
            initialCoins = 100000,
            healthPoints = 10
        )
        
        val state = GameState(level)
        val engine = GameEngine(state)
        
        // Place and upgrade pike tower to level 9
        assertTrue(engine.placeDefender(DefenderType.SPIKE_TOWER, Position(2, 1)))
        val tower = state.defenders.first()
        tower.level.value = 9
        
        // Start the game
        engine.startFirstPlayerTurn()
        tower.resetActions()
        
        assertEquals(2, tower.actionsPerTurnCalculated, "Level 9 pike tower should have 2 actions")
        assertEquals(2, tower.actionsRemaining.value, "Tower should have 2 actions")
        
        // Use both actions
        tower.actionsRemaining.value = 0
        
        // Upgrade to level 10 (gains +1 action: 2 -> 3)
        assertTrue(engine.upgradeDefender(tower.id))
        assertEquals(10, tower.level.value, "Tower should be level 10")
        assertEquals(3, tower.actionsPerTurnCalculated, "Level 10 pike tower should have 3 actions")
        
        // EXPECTED: Should gain 1 action (0 -> 1) because actionsPerTurn increased by 1
        assertEquals(1, tower.actionsRemaining.value, "Tower should gain 1 action (the increase in actionsPerTurn)")
    }
    
    @Test
    fun testMineUpgradeActions() {
        // Test dwarven mine upgrade which also gains actions
        val level = Level(
            id = 1,
            name = "Test Level",
            gridWidth = 10,
            gridHeight = 6,
            startPositions = listOf(Position(0, 3)),
            targetPositions = listOf(Position(9, 3)),
            pathCells = (0..9).map { Position(it, 3) }.toSet(),
            buildAreas = setOf(Position(2, 1), Position(2, 2)),
            attackerWaves = listOf(
                AttackerWave(listOf(AttackerType.GOBLIN))
            ),
            initialCoins = 100000,
            healthPoints = 10
        )
        
        val state = GameState(level)
        val engine = GameEngine(state)
        
        // Place mine at level 4 (1 action)
        assertTrue(engine.placeDefender(DefenderType.DWARVEN_MINE, Position(2, 1)))
        val mine = state.defenders.first()
        mine.level.value = 4
        
        // Start the game
        engine.startFirstPlayerTurn()
        mine.resetActions()
        
        assertEquals(1, mine.actionsPerTurnCalculated, "Level 4 mine should have 1 action")
        assertEquals(1, mine.actionsRemaining.value, "Mine should have 1 action")
        
        // Use the action
        mine.actionsRemaining.value = 0
        
        // Upgrade to level 5 (gains +1 action: 1 -> 2)
        assertTrue(engine.upgradeDefender(mine.id))
        assertEquals(5, mine.level.value, "Mine should be level 5")
        assertEquals(2, mine.actionsPerTurnCalculated, "Level 5 mine should have 2 actions")
        
        // EXPECTED: Should gain 1 action (0 -> 1) because actionsPerTurn increased by 1
        assertEquals(1, mine.actionsRemaining.value, "Mine should gain 1 action (the increase in actionsPerTurn)")
    }
}
