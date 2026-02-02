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
    fun testSpikeTowerMaxRange() {
        // Spike tower has base range 1, max range 2
        // Range formula: baseRange + (level - 1) / 2
        // Level 1: 1 + (1-1)/2 = 1 + 0 = 1
        // Level 3: 1 + (3-1)/2 = 1 + 1 = 2 (at max)
        // Level 5: 1 + (5-1)/2 = 1 + 2 = 3 (capped at maxRange 2)
        val spikeTower = Defender(
            id = 1,
            type = DefenderType.SPIKE_TOWER,
            position = mutableStateOf(Position(5, 5)),
            level = mutableStateOf(1)
        )
        
        assertEquals(2, spikeTower.type.maxRange, "Spike tower should have max range of 2")
        assertEquals(1, spikeTower.range, "Level 1 spike tower should have range 1")
        
        spikeTower.level.value = 3
        assertEquals(2, spikeTower.range, "Level 3 spike tower should have range 2 (at max)")
        
        spikeTower.level.value = 5
        assertEquals(2, spikeTower.range, "Level 5 spike tower should have range 2 (capped)")
        
        spikeTower.level.value = 10
        assertEquals(2, spikeTower.range, "Level 10 spike tower should have range 2 (capped)")
    }
    
    @Test
    fun testDwarvenMineMaxRange() {
        // Dwarven mine has special growth: 3 base + 1 every 5 levels, max 10
        // Level 1: 3 + (1/5) = 3 + 0 = 3
        // Level 5: 3 + (5/5) = 3 + 1 = 4
        // Level 10: 3 + (10/5) = 3 + 2 = 5
        // Level 35: 3 + (35/5) = 3 + 7 = 10 (at max)
        // Level 40: 3 + (40/5) = 3 + 8 = 11 (capped at maxRange 10)
        val mine = Defender(
            id = 1,
            type = DefenderType.DWARVEN_MINE,
            position = mutableStateOf(Position(5, 5)),
            level = mutableStateOf(1)
        )
        
        assertEquals(10, mine.type.maxRange, "Dwarven mine should have max range of 10")
        assertEquals(3, mine.range, "Level 1 mine should have range 3")
        
        mine.level.value = 5
        assertEquals(4, mine.range, "Level 5 mine should have range 4")
        
        mine.level.value = 10
        assertEquals(5, mine.range, "Level 10 mine should have range 5")
        
        mine.level.value = 35
        assertEquals(10, mine.range, "Level 35 mine should have range 10 (at max)")
        
        mine.level.value = 40
        assertEquals(10, mine.range, "Level 40 mine should have range 10 (capped)")
    }
    
    @Test
    fun testWizardTowerMaxRange() {
        // Wizard tower has base range 3, max range 15
        // Range formula: baseRange + (level - 1) / 2
        // Level 1: 3 + (1-1)/2 = 3 + 0 = 3
        // Level 10: 3 + (10-1)/2 = 3 + 4 = 7
        // Level 20: 3 + (20-1)/2 = 3 + 9 = 12
        // Level 25: 3 + (25-1)/2 = 3 + 12 = 15 (at max)
        // Level 30: 3 + (30-1)/2 = 3 + 14 = 17 (capped at maxRange 15)
        val wizardTower = Defender(
            id = 1,
            type = DefenderType.WIZARD_TOWER,
            position = mutableStateOf(Position(5, 5)),
            level = mutableStateOf(1)
        )
        
        assertEquals(15, wizardTower.type.maxRange, "Wizard tower should have max range of 15")
        assertEquals(3, wizardTower.range, "Level 1 wizard tower should have range 3")
        
        wizardTower.level.value = 10
        assertEquals(7, wizardTower.range, "Level 10 wizard tower should have range 7")
        
        wizardTower.level.value = 20
        assertEquals(12, wizardTower.range, "Level 20 wizard tower should have range 12")
        
        wizardTower.level.value = 25
        assertEquals(15, wizardTower.range, "Level 25 wizard tower should have range 15 (at max)")
        
        wizardTower.level.value = 30
        assertEquals(15, wizardTower.range, "Level 30 wizard tower should have range 15 (capped)")
    }
    
    @Test
    fun testAlchemyTowerMaxRange() {
        // Alchemy tower has base range 2, max range 10
        // Range formula: baseRange + (level - 1) / 2
        // Level 1: 2 + (1-1)/2 = 2 + 0 = 2
        // Level 5: 2 + (5-1)/2 = 2 + 2 = 4
        // Level 10: 2 + (10-1)/2 = 2 + 4 = 6
        // Level 17: 2 + (17-1)/2 = 2 + 8 = 10 (at max)
        // Level 20: 2 + (20-1)/2 = 2 + 9 = 11 (capped at maxRange 10)
        val alchemyTower = Defender(
            id = 1,
            type = DefenderType.ALCHEMY_TOWER,
            position = mutableStateOf(Position(5, 5)),
            level = mutableStateOf(1)
        )
        
        assertEquals(10, alchemyTower.type.maxRange, "Alchemy tower should have max range of 10")
        assertEquals(2, alchemyTower.range, "Level 1 alchemy tower should have range 2")
        
        alchemyTower.level.value = 5
        assertEquals(4, alchemyTower.range, "Level 5 alchemy tower should have range 4")
        
        alchemyTower.level.value = 10
        assertEquals(6, alchemyTower.range, "Level 10 alchemy tower should have range 6")
        
        alchemyTower.level.value = 17
        assertEquals(10, alchemyTower.range, "Level 17 alchemy tower should have range 10 (at max)")
        
        alchemyTower.level.value = 20
        assertEquals(10, alchemyTower.range, "Level 20 alchemy tower should have range 10 (capped)")
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
