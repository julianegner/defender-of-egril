package de.egril.defender.game

import de.egril.defender.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Integration test for tower placement - verifies that tower placement
 * does NOT trigger info messages (info messages are now shown when tower
 * becomes available, not when placed)
 */
class TowerFirstUseInfoTest {
    
    @Test
    fun testWizardPlacementNoInfo() {
        val gameState = createTestGameState()
        val towerManager = TowerManager(gameState)
        
        // Initially no info should be shown
        assertEquals(InfoType.NONE, gameState.infoState.value.currentInfo, "No info initially")
        
        // Place a wizard tower
        val placed = towerManager.placeDefender(DefenderType.WIZARD_TOWER, Position(0, 1))
        assertTrue(placed, "Should place wizard tower")
        
        // Verify NO info is shown (info is shown when tower becomes available, not when placed)
        assertEquals(InfoType.NONE, gameState.infoState.value.currentInfo, "Should not show info when placing tower")
        
        // Place another wizard tower - should still not show info
        val placedSecond = towerManager.placeDefender(DefenderType.WIZARD_TOWER, Position(1, 1))
        assertTrue(placedSecond, "Should place second wizard tower")
        assertEquals(InfoType.NONE, gameState.infoState.value.currentInfo, "Should not show info when placing tower")
    }
    
    @Test
    fun testAlchemyPlacementNoInfo() {
        val gameState = createTestGameState()
        val towerManager = TowerManager(gameState)
        
        // Place an alchemy tower
        val placed = towerManager.placeDefender(DefenderType.ALCHEMY_TOWER, Position(0, 1))
        assertTrue(placed, "Should place alchemy tower")
        
        // Verify NO info is shown
        assertEquals(InfoType.NONE, gameState.infoState.value.currentInfo, "Should not show info when placing tower")
    }
    
    @Test
    fun testBallistaPlacementNoInfo() {
        val gameState = createTestGameState()
        val towerManager = TowerManager(gameState)
        
        // Place a ballista tower
        val placed = towerManager.placeDefender(DefenderType.BALLISTA_TOWER, Position(0, 1))
        assertTrue(placed, "Should place ballista tower")
        
        // Verify NO info is shown
        assertEquals(InfoType.NONE, gameState.infoState.value.currentInfo, "Should not show info when placing tower")
    }
    
    @Test
    fun testMinePlacementNoInfo() {
        val gameState = createTestGameState()
        val towerManager = TowerManager(gameState)
        
        // Place a dwarven mine
        val placed = towerManager.placeDefender(DefenderType.DWARVEN_MINE, Position(0, 1))
        assertTrue(placed, "Should place dwarven mine")
        
        // Verify NO info is shown
        assertEquals(InfoType.NONE, gameState.infoState.value.currentInfo, "Should not show info when placing tower")
    }
    
    @Test
    fun testBasicTowersNoInfo() {
        val gameState = createTestGameState()
        val towerManager = TowerManager(gameState)
        
        // Place basic towers - should not show any info
        val placedSpike = towerManager.placeDefender(DefenderType.SPIKE_TOWER, Position(0, 1))
        assertTrue(placedSpike, "Should place spike tower")
        assertEquals(InfoType.NONE, gameState.infoState.value.currentInfo, "Should not show info for spike tower")
        
        val placedSpear = towerManager.placeDefender(DefenderType.SPEAR_TOWER, Position(1, 1))
        assertTrue(placedSpear, "Should place spear tower")
        assertEquals(InfoType.NONE, gameState.infoState.value.currentInfo, "Should not show info for spear tower")
        
        val placedBow = towerManager.placeDefender(DefenderType.BOW_TOWER, Position(2, 1))
        assertTrue(placedBow, "Should place bow tower")
        assertEquals(InfoType.NONE, gameState.infoState.value.currentInfo, "Should not show info for bow tower")
    }
    
    @Test
    fun testMultipleTowerPlacementsNoInfo() {
        val gameState = createTestGameState()
        val towerManager = TowerManager(gameState)
        
        // Place multiple advanced towers - none should trigger info
        towerManager.placeDefender(DefenderType.WIZARD_TOWER, Position(0, 1))
        assertEquals(InfoType.NONE, gameState.infoState.value.currentInfo, "Should not show wizard info")
        
        towerManager.placeDefender(DefenderType.ALCHEMY_TOWER, Position(1, 1))
        assertEquals(InfoType.NONE, gameState.infoState.value.currentInfo, "Should not show alchemy info")
        
        towerManager.placeDefender(DefenderType.BALLISTA_TOWER, Position(2, 1))
        assertEquals(InfoType.NONE, gameState.infoState.value.currentInfo, "Should not show ballista info")
    }
    
    private fun createTestGameState(): GameState {
        // Create a simple level with build areas
        val level = Level(
            id = 1,
            name = "Test Level",
            gridWidth = 10,
            gridHeight = 6,
            startPositions = listOf(Position(0, 3)),
            targetPositions = listOf(Position(9, 3)),
            pathCells = (0..9).map { Position(it, 3) }.toSet(),
            buildAreas = setOf(
                Position(0, 1),
                Position(1, 1),
                Position(2, 1),
                Position(3, 1)
            ),
            attackerWaves = listOf(
                AttackerWave(listOf(AttackerType.GOBLIN))
            ),
            initialCoins = 1000,  // Enough for all towers
            healthPoints = 10,
            availableTowers = setOf(
                DefenderType.SPIKE_TOWER,
                DefenderType.SPEAR_TOWER,
                DefenderType.BOW_TOWER,
                DefenderType.WIZARD_TOWER,
                DefenderType.ALCHEMY_TOWER,
                DefenderType.BALLISTA_TOWER,
                DefenderType.DWARVEN_MINE
            )
        )
        
        return GameState(level = level)
    }
}
