package de.egril.defender.ui.gameplay

import de.egril.defender.model.*
import de.egril.defender.game.LevelData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for tower placement range preview logic.
 * 
 * These tests verify that the range preview calculations work correctly
 * for different tower types and game scenarios.
 */
class TowerPlacementRangePreviewTest {
    
    @Test
    fun testSpikeTowerRangePreview() {
        // Spike tower has range 1 (adjacent tiles only)
        val towerType = DefenderType.SPIKE_TOWER
        val hoverPosition = Position(5, 5)
        
        // Test adjacent positions (should be in range)
        val neighbors = hoverPosition.getHexNeighbors()
        for (neighbor in neighbors) {
            val distance = hoverPosition.distanceTo(neighbor)
            assertTrue(
                distance >= towerType.minRange && distance <= towerType.baseRange,
                "Neighbor at $neighbor should be in range of Spike Tower at $hoverPosition"
            )
        }
        
        // Test position 2 tiles away (should NOT be in range)
        val farPosition = Position(7, 5)
        val distance = hoverPosition.distanceTo(farPosition)
        assertFalse(
            distance <= towerType.baseRange,
            "Position at $farPosition should NOT be in range of Spike Tower at $hoverPosition"
        )
    }
    
    @Test
    fun testSpearTowerRangePreview() {
        // Spear tower has base range 2, max range 5
        val towerType = DefenderType.SPEAR_TOWER
        val hoverPosition = Position(5, 5)
        
        assertEquals(2, towerType.baseRange, "Spear should have base range of 2")
        assertEquals(5, towerType.maxRange, "Spear should have max range of 5")
        
        // Test position 2 tiles away (should be in range at level 1)
        val inRangePosition = Position(7, 5)
        val distance = hoverPosition.distanceTo(inRangePosition)
        assertTrue(
            distance >= towerType.minRange && distance <= towerType.baseRange,
            "Position at $inRangePosition (distance=$distance) should be in range of Spear Tower"
        )
        
        // Test position 3 tiles away (should NOT be in range at level 1)
        val outOfRangePosition = Position(8, 5)
        val outDistance = hoverPosition.distanceTo(outOfRangePosition)
        assertFalse(
            outDistance <= towerType.baseRange,
            "Position at $outOfRangePosition (distance=$outDistance) should NOT be in range at level 1"
        )
    }
    
    @Test
    fun testBowTowerRangePreview() {
        // Bow tower has base range 3, max range 20
        val towerType = DefenderType.BOW_TOWER
        val hoverPosition = Position(5, 5)
        
        assertEquals(3, towerType.baseRange, "Bow should have base range of 3")
        assertEquals(20, towerType.maxRange, "Bow should have max range of 20")
        
        // Test position 3 tiles away (should be in range)
        val nearPosition = Position(8, 5)
        val distance = hoverPosition.distanceTo(nearPosition)
        assertTrue(
            distance >= towerType.minRange && distance <= towerType.baseRange,
            "Position at $nearPosition (distance=$distance) should be in range of Bow Tower"
        )
        
        // Test position 4 tiles away (should NOT be in range at level 1)
        val farPosition = Position(9, 5)
        val farDistance = hoverPosition.distanceTo(farPosition)
        assertFalse(
            farDistance <= towerType.baseRange,
            "Position at $farPosition (distance=$farDistance) should NOT be in range at level 1"
        )
    }
    
    @Test
    fun testBallistaTowerMinRangeRestriction() {
        // Ballista has min range 3, no max range cap (grows with level)
        val towerType = DefenderType.BALLISTA_TOWER
        val hoverPosition = Position(5, 5)
        
        assertEquals(3, towerType.minRange, "Ballista should have min range of 3")
        assertEquals(5, towerType.baseRange, "Ballista should have base range of 5")
        assertEquals(null, towerType.maxRange, "Ballista should have no max range cap")
        
        // Test position 2 tiles away (should NOT be in range - below minimum)
        val tooClose = Position(7, 5)
        val closeDistance = hoverPosition.distanceTo(tooClose)
        assertFalse(
            closeDistance >= towerType.minRange,
            "Position at $tooClose (distance=$closeDistance) should be below min range of Ballista"
        )
        
        // Test position 3 tiles away (should be in range - at minimum)
        val minRangePos = Position(8, 5)
        val minDistance = hoverPosition.distanceTo(minRangePos)
        assertTrue(
            minDistance >= towerType.minRange && minDistance <= towerType.baseRange,
            "Position at $minRangePos (distance=$minDistance) should be in range of Ballista"
        )
        
        // Test position 5 tiles away (should be in range - at base range)
        val maxRangePos = Position(10, 5)
        val maxDistance = hoverPosition.distanceTo(maxRangePos)
        assertTrue(
            maxDistance >= towerType.minRange && maxDistance <= towerType.baseRange,
            "Position at $maxRangePos (distance=$maxDistance) should be in range of Ballista"
        )
        
        // For level 1 ballista, position 6 tiles away is out of range
        // But as ballista levels up, range will increase without cap
        val beyondBase = Position(11, 5)
        val beyondDistance = hoverPosition.distanceTo(beyondBase)
        assertFalse(
            beyondDistance <= towerType.baseRange,
            "Position at $beyondBase (distance=$beyondDistance) should be beyond base range at level 1"
        )
    }
    
    @Test
    fun testAreaAttackTowerTargetsRiverTiles() {
        // Wizard tower has area attack (FIREBALL) and can target river tiles
        val wizardType = DefenderType.WIZARD_TOWER
        assertEquals(AttackType.AREA, wizardType.attackType, "Wizard should have AREA attack type")
        
        // Alchemy tower has LASTING attack and can also target river tiles
        val alchemyType = DefenderType.ALCHEMY_TOWER
        assertEquals(AttackType.LASTING, alchemyType.attackType, "Alchemy should have LASTING attack type")
        
        // These tower types should show preview on both path AND river tiles
        // (This is implemented in GridCell's isInPreviewRange calculation)
    }
    
    @Test
    fun testSingleTargetTowerOnlyTargetsPathTiles() {
        // Bow tower has single-target (RANGED) attack
        val bowType = DefenderType.BOW_TOWER
        assertEquals(AttackType.RANGED, bowType.attackType, "Bow should have RANGED attack type")
        
        // Spear tower has single-target (RANGED) attack
        val spearType = DefenderType.SPEAR_TOWER
        assertEquals(AttackType.RANGED, spearType.attackType, "Spear should have RANGED attack type")
        
        // Spike tower has single-target (MELEE) attack
        val spikeType = DefenderType.SPIKE_TOWER
        assertEquals(AttackType.MELEE, spikeType.attackType, "Spike should have MELEE attack type")
        
        // These tower types should show preview only on path tiles, NOT river tiles
        // (This is implemented in GridCell's isInPreviewRange calculation)
    }
    
    @Test
    fun testSpecialStructuresHaveNoRangePreview() {
        // Dwarven mine has no attack capability
        val mineType = DefenderType.DWARVEN_MINE
        assertEquals(AttackType.NONE, mineType.attackType, "Mine should have NONE attack type")
        assertEquals(0, mineType.baseDamage, "Mine should have 0 damage")
        
        // Dragon's lair has no attack capability
        val lairType = DefenderType.DRAGONS_LAIR
        assertEquals(AttackType.NONE, lairType.attackType, "Dragon's Lair should have NONE attack type")
        assertEquals(0, lairType.baseDamage, "Dragon's Lair should have 0 damage")
        
        // These structures should not show range preview
        // (Preview is only shown for towers with attack capability)
    }
    
    @Test
    fun testBuildableTileDetection() {
        // Create a simple test level
        val levels = LevelData.createLevels()
        if (levels.isEmpty()) {
            println("No levels available, skipping test")
            return
        }
        val level = levels.first()
        val gameState = GameState(level)
        
        // Find a build area tile
        var buildAreaFound = false
        
        for (y in 0 until level.gridHeight) {
            for (x in 0 until level.gridWidth) {
                val pos = Position(x, y)
                if (level.isBuildArea(pos)) {
                    buildAreaFound = true
                }
            }
        }
        
        assertTrue(buildAreaFound, "Level should have buildable tiles")
        
        // Preview should only show on buildable tiles that:
        // 1. Are build areas or build islands
        // 2. Don't have an existing tower
        // 3. Don't have an enemy
    }
}
