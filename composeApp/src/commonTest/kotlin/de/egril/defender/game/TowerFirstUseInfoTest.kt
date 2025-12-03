package de.egril.defender.game

import de.egril.defender.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Integration test for tower first-use info messages
 */
class TowerFirstUseInfoTest {
    
    @Test
    fun testWizardFirstUseInfo() {
        val gameState = createTestGameState()
        val towerManager = TowerManager(gameState)
        
        // Initially no info should be shown
        assertEquals(InfoType.NONE, gameState.infoState.value.currentInfo, "No info initially")
        
        // Place a wizard tower for the first time
        val placed = towerManager.placeDefender(DefenderType.WIZARD_TOWER, Position(0, 1))
        assertTrue(placed, "Should place wizard tower")
        
        // Verify wizard first use info is shown
        assertEquals(InfoType.WIZARD_FIRST_USE, gameState.infoState.value.currentInfo, "Should show wizard first use info")
        assertTrue(gameState.infoState.value.shouldShowOverlay(), "Should show overlay")
        
        // Dismiss the info
        gameState.infoState.value = gameState.infoState.value.dismissInfo()
        
        // Place another wizard tower - should not show info again
        val placedSecond = towerManager.placeDefender(DefenderType.WIZARD_TOWER, Position(1, 1))
        assertTrue(placedSecond, "Should place second wizard tower")
        assertEquals(InfoType.NONE, gameState.infoState.value.currentInfo, "Should not show info again")
        assertTrue(gameState.infoState.value.hasSeen(InfoType.WIZARD_FIRST_USE), "Should have seen wizard info")
    }
    
    @Test
    fun testAlchemyFirstUseInfo() {
        val gameState = createTestGameState()
        val towerManager = TowerManager(gameState)
        
        // Place an alchemy tower for the first time
        val placed = towerManager.placeDefender(DefenderType.ALCHEMY_TOWER, Position(0, 1))
        assertTrue(placed, "Should place alchemy tower")
        
        // Verify alchemy first use info is shown
        assertEquals(InfoType.ALCHEMY_FIRST_USE, gameState.infoState.value.currentInfo, "Should show alchemy first use info")
    }
    
    @Test
    fun testBallistaFirstUseInfo() {
        val gameState = createTestGameState()
        val towerManager = TowerManager(gameState)
        
        // Place a ballista tower for the first time
        val placed = towerManager.placeDefender(DefenderType.BALLISTA_TOWER, Position(0, 1))
        assertTrue(placed, "Should place ballista tower")
        
        // Verify ballista first use info is shown
        assertEquals(InfoType.BALLISTA_FIRST_USE, gameState.infoState.value.currentInfo, "Should show ballista first use info")
    }
    
    @Test
    fun testMineFirstUseInfo() {
        val gameState = createTestGameState()
        val towerManager = TowerManager(gameState)
        
        // Place a dwarven mine for the first time
        val placed = towerManager.placeDefender(DefenderType.DWARVEN_MINE, Position(0, 1))
        assertTrue(placed, "Should place dwarven mine")
        
        // Verify mine first use info is shown
        assertEquals(InfoType.MINE_FIRST_USE, gameState.infoState.value.currentInfo, "Should show mine first use info")
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
    fun testMultipleTowerFirstUseInfos() {
        val gameState = createTestGameState()
        val towerManager = TowerManager(gameState)
        
        // Place wizard, dismiss, then place alchemy
        towerManager.placeDefender(DefenderType.WIZARD_TOWER, Position(0, 1))
        assertEquals(InfoType.WIZARD_FIRST_USE, gameState.infoState.value.currentInfo, "Should show wizard info")
        gameState.infoState.value = gameState.infoState.value.dismissInfo()
        
        towerManager.placeDefender(DefenderType.ALCHEMY_TOWER, Position(1, 1))
        assertEquals(InfoType.ALCHEMY_FIRST_USE, gameState.infoState.value.currentInfo, "Should show alchemy info")
        gameState.infoState.value = gameState.infoState.value.dismissInfo()
        
        towerManager.placeDefender(DefenderType.BALLISTA_TOWER, Position(2, 1))
        assertEquals(InfoType.BALLISTA_FIRST_USE, gameState.infoState.value.currentInfo, "Should show ballista info")
        gameState.infoState.value = gameState.infoState.value.dismissInfo()
        
        // Verify all were seen
        assertTrue(gameState.infoState.value.hasSeen(InfoType.WIZARD_FIRST_USE), "Should have seen wizard")
        assertTrue(gameState.infoState.value.hasSeen(InfoType.ALCHEMY_FIRST_USE), "Should have seen alchemy")
        assertTrue(gameState.infoState.value.hasSeen(InfoType.BALLISTA_FIRST_USE), "Should have seen ballista")
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
            buildIslands = setOf(
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
