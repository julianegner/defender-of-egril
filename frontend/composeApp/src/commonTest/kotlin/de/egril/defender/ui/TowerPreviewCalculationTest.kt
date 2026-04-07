package de.egril.defender.ui

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.*
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for tower upgrade preview calculations in DefenderInfo.kt
 * These tests verify that the "next level" stats match what the actual defender would have after upgrade
 */
class TowerPreviewCalculationTest {
    
    @Test
    fun testDwarvenMinePreviewAtLevel141() {
        // Create a dwarven mine at level 141
        val mine = Defender(
            id = 1,
            type = DefenderType.DWARVEN_MINE,
            position = mutableStateOf(Position(5, 5)),
            level = mutableStateOf(141)
        )
        
        // Current stats
        assertEquals(360, mine.trapDamage, "Level 141 mine should have 360 trap damage")
        assertEquals(10, mine.range, "Level 141 mine should have range 10 (capped)")
        assertEquals(29, mine.actionsPerTurnCalculated, "Level 141 mine should have 29 actions")
        
        // Calculate next level stats (as done in DefenderInfo.kt)
        val nextLevel = mine.level.value + 1  // 142
        val baseDamage = mine.trapDamage  // 360
        val nextLevelDamage = baseDamage + 5  // 365
        
        val nextRangeCalculated = mine.type.baseRange + (nextLevel - 1) / 2
        val nextRange = if (mine.type == DefenderType.DWARVEN_MINE) {
            val mineReach = 3 + (nextLevel / 5)
            minOf(mineReach, 10)
        } else {
            nextRangeCalculated
        }
        
        val nextActions = if (mine.type == DefenderType.DWARVEN_MINE) {
            1 + (nextLevel / 5)
        } else {
            mine.type.actionsPerTurn
        }
        
        // Verify next level preview
        assertEquals(365, nextLevelDamage, "Next level (142) should show 365 trap damage")
        assertEquals(10, nextRange, "Next level (142) should show range 10 (still capped)")
        assertEquals(29, nextActions, "Next level (142) should show 29 actions")
        
        // Verify by actually upgrading
        mine.level.value = 142
        assertEquals(365, mine.trapDamage, "Upgraded mine should have 365 trap damage")
        assertEquals(10, mine.range, "Upgraded mine should have range 10")
        assertEquals(29, mine.actionsPerTurnCalculated, "Upgraded mine should have 29 actions")
    }
    
    @Test
    fun testSpikeTowerPreviewAtLevel10() {
        // Create a spike tower at level 10
        val tower = Defender(
            id = 1,
            type = DefenderType.SPIKE_TOWER,
            position = mutableStateOf(Position(5, 5)),
            level = mutableStateOf(10)
        )
        
        // Current stats
        assertEquals(50, tower.damage, "Level 10 spike tower should have 50 damage")
        assertEquals(2, tower.range, "Level 10 spike tower should have range 2 (capped)")
        assertEquals(3, tower.actionsPerTurnCalculated, "Level 10 spike tower should have 3 actions (capped)")
        
        // Calculate next level stats
        val nextLevel = tower.level.value + 1  // 11
        val nextLevelDamage = tower.damage + 5  // 55
        
        val nextRangeCalculated = tower.type.baseRange + (nextLevel - 1) / 2
        val nextRange = if (tower.type == DefenderType.SPIKE_TOWER && nextLevel >= 5) {
            minOf(nextRangeCalculated, 2)
        } else {
            nextRangeCalculated
        }
        
        val nextActions = if (tower.type == DefenderType.SPIKE_TOWER) {
            val bonusActions = nextLevel / 5
            minOf(tower.type.actionsPerTurn + bonusActions, 3)
        } else {
            tower.type.actionsPerTurn
        }
        
        // Verify next level preview
        assertEquals(55, nextLevelDamage, "Next level (11) should show 55 damage")
        assertEquals(2, nextRange, "Next level (11) should show range 2 (capped)")
        assertEquals(3, nextActions, "Next level (11) should show 3 actions (capped)")
        
        // Verify by actually upgrading
        tower.level.value = 11
        assertEquals(55, tower.damage, "Upgraded tower should have 55 damage")
        assertEquals(2, tower.range, "Upgraded tower should have range 2")
        assertEquals(3, tower.actionsPerTurnCalculated, "Upgraded tower should have 3 actions")
    }
    
    @Test
    fun testBowTowerPreview() {
        // Create a bow tower at level 5
        val tower = Defender(
            id = 1,
            type = DefenderType.BOW_TOWER,
            position = mutableStateOf(Position(5, 5)),
            level = mutableStateOf(5)
        )
        
        // Current stats
        assertEquals(30, tower.damage, "Level 5 bow tower should have 30 damage")
        assertEquals(5, tower.range, "Level 5 bow tower should have range 5")
        assertEquals(1, tower.actionsPerTurnCalculated, "Level 5 bow tower should have 1 action")
        
        // Calculate next level stats
        val nextLevel = tower.level.value + 1  // 6
        val nextLevelDamage = tower.damage + 5  // 35
        val nextRange = tower.type.baseRange + (nextLevel - 1) / 2  // 3 + 5/2 = 3 + 2 = 5
        val nextActions = tower.type.actionsPerTurn  // 1
        
        // Verify next level preview
        assertEquals(35, nextLevelDamage, "Next level (6) should show 35 damage")
        assertEquals(5, nextRange, "Next level (6) should show range 5")
        assertEquals(1, nextActions, "Next level (6) should show 1 action")
        
        // Verify by actually upgrading
        tower.level.value = 6
        assertEquals(35, tower.damage, "Upgraded tower should have 35 damage")
        assertEquals(5, tower.range, "Upgraded tower should have range 5")
        assertEquals(1, tower.actionsPerTurnCalculated, "Upgraded tower should have 1 action")
    }
    
    @Test
    fun testAlchemyTowerPreviewWithLastingDamage() {
        // Create an alchemy tower (LASTING attack type)
        val tower = Defender(
            id = 1,
            type = DefenderType.ALCHEMY_TOWER,
            position = mutableStateOf(Position(5, 5)),
            level = mutableStateOf(8)
        )
        
        // Current stats (LASTING does half damage)
        assertEquals(50, tower.damage, "Level 8 alchemy tower should have 50 base damage")
        assertEquals(25, tower.actualDamage, "Level 8 alchemy tower should have 25 actual damage (halved)")
        assertEquals(5, tower.range, "Level 8 alchemy tower should have range 5")
        
        // Calculate next level stats
        val nextLevel = tower.level.value + 1  // 9
        val baseDamage = tower.damage  // 50
        val nextLevelDamage = baseDamage + 5  // 55
        val nextActualDamage = when (tower.type.attackType) {
            AttackType.LASTING -> nextLevelDamage / 2  // 27
            else -> nextLevelDamage
        }
        
        // Verify next level preview
        assertEquals(55, nextLevelDamage, "Next level (9) should show 55 base damage")
        assertEquals(27, nextActualDamage, "Next level (9) should show 27 actual damage (halved)")
        
        // Verify by actually upgrading
        tower.level.value = 9
        assertEquals(55, tower.damage, "Upgraded tower should have 55 base damage")
        assertEquals(27, tower.actualDamage, "Upgraded tower should have 27 actual damage")
    }
    
    @Test
    fun testWizardTowerPreview() {
        // Test wizard tower with AREA attack
        val tower = Defender(
            id = 1,
            type = DefenderType.WIZARD_TOWER,
            position = mutableStateOf(Position(5, 5)),
            level = mutableStateOf(3)
        )
        
        // Current stats
        assertEquals(40, tower.damage, "Level 3 wizard tower should have 40 damage")
        assertEquals(4, tower.range, "Level 3 wizard tower should have range 4")
        assertEquals(1, tower.actionsPerTurnCalculated, "Level 3 wizard tower should have 1 action")
        
        // Calculate next level stats
        val nextLevel = tower.level.value + 1  // 4
        val nextLevelDamage = tower.damage + 5  // 45
        val nextRange = tower.type.baseRange + (nextLevel - 1) / 2  // 3 + 3/2 = 3 + 1 = 4
        
        // Verify next level preview
        assertEquals(45, nextLevelDamage, "Next level (4) should show 45 damage")
        assertEquals(4, nextRange, "Next level (4) should show range 4")
        
        // Verify by actually upgrading
        tower.level.value = 4
        assertEquals(45, tower.damage, "Upgraded tower should have 45 damage")
        assertEquals(4, tower.range, "Upgraded tower should have range 4")
    }
    
    @Test
    fun testBallistaTowerPreview() {
        // Test ballista with minimum range
        val tower = Defender(
            id = 1,
            type = DefenderType.BALLISTA_TOWER,
            position = mutableStateOf(Position(5, 5)),
            level = mutableStateOf(7)
        )
        
        // Current stats
        assertEquals(80, tower.damage, "Level 7 ballista should have 80 damage")
        assertEquals(8, tower.range, "Level 7 ballista should have range 8")
        assertEquals(3, tower.type.minRange, "Ballista should have min range 3")
        
        // Calculate next level stats
        val nextLevel = tower.level.value + 1  // 8
        val nextLevelDamage = tower.damage + 5  // 85
        val nextRange = tower.type.baseRange + (nextLevel - 1) / 2  // 5 + 7/2 = 5 + 3 = 8
        
        // Verify next level preview
        assertEquals(85, nextLevelDamage, "Next level (8) should show 85 damage")
        assertEquals(8, nextRange, "Next level (8) should show range 8")
        assertEquals(3, tower.type.minRange, "Ballista should still have min range 3")
        
        // Verify by actually upgrading
        tower.level.value = 8
        assertEquals(85, tower.damage, "Upgraded tower should have 85 damage")
        assertEquals(8, tower.range, "Upgraded tower should have range 8")
    }
    
    @Test
    fun testDwarvenMineEarlyLevelsRange() {
        // Test mine at early levels where range is below cap
        val mine = Defender(
            id = 1,
            type = DefenderType.DWARVEN_MINE,
            position = mutableStateOf(Position(5, 5)),
            level = mutableStateOf(5)
        )
        
        // Current stats: 3 + (5/5) = 3 + 1 = 4
        assertEquals(4, mine.range, "Level 5 mine should have range 4")
        
        // Calculate next level stats
        val nextLevel = mine.level.value + 1  // 6
        val nextRange = if (mine.type == DefenderType.DWARVEN_MINE) {
            val mineReach = 3 + (nextLevel / 5)
            minOf(mineReach, 10)
        } else {
            0
        }
        
        // Next level: 3 + (6/5) = 3 + 1 = 4
        assertEquals(4, nextRange, "Next level (6) should show range 4")
        
        // Verify by actually upgrading
        mine.level.value = 6
        assertEquals(4, mine.range, "Upgraded mine should have range 4")
    }
    
    @Test
    fun testSpikeTowerRangeCap() {
        // Test spike tower before and after range cap at level 5
        val tower = Defender(
            id = 1,
            type = DefenderType.SPIKE_TOWER,
            position = mutableStateOf(Position(5, 5)),
            level = mutableStateOf(4)
        )
        
        // Current stats: 1 + (4-1)/2 = 1 + 3/2 = 1 + 1 = 2
        assertEquals(2, tower.range, "Level 4 spike tower should have range 2")
        
        // Calculate next level stats (cap kicks in at level 5+)
        val nextLevel = tower.level.value + 1  // 5
        val nextRangeCalculated = tower.type.baseRange + (nextLevel - 1) / 2  // 1 + 4/2 = 1 + 2 = 3
        val nextRange = if (tower.type == DefenderType.SPIKE_TOWER && nextLevel >= 5) {
            minOf(nextRangeCalculated, 2)  // Capped at 2
        } else {
            nextRangeCalculated
        }
        
        assertEquals(2, nextRange, "Next level (5) should show range 2 (capped)")
        
        // Verify by actually upgrading
        tower.level.value = 5
        assertEquals(2, tower.range, "Upgraded tower should have range 2 (capped)")
    }
}
