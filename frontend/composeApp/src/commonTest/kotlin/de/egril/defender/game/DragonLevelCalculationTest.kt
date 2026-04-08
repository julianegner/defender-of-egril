package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.Attacker
import de.egril.defender.model.AttackerType
import de.egril.defender.model.Position
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test that dragon level calculation matches the specification:
 * - Dragon base health is 500 HP
 * - Level 1: 1-999 HP
 * - Level 2: 1000-1499 HP
 * - Level 3: 1500-1999 HP
 * - Level 4: 2000-2499 HP
 * - Level 5: 2500-2999 HP
 * - etc.
 */
class DragonLevelCalculationTest {
    
    @Test
    fun testDragonLevel1WithBaseHealth() {
        val dragon = Attacker(
            id = 1,
            type = AttackerType.DRAGON,
            position = mutableStateOf(Position(0, 0)),
            currentHealth = mutableStateOf(500)
        )
        dragon.updateDragonLevel()
        assertEquals(1, dragon.level.value, 
            "Dragon with 500 HP should be level 1")
    }
    
    @Test
    fun testDragonLevel1WithMaxHP() {
        val dragon = Attacker(
            id = 1,
            type = AttackerType.DRAGON,
            position = mutableStateOf(Position(0, 0)),
            currentHealth = mutableStateOf(999)
        )
        dragon.updateDragonLevel()
        assertEquals(1, dragon.level.value, 
            "Dragon with 999 HP should be level 1")
    }
    
    @Test
    fun testDragonLevel2WithMinHP() {
        val dragon = Attacker(
            id = 1,
            type = AttackerType.DRAGON,
            position = mutableStateOf(Position(0, 0)),
            currentHealth = mutableStateOf(1000)
        )
        dragon.updateDragonLevel()
        assertEquals(2, dragon.level.value, 
            "Dragon with 1000 HP should be level 2")
    }
    
    @Test
    fun testDragonLevel2WithMaxHP() {
        val dragon = Attacker(
            id = 1,
            type = AttackerType.DRAGON,
            position = mutableStateOf(Position(0, 0)),
            currentHealth = mutableStateOf(1499)
        )
        dragon.updateDragonLevel()
        assertEquals(2, dragon.level.value, 
            "Dragon with 1499 HP should be level 2")
    }
    
    @Test
    fun testDragonLevel3WithMinHP() {
        val dragon = Attacker(
            id = 1,
            type = AttackerType.DRAGON,
            position = mutableStateOf(Position(0, 0)),
            currentHealth = mutableStateOf(1500)
        )
        dragon.updateDragonLevel()
        assertEquals(3, dragon.level.value, 
            "Dragon with 1500 HP should be level 3")
    }
    
    @Test
    fun testDragonLevel3WithMaxHP() {
        val dragon = Attacker(
            id = 1,
            type = AttackerType.DRAGON,
            position = mutableStateOf(Position(0, 0)),
            currentHealth = mutableStateOf(1999)
        )
        dragon.updateDragonLevel()
        assertEquals(3, dragon.level.value, 
            "Dragon with 1999 HP should be level 3")
    }
    
    @Test
    fun testDragonLevel5() {
        val dragon = Attacker(
            id = 1,
            type = AttackerType.DRAGON,
            position = mutableStateOf(Position(0, 0)),
            currentHealth = mutableStateOf(2500)
        )
        dragon.updateDragonLevel()
        assertEquals(5, dragon.level.value, 
            "Dragon with 2500 HP should be level 5")
    }
    
    @Test
    fun testDragonLevel10() {
        val dragon = Attacker(
            id = 1,
            type = AttackerType.DRAGON,
            position = mutableStateOf(Position(0, 0)),
            currentHealth = mutableStateOf(5000)
        )
        dragon.updateDragonLevel()
        assertEquals(10, dragon.level.value, 
            "Dragon with 5000 HP should be level 10")
    }
    
    @Test
    fun testDragonLevelDamageMatchesLevel() {
        // Test that dragon damage matches its level
        val testCases = listOf(
            Pair(500, 1),   // 500 HP = level 1, 1 damage
            Pair(1000, 2),  // 1000 HP = level 2, 2 damage
            Pair(1500, 3),  // 1500 HP = level 3, 3 damage
            Pair(2000, 4),  // 2000 HP = level 4, 4 damage
            Pair(2500, 5),  // 2500 HP = level 5, 5 damage
            Pair(5000, 10)  // 5000 HP = level 10, 10 damage
        )
        
        for ((health, expectedLevel) in testCases) {
            val dragon = Attacker(
                id = 1,
                type = AttackerType.DRAGON,
                position = mutableStateOf(Position(0, 0)),
                currentHealth = mutableStateOf(health)
            )
            dragon.updateDragonLevel()
            
            assertEquals(expectedLevel, dragon.level.value, 
                "Dragon with $health HP should be level $expectedLevel")
            assertEquals(expectedLevel, dragon.calculateTargetDamage(), 
                "Dragon with $health HP (level $expectedLevel) should deal $expectedLevel HP damage")
        }
    }
    
    @Test
    fun testDragonLevelNeverBelowOne() {
        // Even with low health, dragon level should be at least 1
        val dragon = Attacker(
            id = 1,
            type = AttackerType.DRAGON,
            position = mutableStateOf(Position(0, 0)),
            currentHealth = mutableStateOf(1)
        )
        dragon.updateDragonLevel()
        assertEquals(1, dragon.level.value, 
            "Dragon with 1 HP should still be level 1 (minimum)")
    }
    
    @Test
    fun testDragonMaxHealthCalculation() {
        // Test that maxHealth property returns correct value
        val dragon = Attacker(
            id = 1,
            type = AttackerType.DRAGON,
            position = mutableStateOf(Position(0, 0)),
            level = mutableStateOf(5)
        )
        assertEquals(2500, dragon.maxHealth, 
            "Dragon at level 5 should have max health of 2500 (500 × 5)")
    }
}
