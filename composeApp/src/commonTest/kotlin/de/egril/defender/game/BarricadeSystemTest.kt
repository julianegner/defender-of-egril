package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for barricade building mechanics.
 * 
 * Requirements:
 * - Spike Tower: Can build barricades at level 20+, HP = (level - 20) / 2 (minimum 1)
 * - Spear Tower: Can build barricades at level 10+, HP = level - 10 (minimum 1)
 */
class BarricadeSystemTest {
    
    // Helper function to create a minimal test level
    private fun createTestLevel(): Level {
        val pathCells = setOf(
            Position(0, 0), Position(1, 0), Position(2, 0), Position(3, 0), 
            Position(4, 0), Position(5, 0), Position(6, 0)
        )
        val buildIslands = setOf(Position(2, 2), Position(2, 3), Position(3, 2), Position(3, 3))
        
        return Level(
            id = 1,
            name = "Test Level",
            subtitle = "Test",
            gridWidth = 10,
            gridHeight = 10,
            startPositions = listOf(Position(0, 0)),
            targetPositions = listOf(Position(6, 0)),
            pathCells = pathCells,
            buildIslands = buildIslands,
            attackerWaves = emptyList(),
            initialCoins = 1000,
            healthPoints = 10,
            availableTowers = setOf(DefenderType.SPIKE_TOWER, DefenderType.SPEAR_TOWER)
        )
    }
    
    @Test
    fun testSpikeTowerCannotBuildBarricadeBeforeLevel20() {
        val level = createTestLevel()
        val gameState = GameState(level)
        val barricadeSystem = BarricadeSystem(gameState)
        
        // Test levels 1-19: should NOT be able to build barricades
        for (lvl in 1..19) {
            val tower = Defender(
                id = lvl,
                type = DefenderType.SPIKE_TOWER,
                position = mutableStateOf(Position(3, 0)),
                level = mutableStateOf(lvl)
            )
            assertFalse(
                barricadeSystem.canBuildBarricade(tower),
                "Spike tower level $lvl should NOT be able to build barricades"
            )
        }
    }
    
    @Test
    fun testSpikeTowerCanBuildBarricadeAtLevel20AndAbove() {
        val level = createTestLevel()
        val gameState = GameState(level)
        val barricadeSystem = BarricadeSystem(gameState)
        
        // Test levels 20, 21, 25, 30, etc: should be able to build barricades
        val testLevels = listOf(20, 21, 22, 25, 30, 40, 50)
        for (lvl in testLevels) {
            val tower = Defender(
                id = lvl,
                type = DefenderType.SPIKE_TOWER,
                position = mutableStateOf(Position(3, 0)),
                level = mutableStateOf(lvl)
            )
            assertTrue(
                barricadeSystem.canBuildBarricade(tower),
                "Spike tower level $lvl should be able to build barricades"
            )
        }
    }
    
    @Test
    fun testSpikeTowerBarricadeHPCalculation() {
        val level = createTestLevel()
        val gameState = GameState(level)
        val barricadeSystem = BarricadeSystem(gameState)
        
        // Level 20: (20 - 20) / 2 = 0, but minimum is 1
        val tower20 = Defender(
            id = 1,
            type = DefenderType.SPIKE_TOWER,
            position = mutableStateOf(Position(3, 0)),
            level = mutableStateOf(20)
        )
        assertEquals(1, barricadeSystem.calculateBarricadeHP(tower20), 
            "Level 20 spike tower barricade should have 1 HP (minimum)")
        
        // Level 21: (21 - 20) / 2 = 1 / 2 = 0 (integer division), but minimum is 1
        val tower21 = Defender(
            id = 2,
            type = DefenderType.SPIKE_TOWER,
            position = mutableStateOf(Position(3, 0)),
            level = mutableStateOf(21)
        )
        assertEquals(1, barricadeSystem.calculateBarricadeHP(tower21), 
            "Level 21 spike tower barricade should have 1 HP")
        
        // Level 22: (22 - 20) / 2 = 1
        val tower22 = Defender(
            id = 3,
            type = DefenderType.SPIKE_TOWER,
            position = mutableStateOf(Position(3, 0)),
            level = mutableStateOf(22)
        )
        assertEquals(1, barricadeSystem.calculateBarricadeHP(tower22), 
            "Level 22 spike tower barricade should have 1 HP")
        
        // Level 24: (24 - 20) / 2 = 2
        val tower24 = Defender(
            id = 4,
            type = DefenderType.SPIKE_TOWER,
            position = mutableStateOf(Position(3, 0)),
            level = mutableStateOf(24)
        )
        assertEquals(2, barricadeSystem.calculateBarricadeHP(tower24), 
            "Level 24 spike tower barricade should have 2 HP")
        
        // Level 30: (30 - 20) / 2 = 5
        val tower30 = Defender(
            id = 5,
            type = DefenderType.SPIKE_TOWER,
            position = mutableStateOf(Position(3, 0)),
            level = mutableStateOf(30)
        )
        assertEquals(5, barricadeSystem.calculateBarricadeHP(tower30), 
            "Level 30 spike tower barricade should have 5 HP")
        
        // Level 40: (40 - 20) / 2 = 10
        val tower40 = Defender(
            id = 6,
            type = DefenderType.SPIKE_TOWER,
            position = mutableStateOf(Position(3, 0)),
            level = mutableStateOf(40)
        )
        assertEquals(10, barricadeSystem.calculateBarricadeHP(tower40), 
            "Level 40 spike tower barricade should have 10 HP")
    }
    
    @Test
    fun testSpearTowerBarricadeBuildingUnchanged() {
        val level = createTestLevel()
        val gameState = GameState(level)
        val barricadeSystem = BarricadeSystem(gameState)
        
        // Spear tower should still be able to build at level 10+
        for (lvl in 1..9) {
            val tower = Defender(
                id = lvl,
                type = DefenderType.SPEAR_TOWER,
                position = mutableStateOf(Position(3, 0)),
                level = mutableStateOf(lvl)
            )
            assertFalse(
                barricadeSystem.canBuildBarricade(tower),
                "Spear tower level $lvl should NOT be able to build barricades"
            )
        }
        
        for (lvl in 10..20) {
            val tower = Defender(
                id = lvl,
                type = DefenderType.SPEAR_TOWER,
                position = mutableStateOf(Position(3, 0)),
                level = mutableStateOf(lvl)
            )
            assertTrue(
                barricadeSystem.canBuildBarricade(tower),
                "Spear tower level $lvl should be able to build barricades"
            )
        }
    }
    
    @Test
    fun testSpearTowerBarricadeHPCalculationUnchanged() {
        val level = createTestLevel()
        val gameState = GameState(level)
        val barricadeSystem = BarricadeSystem(gameState)
        
        // Spear tower HP should still be: level - 10 (minimum 1)
        
        // Level 10: 10 - 10 = 0, but minimum is 1
        val tower10 = Defender(
            id = 1,
            type = DefenderType.SPEAR_TOWER,
            position = mutableStateOf(Position(3, 0)),
            level = mutableStateOf(10)
        )
        assertEquals(1, barricadeSystem.calculateBarricadeHP(tower10), 
            "Level 10 spear tower barricade should have 1 HP")
        
        // Level 11: 11 - 10 = 1
        val tower11 = Defender(
            id = 2,
            type = DefenderType.SPEAR_TOWER,
            position = mutableStateOf(Position(3, 0)),
            level = mutableStateOf(11)
        )
        assertEquals(1, barricadeSystem.calculateBarricadeHP(tower11), 
            "Level 11 spear tower barricade should have 1 HP")
        
        // Level 15: 15 - 10 = 5
        val tower15 = Defender(
            id = 3,
            type = DefenderType.SPEAR_TOWER,
            position = mutableStateOf(Position(3, 0)),
            level = mutableStateOf(15)
        )
        assertEquals(5, barricadeSystem.calculateBarricadeHP(tower15), 
            "Level 15 spear tower barricade should have 5 HP")
        
        // Level 20: 20 - 10 = 10
        val tower20 = Defender(
            id = 4,
            type = DefenderType.SPEAR_TOWER,
            position = mutableStateOf(Position(3, 0)),
            level = mutableStateOf(20)
        )
        assertEquals(10, barricadeSystem.calculateBarricadeHP(tower20), 
            "Level 20 spear tower barricade should have 10 HP")
    }
    
    @Test
    fun testOtherTowersCannotBuildBarricades() {
        val level = createTestLevel()
        val gameState = GameState(level)
        val barricadeSystem = BarricadeSystem(gameState)
        
        // Test that other tower types cannot build barricades at any level
        val otherTowerTypes = listOf(
            DefenderType.BOW_TOWER,
            DefenderType.WIZARD_TOWER,
            DefenderType.ALCHEMY_TOWER,
            DefenderType.BALLISTA_TOWER,
            DefenderType.DWARVEN_MINE,
            DefenderType.DRAGONS_LAIR
        )
        
        for (towerType in otherTowerTypes) {
            for (lvl in listOf(1, 10, 20, 30)) {
                val tower = Defender(
                    id = 1,
                    type = towerType,
                    position = mutableStateOf(Position(3, 0)),
                    level = mutableStateOf(lvl)
                )
                assertFalse(
                    barricadeSystem.canBuildBarricade(tower),
                    "$towerType level $lvl should NOT be able to build barricades"
                )
            }
        }
    }
    
    @Test
    fun testBarricadeBuildingIntegration() {
        // Integration test: actually build a barricade and verify HP
        val level = createTestLevel()
        val gameState = GameState(level)
        val barricadeSystem = BarricadeSystem(gameState)
        
        // Create a level 22 spike tower (should give 1 HP barricade)
        val tower = Defender(
            id = 1,
            type = DefenderType.SPIKE_TOWER,
            position = mutableStateOf(Position(3, 0)),
            level = mutableStateOf(22)
        )
        tower.buildTimeRemaining.value = 0  // Mark as ready
        tower.actionsRemaining.value = 1   // Give it an action
        
        gameState.defenders.add(tower)
        
        // Build barricade on path
        val barricadePos = Position(4, 0)
        val success = barricadeSystem.performBuildBarricade(tower.id, barricadePos)
        
        assertTrue(success, "Barricade building should succeed")
        assertEquals(1, gameState.barricades.size, "Should have 1 barricade")
        
        val barricade = gameState.barricades.first()
        assertEquals(barricadePos, barricade.position, "Barricade should be at correct position")
        assertEquals(1, barricade.healthPoints.value, "Barricade should have 1 HP")
    }
    
    @Test
    fun testBarricadeTutorialTriggersAtCorrectLevel() {
        val level = createTestLevel()
        val gameState = GameState(level)
        val towerManager = TowerManager(gameState)
        
        // Test Spike Tower: Tutorial should trigger when upgrading from level 19 to 20
        val spikeTower = Defender(
            id = 1,
            type = DefenderType.SPIKE_TOWER,
            position = mutableStateOf(Position(2, 2)),
            level = mutableStateOf(19)
        )
        spikeTower.buildTimeRemaining.value = 0
        gameState.defenders.add(spikeTower)
        gameState.coins.value = 1000
        
        // Upgrade to level 20 - should trigger tutorial
        towerManager.upgradeDefender(spikeTower.id)
        assertEquals(20, spikeTower.level.value, "Spike tower should be level 20")
        assertTrue(spikeTower.hasShownBarricadeTutorial.value, "Barricade tutorial should be shown for spike tower at level 20")
        
        // Test Spear Tower: Tutorial should trigger when upgrading from level 9 to 10
        val spearTower = Defender(
            id = 2,
            type = DefenderType.SPEAR_TOWER,
            position = mutableStateOf(Position(3, 2)),
            level = mutableStateOf(9)
        )
        spearTower.buildTimeRemaining.value = 0
        gameState.defenders.add(spearTower)
        gameState.coins.value = 1000
        
        // Upgrade to level 10 - should trigger tutorial
        towerManager.upgradeDefender(spearTower.id)
        assertEquals(10, spearTower.level.value, "Spear tower should be level 10")
        assertTrue(spearTower.hasShownBarricadeTutorial.value, "Barricade tutorial should be shown for spear tower at level 10")
    }
}
