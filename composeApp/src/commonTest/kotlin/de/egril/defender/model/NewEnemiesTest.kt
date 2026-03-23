package de.egril.defender.model

import androidx.compose.runtime.mutableStateOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for new enemy types and their special abilities
 */
class NewEnemiesTest {
    
    @Test
    fun testBlueDemonImmuneToAcid() {
        val blueDemon = Attacker(
            id = 1,
            type = AttackerType.BLUE_DEMON,
            position = mutableStateOf(Position(0, 0)),
            level = mutableStateOf(1)
        )
        
        assertTrue(blueDemon.canBeDamagedByFireball(), "Blue Demon should be damaged by fireballs")
        assertFalse(blueDemon.canBeDamagedByAcid(), "Blue Demon should be immune to acid")
    }
    
    @Test
    fun testRedDemonImmuneToFireball() {
        val redDemon = Attacker(
            id = 1,
            type = AttackerType.RED_DEMON,
            position = mutableStateOf(Position(0, 0)),
            level = mutableStateOf(1)
        )
        
        assertTrue(redDemon.canBeDamagedByAcid(), "Red Demon should be damaged by acid")
        assertFalse(redDemon.canBeDamagedByFireball(), "Red Demon should be immune to fireballs")
    }
    
    @Test
    fun testEnemyLevelAffectsHealth() {
        val goblin1 = Attacker(
            id = 1,
            type = AttackerType.GOBLIN,
            position = mutableStateOf(Position(0, 0)),
            level = mutableStateOf(1)
        )
        
        val goblin5 = Attacker(
            id = 2,
            type = AttackerType.GOBLIN,
            position = mutableStateOf(Position(1, 0)),
            level = mutableStateOf(5)
        )
        
        assertEquals(20, goblin1.maxHealth, "Level 1 goblin should have 20 health")
        assertEquals(100, goblin5.maxHealth, "Level 5 goblin should have 100 health (20 * 5)")
        assertEquals(20, goblin1.currentHealth.value, "Level 1 goblin should start with max health")
        assertEquals(100, goblin5.currentHealth.value, "Level 5 goblin should start with max health")
    }
    
    @Test
    fun testEwhadIsBoss() {
        val ewhad = Attacker(
            id = 1,
            type = AttackerType.EWHAD,
            position = mutableStateOf(Position(0, 0)),
            level = mutableStateOf(1)
        )
        
        assertTrue(ewhad.type.isBoss, "Ewhad should be marked as a boss")
        assertEquals(200, ewhad.maxHealth, "Ewhad should have 200 base health")
    }
    
    @Test
    fun testRedWitchCanDisable() {
        val redWitch = Attacker(
            id = 1,
            type = AttackerType.RED_WITCH,
            position = mutableStateOf(Position(0, 0)),
            level = mutableStateOf(1)
        )
        
        assertTrue(redWitch.type.canDisableTowers, "Red Witch should be able to disable towers")
    }
    
    @Test
    fun testGreenWitchCanHeal() {
        val greenWitch = Attacker(
            id = 1,
            type = AttackerType.GREEN_WITCH,
            position = mutableStateOf(Position(0, 0)),
            level = mutableStateOf(1)
        )
        
        assertTrue(greenWitch.type.canHeal, "Green Witch should be able to heal")
    }
    
    @Test
    fun testTowerDisableStatus() {
        val tower = Defender(
            id = 1,
            type = DefenderType.SPIKE_TOWER,
            position = mutableStateOf(Position(0, 0)),
            level = mutableStateOf(5),
            buildTimeRemaining = mutableStateOf(0)
        )
        
        tower.resetActions()
        assertTrue(tower.isReady, "Tower should be ready")
        assertFalse(tower.isDisabled.value, "Tower should not be disabled initially")
        
        // Disable the tower
        tower.isDisabled.value = true
        tower.disabledTurnsRemaining.value = 3
        
        assertTrue(tower.isDisabled.value, "Tower should be disabled")
        assertEquals(3, tower.disabledTurnsRemaining.value, "Tower should have 3 turns remaining")
        
        // Tower should not be able to attack when disabled
        val enemy = Attacker(
            id = 2,
            type = AttackerType.GOBLIN,
            position = mutableStateOf(Position(1, 0)),
            level = mutableStateOf(1)
        )
        
        assertFalse(tower.canAttack(enemy), "Disabled tower should not be able to attack")
    }
}
