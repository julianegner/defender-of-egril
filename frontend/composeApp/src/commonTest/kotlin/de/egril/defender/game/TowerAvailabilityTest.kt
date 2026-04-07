package de.egril.defender.game

import de.egril.defender.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Test that only available towers can be placed in a level
 */
class TowerAvailabilityTest {
    
    @Test
    fun testLevelWithAllTowersAvailable() {
        // Create a level with all towers available (default behavior)
        val level = Level(
            id = 1,
            name = "Test Level",
            pathCells = setOf(Position(0, 0), Position(1, 0)),
            buildAreas = setOf(Position(0, 1)),
            attackerWaves = listOf(AttackerWave(listOf(AttackerType.GOBLIN))),
            availableTowers = DefenderType.entries.toSet()
        )
        
        val gameState = GameState(level)
        
        // All tower types should be available
        DefenderType.entries.forEach { type ->
            assertTrue(
                level.availableTowers.contains(type),
                "Tower type ${type.displayName} should be available"
            )
        }
    }
    
    @Test
    fun testLevelWithLimitedTowers() {
        // Create a level with only specific towers
        val availableTowers = setOf(
            DefenderType.SPIKE_TOWER,
            DefenderType.BOW_TOWER
        )
        
        val level = Level(
            id = 1,
            name = "Test Level",
            pathCells = setOf(Position(0, 0), Position(1, 0)),
            buildAreas = setOf(Position(0, 1)),
            attackerWaves = listOf(AttackerWave(listOf(AttackerType.GOBLIN))),
            availableTowers = availableTowers
        )
        
        val gameState = GameState(level)
        gameState.coins.value = 1000  // Ensure enough coins
        
        // Only specified towers should be available
        assertTrue(
            level.availableTowers.contains(DefenderType.SPIKE_TOWER),
            "Spike Tower should be available"
        )
        assertTrue(
            level.availableTowers.contains(DefenderType.BOW_TOWER),
            "Bow Tower should be available"
        )
        
        // Other towers should NOT be available
        assertFalse(
            level.availableTowers.contains(DefenderType.WIZARD_TOWER),
            "Wizard Tower should NOT be available"
        )
        assertFalse(
            level.availableTowers.contains(DefenderType.BALLISTA_TOWER),
            "Ballista Tower should NOT be available"
        )
    }
    
    @Test
    fun testCanPlaceDefenderChecksAvailability() {
        // Create a level with only spike tower
        val level = Level(
            id = 1,
            name = "Test Level",
            pathCells = setOf(Position(0, 0), Position(1, 0)),
            buildAreas = setOf(Position(0, 1)),
            attackerWaves = listOf(AttackerWave(listOf(AttackerType.GOBLIN))),
            availableTowers = setOf(DefenderType.SPIKE_TOWER)
        )
        
        val gameState = GameState(level)
        gameState.coins.value = 1000  // Ensure enough coins
        
        // Should be able to place spike tower (available and affordable)
        assertTrue(
            gameState.canPlaceDefender(DefenderType.SPIKE_TOWER),
            "Should be able to place spike tower"
        )
        
        // Should NOT be able to place bow tower (not available, even though affordable)
        assertFalse(
            gameState.canPlaceDefender(DefenderType.BOW_TOWER),
            "Should NOT be able to place bow tower (not available)"
        )
        
        // Should NOT be able to place wizard tower (not available)
        assertFalse(
            gameState.canPlaceDefender(DefenderType.WIZARD_TOWER),
            "Should NOT be able to place wizard tower (not available)"
        )
    }
    
    @Test
    fun testCanPlaceDefenderChecksCoinsAndAvailability() {
        // Create a level with spike and wizard towers
        val level = Level(
            id = 1,
            name = "Test Level",
            pathCells = setOf(Position(0, 0), Position(1, 0)),
            buildAreas = setOf(Position(0, 1)),
            attackerWaves = listOf(AttackerWave(listOf(AttackerType.GOBLIN))),
            availableTowers = setOf(DefenderType.SPIKE_TOWER, DefenderType.WIZARD_TOWER)
        )
        
        val gameState = GameState(level)
        gameState.coins.value = 15  // Only enough for spike tower (10 coins)
        
        // Should be able to place spike tower (available and affordable)
        assertTrue(
            gameState.canPlaceDefender(DefenderType.SPIKE_TOWER),
            "Should be able to place spike tower"
        )
        
        // Should NOT be able to place wizard tower (available but not affordable, costs 50)
        assertFalse(
            gameState.canPlaceDefender(DefenderType.WIZARD_TOWER),
            "Should NOT be able to place wizard tower (not affordable)"
        )
        
        // Should NOT be able to place bow tower (not available)
        assertFalse(
            gameState.canPlaceDefender(DefenderType.BOW_TOWER),
            "Should NOT be able to place bow tower (not available)"
        )
    }
    
    @Test
    fun testAvailableTowersCount() {
        val level = Level(
            id = 1,
            name = "Test Level",
            pathCells = setOf(Position(0, 0), Position(1, 0)),
            buildAreas = setOf(Position(0, 1)),
            attackerWaves = listOf(AttackerWave(listOf(AttackerType.GOBLIN))),
            availableTowers = setOf(
                DefenderType.SPIKE_TOWER,
                DefenderType.BOW_TOWER,
                DefenderType.WIZARD_TOWER
            )
        )
        
        assertEquals(3, level.availableTowers.size, "Should have exactly 3 available towers")
    }
}
