package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.*
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test that upgrade preview calculations match actual tower stats
 */
class UpgradePreviewTest {
    
    @Test
    fun testSpikeTowerUpgradePreview() {
        // Test Spike Tower upgrade preview at various levels
        val spikeTower = Defender(
            id = 1,
            type = DefenderType.SPIKE_TOWER,
            position = mutableStateOf(Position(5, 5)),
            level = mutableStateOf(1)
        )
        
        // Level 1 -> 2: range should stay at 1
        assertEquals(1, spikeTower.range, "Level 1: range should be 1")
        spikeTower.level.value = 2
        assertEquals(1, spikeTower.range, "Level 2: range should be 1")
        
        // Level 2 -> 3: range should go to 2 (at max)
        spikeTower.level.value = 3
        assertEquals(2, spikeTower.range, "Level 3: range should be 2 (at max)")
        
        // Level 4 -> 5: range should stay at 2 (capped)
        spikeTower.level.value = 4
        assertEquals(2, spikeTower.range, "Level 4: range should be 2")
        spikeTower.level.value = 5
        assertEquals(2, spikeTower.range, "Level 5: range should be 2 (capped)")
    }
    
    @Test
    fun testSpearTowerUpgradePreview() {
        val spearTower = Defender(
            id = 1,
            type = DefenderType.SPEAR_TOWER,
            position = mutableStateOf(Position(5, 5)),
            level = mutableStateOf(1)
        )
        
        // Level 6 -> 7: range should stay at 5 (hitting cap)
        spearTower.level.value = 6
        assertEquals(4, spearTower.range, "Level 6: range should be 4")
        spearTower.level.value = 7
        assertEquals(5, spearTower.range, "Level 7: range should be 5 (at max)")
        
        // Level 7 -> 8: range should stay at 5 (capped)
        spearTower.level.value = 8
        assertEquals(5, spearTower.range, "Level 8: range should be 5 (capped)")
    }
    
    @Test
    fun testBowTowerUpgradePreview() {
        val bowTower = Defender(
            id = 1,
            type = DefenderType.BOW_TOWER,
            position = mutableStateOf(Position(5, 5)),
            level = mutableStateOf(34)
        )
        
        // Level 34 -> 35: range should hit cap at 20
        assertEquals(19, bowTower.range, "Level 34: range should be 19")
        bowTower.level.value = 35
        assertEquals(20, bowTower.range, "Level 35: range should be 20 (at max)")
        
        // Level 35 -> 36: range should stay at 20 (capped)
        bowTower.level.value = 36
        assertEquals(20, bowTower.range, "Level 36: range should be 20 (capped)")
    }
    
    @Test
    fun testBallistaTowerUpgradePreview() {
        val ballistaTower = Defender(
            id = 1,
            type = DefenderType.BALLISTA_TOWER,
            position = mutableStateOf(Position(5, 5)),
            level = mutableStateOf(19)
        )
        
        // Ballista has no max range, should keep growing
        assertEquals(14, ballistaTower.range, "Level 19: range should be 14")
        ballistaTower.level.value = 20
        assertEquals(14, ballistaTower.range, "Level 20: range should be 14")
        ballistaTower.level.value = 21
        assertEquals(15, ballistaTower.range, "Level 21: range should be 15 (unlimited)")
    }
    
    @Test
    fun testDwarvenMineUpgradePreview() {
        val mine = Defender(
            id = 1,
            type = DefenderType.DWARVEN_MINE,
            position = mutableStateOf(Position(5, 5)),
            level = mutableStateOf(34)
        )
        
        // Dwarven mine has special growth, should cap at 10
        assertEquals(9, mine.range, "Level 34: range should be 9")
        mine.level.value = 35
        assertEquals(10, mine.range, "Level 35: range should be 10 (at max)")
        mine.level.value = 36
        assertEquals(10, mine.range, "Level 36: range should be 10 (capped)")
    }
}
