package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for tower undo mechanics, especially during building phase
 */
class TowerUndoTest {
    
    @Test
    fun testUndoTowerDuringInitialBuilding() {
        // Test undo during initial building phase (buildTime = 0)
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
            initialCoins = 100,
            healthPoints = 10
        )
        
        val state = GameState(level)
        val engine = GameEngine(state)
        
        // Verify we're in initial building phase
        assertEquals(GamePhase.INITIAL_BUILDING, state.phase.value)
        assertEquals(100, state.coins.value)
        
        // Place a tower - should build instantly
        assertTrue(engine.placeDefender(DefenderType.SPEAR_TOWER, Position(2, 1)))
        assertEquals(85, state.coins.value, "Should cost 15 coins")
        assertEquals(1, state.defenders.size)
        
        val tower = state.defenders.first()
        assertEquals(0, tower.buildTimeRemaining.value, "Should build instantly in initial phase")
        assertTrue(tower.isReady, "Tower should be ready")
        assertEquals(0, tower.placedOnTurn, "Placed on turn 0")
        assertFalse(tower.hasBeenUsed.value, "Tower hasn't been used")
        
        // Undo the tower - should get full refund
        assertTrue(engine.undoTower(tower.id), "Should be able to undo")
        assertEquals(100, state.coins.value, "Should get full 100% refund (15 coins)")
        assertEquals(0, state.defenders.size, "Tower should be removed")
    }
    
    @Test
    fun testUndoTowerDuringBuilding() {
        // Test undo during regular player turn (buildTime > 0)
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
            initialCoins = 200,
            healthPoints = 10
        )
        
        val state = GameState(level)
        val engine = GameEngine(state)
        
        // Start the game (move to player turn)
        engine.startFirstPlayerTurn()
        assertEquals(GamePhase.PLAYER_TURN, state.phase.value)
        assertEquals(1, state.turnNumber.value)
        
        // Place a tower - should require build time
        assertTrue(engine.placeDefender(DefenderType.WIZARD_TOWER, Position(2, 1)))
        assertEquals(150, state.coins.value, "Should cost 50 coins")
        assertEquals(1, state.defenders.size)
        
        val tower = state.defenders.first()
        assertEquals(2, tower.buildTimeRemaining.value, "Wizard tower requires 2 turns to build")
        assertFalse(tower.isReady, "Tower should not be ready yet")
        assertEquals(1, tower.placedOnTurn, "Placed on turn 1")
        assertFalse(tower.hasBeenUsed.value, "Tower hasn't been used")
        
        // Undo the tower while it's still building - should get full refund
        assertTrue(engine.undoTower(tower.id), "Should be able to undo building tower")
        assertEquals(200, state.coins.value, "Should get full 100% refund (50 coins)")
        assertEquals(0, state.defenders.size, "Tower should be removed")
    }
    
    @Test
    fun testCannotUndoTowerAfterUsed() {
        // Test that undo is not available after tower has been used
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
            initialCoins = 100,
            healthPoints = 10
        )
        
        val state = GameState(level)
        val engine = GameEngine(state)
        
        // Place a tower during initial building phase
        assertTrue(engine.placeDefender(DefenderType.SPEAR_TOWER, Position(2, 1)))
        val tower = state.defenders.first()
        
        // Mark tower as used
        tower.hasBeenUsed.value = true
        
        // Try to undo - should fail
        assertFalse(engine.undoTower(tower.id), "Should not be able to undo used tower")
        assertEquals(1, state.defenders.size, "Tower should still exist")
    }
    
    @Test
    fun testCannotUndoTowerOnDifferentTurn() {
        // Test that undo is not available on a different turn
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
            initialCoins = 200,
            healthPoints = 10
        )
        
        val state = GameState(level)
        val engine = GameEngine(state)
        
        // Start the game
        engine.startFirstPlayerTurn()
        assertEquals(1, state.turnNumber.value)
        
        // Place a tower
        assertTrue(engine.placeDefender(DefenderType.SPEAR_TOWER, Position(2, 1)))
        val tower = state.defenders.first()
        assertEquals(1, tower.placedOnTurn, "Placed on turn 1")
        
        // End turn and start new turn
        engine.startEnemyTurn(); engine.completeEnemyTurn()
        engine.startEnemyTurn()
        engine.completeEnemyTurn()
        state.turnNumber.value = 2
        
        // Try to undo - should fail (different turn)
        assertFalse(engine.undoTower(tower.id), "Should not be able to undo tower from previous turn")
        assertEquals(1, state.defenders.size, "Tower should still exist")
    }
    
    @Test
    fun testUndoUpgradedTowerRefundsTotal() {
        // Test that undo returns the total cost including upgrades
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
            initialCoins = 1000,
            healthPoints = 10
        )
        
        val state = GameState(level)
        val engine = GameEngine(state)
        
        // Verify we're in initial building phase
        assertEquals(GamePhase.INITIAL_BUILDING, state.phase.value)
        
        // Place a tower
        assertTrue(engine.placeDefender(DefenderType.SPEAR_TOWER, Position(2, 1)))
        assertEquals(985, state.coins.value, "Should cost 15 coins")
        
        val tower = state.defenders.first()
        
        // Upgrade the tower (costs 15 more)
        assertTrue(engine.upgradeDefender(tower.id))
        assertEquals(970, state.coins.value, "Should cost 15 more coins for upgrade")
        assertEquals(2, tower.level.value, "Tower should be level 2")
        assertEquals(30, tower.totalCost, "Total cost should be 30 (base 15 + upgrade 15)")
        
        // Undo the tower - should get full refund of total cost
        assertTrue(engine.undoTower(tower.id), "Should be able to undo")
        assertEquals(1000, state.coins.value, "Should get full refund of total cost (30 coins)")
        assertEquals(0, state.defenders.size, "Tower should be removed")
    }
}
