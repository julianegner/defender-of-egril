package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests for tower range mechanics with maxRange limits
 */
class TowerRangeTest {
    
    @Test
    fun testSpearTowerMaxRange() {
        // Spear tower has base range 2, max range 5
        // Range formula: baseRange + (level - 1) / 2
        // Level 1: 2 + (1-1)/2 = 2 + 0 = 2
        // Level 3: 2 + (3-1)/2 = 2 + 1 = 3
        // Level 5: 2 + (5-1)/2 = 2 + 2 = 4
        // Level 7: 2 + (7-1)/2 = 2 + 3 = 5 (capped at maxRange 5)
        // Level 9: 2 + (9-1)/2 = 2 + 4 = 6 (capped at maxRange 5)
        val spearTower = Defender(
            id = 1,
            type = DefenderType.SPEAR_TOWER,
            position = mutableStateOf(Position(5, 5)),
            level = mutableStateOf(1)
        )
        
        assertEquals(5, spearTower.type.maxRange, "Spear tower should have max range of 5")
        assertEquals(2, spearTower.range, "Level 1 spear tower should have range 2")
        
        spearTower.level.value = 3
        assertEquals(3, spearTower.range, "Level 3 spear tower should have range 3")
        
        spearTower.level.value = 5
        assertEquals(4, spearTower.range, "Level 5 spear tower should have range 4")
        
        spearTower.level.value = 7
        assertEquals(5, spearTower.range, "Level 7 spear tower should have range 5 (at max)")
        
        spearTower.level.value = 9
        assertEquals(5, spearTower.range, "Level 9 spear tower should have range 5 (capped at max)")
        
        spearTower.level.value = 20
        assertEquals(5, spearTower.range, "Level 20 spear tower should have range 5 (capped at max)")
    }
    
    @Test
    fun testBowTowerMaxRange() {
        // Bow tower has base range 3, max range 20
        // Range formula: baseRange + (level - 1) / 2
        // Level 1: 3 + (1-1)/2 = 3 + 0 = 3
        // Level 5: 3 + (5-1)/2 = 3 + 2 = 5
        // Level 10: 3 + (10-1)/2 = 3 + 4 = 7
        // Level 20: 3 + (20-1)/2 = 3 + 9 = 12
        // Level 35: 3 + (35-1)/2 = 3 + 17 = 20 (capped at maxRange 20)
        // Level 40: 3 + (40-1)/2 = 3 + 19 = 22 (capped at maxRange 20)
        val bowTower = Defender(
            id = 1,
            type = DefenderType.BOW_TOWER,
            position = mutableStateOf(Position(5, 5)),
            level = mutableStateOf(1)
        )
        
        assertEquals(20, bowTower.type.maxRange, "Bow tower should have max range of 20")
        assertEquals(3, bowTower.range, "Level 1 bow tower should have range 3")
        
        bowTower.level.value = 5
        assertEquals(5, bowTower.range, "Level 5 bow tower should have range 5")
        
        bowTower.level.value = 10
        assertEquals(7, bowTower.range, "Level 10 bow tower should have range 7")
        
        bowTower.level.value = 20
        assertEquals(12, bowTower.range, "Level 20 bow tower should have range 12")
        
        bowTower.level.value = 35
        assertEquals(20, bowTower.range, "Level 35 bow tower should have range 20 (at max)")
        
        bowTower.level.value = 40
        assertEquals(20, bowTower.range, "Level 40 bow tower should have range 20 (capped at max)")
    }
    
    @Test
    fun testBallistaTowerNoMaxRange() {
        // Ballista tower has no max range cap - grows indefinitely
        // Range formula: baseRange + (level - 1) / 2
        // Level 1: 5 + (1-1)/2 = 5 + 0 = 5
        // Level 5: 5 + (5-1)/2 = 5 + 2 = 7
        // Level 10: 5 + (10-1)/2 = 5 + 4 = 9
        // Level 20: 5 + (20-1)/2 = 5 + 9 = 14
        // Level 50: 5 + (50-1)/2 = 5 + 24 = 29
        val ballistaTower = Defender(
            id = 1,
            type = DefenderType.BALLISTA_TOWER,
            position = mutableStateOf(Position(5, 5)),
            level = mutableStateOf(1)
        )
        
        assertNull(ballistaTower.type.maxRange, "Ballista tower should have no max range cap")
        assertEquals(5, ballistaTower.range, "Level 1 ballista tower should have range 5")
        
        ballistaTower.level.value = 5
        assertEquals(7, ballistaTower.range, "Level 5 ballista tower should have range 7")
        
        ballistaTower.level.value = 10
        assertEquals(9, ballistaTower.range, "Level 10 ballista tower should have range 9")
        
        ballistaTower.level.value = 20
        assertEquals(14, ballistaTower.range, "Level 20 ballista tower should have range 14")
        
        ballistaTower.level.value = 50
        assertEquals(29, ballistaTower.range, "Level 50 ballista tower should have range 29 (no cap)")
    }
    
    @Test
    fun testOtherTowersNoMaxRange() {
        // Spike, Wizard, and Alchemy towers should not have maxRange caps
        assertEquals(null, DefenderType.SPIKE_TOWER.maxRange, "Spike tower should have no max range cap")
        assertEquals(null, DefenderType.WIZARD_TOWER.maxRange, "Wizard tower should have no max range cap")
        assertEquals(null, DefenderType.ALCHEMY_TOWER.maxRange, "Alchemy tower should have no max range cap")
        
        // Note: Spike tower has a special case max range of 2 at level 5+ (hardcoded)
        val spikeTower = Defender(
            id = 1,
            type = DefenderType.SPIKE_TOWER,
            position = mutableStateOf(Position(5, 5)),
            level = mutableStateOf(5)
        )
        assertEquals(2, spikeTower.range, "Level 5+ spike tower should have max range 2 (special case)")
    }
    
    @Test
    fun testCanAttackWithMaxRange() {
        // Test that canAttack respects maxRange
        val spearTower = Defender(
            id = 1,
            type = DefenderType.SPEAR_TOWER,
            position = mutableStateOf(Position(5, 5)),
            level = mutableStateOf(20),  // Level 20: would have range 11 without cap, but capped at 5
            buildTimeRemaining = mutableStateOf(0),
            actionsRemaining = mutableStateOf(1)
        )
        
        assertEquals(5, spearTower.range, "Level 20 spear tower should have range 5 (capped)")
        
        // Create an attacker at distance 5 (should be in range)
        val attacker1 = Attacker(
            id = 1,
            type = AttackerType.GOBLIN,
            position = mutableStateOf(Position(10, 5)),  // Distance 5
            level = mutableStateOf(1),
            currentHealth = mutableStateOf(20)
        )
        assertEquals(5, spearTower.position.value.distanceTo(attacker1.position.value))
        assertEquals(true, spearTower.canAttack(attacker1), "Should be able to attack at max range")
        
        // Create an attacker at distance 6 (should NOT be in range)
        val attacker2 = Attacker(
            id = 2,
            type = AttackerType.GOBLIN,
            position = mutableStateOf(Position(11, 5)),  // Distance 6
            level = mutableStateOf(1),
            currentHealth = mutableStateOf(20)
        )
        assertEquals(6, spearTower.position.value.distanceTo(attacker2.position.value))
        assertEquals(false, spearTower.canAttack(attacker2), "Should NOT be able to attack beyond max range")
    }
}
