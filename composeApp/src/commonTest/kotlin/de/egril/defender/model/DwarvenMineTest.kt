package de.egril.defender.model

import androidx.compose.runtime.mutableStateOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for Dwarven Mine mechanics
 */
class DwarvenMineTest {
    
    @Test
    fun testMineActionsPerLevel() {
        val position = Position(0, 0)
        
        // Level 1-4: 1 base action
        val mine1 = Defender(1, DefenderType.DWARVEN_MINE, mutableStateOf(position), mutableStateOf(1))
        assertEquals(1, mine1.actionsPerTurnCalculated, "Level 1 mine should have 1 action")
        
        val mine4 = Defender(4, DefenderType.DWARVEN_MINE, mutableStateOf(position), mutableStateOf(4))
        assertEquals(1, mine4.actionsPerTurnCalculated, "Level 4 mine should have 1 action")
        
        // Level 5: 2 actions (1 base + 1 bonus)
        val mine5 = Defender(5, DefenderType.DWARVEN_MINE, mutableStateOf(position), mutableStateOf(5))
        assertEquals(2, mine5.actionsPerTurnCalculated, "Level 5 mine should have 2 actions")
        
        // Level 10: 3 actions (1 base + 2 bonus)
        val mine10 = Defender(10, DefenderType.DWARVEN_MINE, mutableStateOf(position), mutableStateOf(10))
        assertEquals(3, mine10.actionsPerTurnCalculated, "Level 10 mine should have 3 actions")
    }
    
    @Test
    fun testMineReach() {
        val position = Position(0, 0)
        
        // Level 1: reach 3
        val mine1 = Defender(1, DefenderType.DWARVEN_MINE, mutableStateOf(position), mutableStateOf(1))
        assertEquals(3, mine1.range, "Level 1 mine should have reach 3")
        
        // Level 5: reach 4 (3 + 1)
        val mine5 = Defender(5, DefenderType.DWARVEN_MINE, mutableStateOf(position), mutableStateOf(5))
        assertEquals(4, mine5.range, "Level 5 mine should have reach 4")
        
        // Level 35: reach 10 (capped)
        val mine35 = Defender(35, DefenderType.DWARVEN_MINE, mutableStateOf(position), mutableStateOf(35))
        assertEquals(10, mine35.range, "Level 35 mine should have reach 10 (capped)")
    }
    
    @Test
    fun testTrapDamage() {
        val position = Position(0, 0)
        
        // Level 1: 10 damage
        val mine1 = Defender(1, DefenderType.DWARVEN_MINE, mutableStateOf(position), mutableStateOf(1))
        assertEquals(10, mine1.trapDamage, "Level 1 mine should have trap damage 10")
        
        // Level 2: 15 damage
        val mine2 = Defender(2, DefenderType.DWARVEN_MINE, mutableStateOf(position), mutableStateOf(2))
        assertEquals(15, mine2.trapDamage, "Level 2 mine should have trap damage 15")
        
        // Level 4: 20 damage
        val mine4 = Defender(4, DefenderType.DWARVEN_MINE, mutableStateOf(position), mutableStateOf(4))
        assertEquals(20, mine4.trapDamage, "Level 4 mine should have trap damage 20")
        
        // Level 10: 35 damage (10 + 5 * 5)
        val mine10 = Defender(10, DefenderType.DWARVEN_MINE, mutableStateOf(position), mutableStateOf(10))
        assertEquals(35, mine10.trapDamage, "Level 10 mine should have trap damage 35")
    }
    
    @Test
    fun testDigOutcomeProbabilities() {
        // Test that all probabilities add up to 100
        val totalProbability = DigOutcome.values().sumOf { it.probability }
        assertEquals(100, totalProbability, "All dig outcome probabilities should sum to 100")
    }
    
    @Test
    fun testDragonProperties() {
        val position = Position(5, 5)
        val dragon = Attacker(
            id = 1,
            type = AttackerType.DRAGON,
            position = mutableStateOf(position),
            level = mutableStateOf(1)
        )
        
        assertTrue(dragon.type.isDragon, "Dragon should have isDragon flag set")
        assertTrue(dragon.type.isBoss, "Dragon should be marked as boss")
        assertEquals(500, dragon.maxHealth, "Dragon base health should be 500")
    }
    
    @Test
    fun testDragonsLairCannotBeSold() {
        val position = Position(3, 3)
        val lair = Defender(1, DefenderType.DRAGONS_LAIR, mutableStateOf(position))
        
        assertEquals(false, lair.canSell, "Dragon's lair should not be sellable")
    }
}
