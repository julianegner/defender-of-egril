package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.Attacker
import de.egril.defender.model.AttackerType
import de.egril.defender.model.Position
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test that mighty units (wizards, witches, demons, dragons) deal damage equal to their level
 * when they reach the target, not just 1 HP.
 */
class MightyUnitDamageTest {
    
    @Test
    fun testGoblinDealsSingleDamage() {
        // Normal units deal 1 HP damage regardless of level
        val goblin = Attacker(
            id = 1,
            type = AttackerType.GOBLIN,
            position = mutableStateOf(Position(0, 0)),
            level = mutableStateOf(3)
        )
        assertEquals(1, goblin.calculateTargetDamage(), 
            "Goblin should always deal 1 HP damage")
    }
    
    @Test
    fun testOrkDealsSingleDamage() {
        val ork = Attacker(
            id = 1,
            type = AttackerType.ORK,
            position = mutableStateOf(Position(0, 0)),
            level = mutableStateOf(5)
        )
        assertEquals(1, ork.calculateTargetDamage(), 
            "Ork should always deal 1 HP damage")
    }
    
    @Test
    fun testSkeletonDealsSingleDamage() {
        val skeleton = Attacker(
            id = 1,
            type = AttackerType.SKELETON,
            position = mutableStateOf(Position(0, 0)),
            level = mutableStateOf(2)
        )
        assertEquals(1, skeleton.calculateTargetDamage(), 
            "Skeleton should always deal 1 HP damage")
    }
    
    @Test
    fun testOgreDealsSingleDamage() {
        val ogre = Attacker(
            id = 1,
            type = AttackerType.OGRE,
            position = mutableStateOf(Position(0, 0)),
            level = mutableStateOf(4)
        )
        assertEquals(1, ogre.calculateTargetDamage(), 
            "Ogre should always deal 1 HP damage")
    }
    
    @Test
    fun testEvilWizardDealsLevelDamage() {
        // Mighty units deal damage equal to their level
        val wizard = Attacker(
            id = 1,
            type = AttackerType.EVIL_WIZARD,
            position = mutableStateOf(Position(0, 0)),
            level = mutableStateOf(3)
        )
        assertEquals(3, wizard.calculateTargetDamage(), 
            "Evil Wizard at level 3 should deal 3 HP damage")
    }
    
    @Test
    fun testRedWitchDealsLevelDamage() {
        val redWitch = Attacker(
            id = 1,
            type = AttackerType.RED_WITCH,
            position = mutableStateOf(Position(0, 0)),
            level = mutableStateOf(5)
        )
        assertEquals(5, redWitch.calculateTargetDamage(), 
            "Red Witch at level 5 should deal 5 HP damage")
    }
    
    @Test
    fun testGreenWitchDealsLevelDamage() {
        val greenWitch = Attacker(
            id = 1,
            type = AttackerType.GREEN_WITCH,
            position = mutableStateOf(Position(0, 0)),
            level = mutableStateOf(4)
        )
        assertEquals(4, greenWitch.calculateTargetDamage(), 
            "Green Witch at level 4 should deal 4 HP damage")
    }
    
    @Test
    fun testEvilMageDealsLevelDamage() {
        val evilMage = Attacker(
            id = 1,
            type = AttackerType.EVIL_MAGE,
            position = mutableStateOf(Position(0, 0)),
            level = mutableStateOf(6)
        )
        assertEquals(6, evilMage.calculateTargetDamage(), 
            "Evil Mage at level 6 should deal 6 HP damage")
    }
    
    @Test
    fun testBlueDemonDealsLevelDamage() {
        val blueDemon = Attacker(
            id = 1,
            type = AttackerType.BLUE_DEMON,
            position = mutableStateOf(Position(0, 0)),
            level = mutableStateOf(7)
        )
        assertEquals(7, blueDemon.calculateTargetDamage(), 
            "Blue Demon at level 7 should deal 7 HP damage")
    }
    
    @Test
    fun testRedDemonDealsLevelDamage() {
        val redDemon = Attacker(
            id = 1,
            type = AttackerType.RED_DEMON,
            position = mutableStateOf(Position(0, 0)),
            level = mutableStateOf(8)
        )
        assertEquals(8, redDemon.calculateTargetDamage(), 
            "Red Demon at level 8 should deal 8 HP damage")
    }
    
    @Test
    fun testDragonDealsLevelDamage() {
        val dragon = Attacker(
            id = 1,
            type = AttackerType.DRAGON,
            position = mutableStateOf(Position(0, 0)),
            level = mutableStateOf(10)
        )
        assertEquals(10, dragon.calculateTargetDamage(), 
            "Dragon at level 10 should deal 10 HP damage")
    }
    
    @Test
    fun testDragonLevel1Damage() {
        val dragon = Attacker(
            id = 1,
            type = AttackerType.DRAGON,
            position = mutableStateOf(Position(0, 0)),
            level = mutableStateOf(1)
        )
        assertEquals(1, dragon.calculateTargetDamage(), 
            "Dragon at level 1 should deal 1 HP damage")
    }
    
    @Test
    fun testDragonHighLevelDamage() {
        // Dragon with 2500 HP = level 5 (2500 / 500)
        val dragon = Attacker(
            id = 1,
            type = AttackerType.DRAGON,
            position = mutableStateOf(Position(0, 0)),
            level = mutableStateOf(5),
            currentHealth = mutableStateOf(2500)
        )
        assertEquals(5, dragon.calculateTargetDamage(), 
            "Dragon at level 5 (2500 HP) should deal 5 HP damage")
    }
    
    @Test
    fun testEwhadReturnsMaxIntMarker() {
        // Ewhad returns Int.MAX_VALUE as a special marker
        // The game engine should handle this specially to cause instant level loss
        val ewhad = Attacker(
            id = 1,
            type = AttackerType.EWHAD,
            position = mutableStateOf(Position(0, 0)),
            level = mutableStateOf(1)
        )
        assertEquals(Int.MAX_VALUE, ewhad.calculateTargetDamage(), 
            "Ewhad should return Int.MAX_VALUE to indicate instant level loss")
    }
    
    @Test
    fun testAllMightyUnitsAtLevel1() {
        // Test all mighty units at level 1 deal 1 damage
        val mightyTypes = listOf(
            AttackerType.EVIL_WIZARD,
            AttackerType.RED_WITCH,
            AttackerType.GREEN_WITCH,
            AttackerType.EVIL_MAGE,
            AttackerType.BLUE_DEMON,
            AttackerType.RED_DEMON,
            AttackerType.DRAGON
        )
        
        for (type in mightyTypes) {
            val attacker = Attacker(
                id = 1,
                type = type,
                position = mutableStateOf(Position(0, 0)),
                level = mutableStateOf(1)
            )
            assertEquals(1, attacker.calculateTargetDamage(), 
                "${type.displayName} at level 1 should deal 1 HP damage")
        }
    }
    
    @Test
    fun testAllMightyUnitsAtLevel10() {
        // Test all mighty units at level 10 deal 10 damage
        val mightyTypes = listOf(
            AttackerType.EVIL_WIZARD,
            AttackerType.RED_WITCH,
            AttackerType.GREEN_WITCH,
            AttackerType.EVIL_MAGE,
            AttackerType.BLUE_DEMON,
            AttackerType.RED_DEMON,
            AttackerType.DRAGON
        )
        
        for (type in mightyTypes) {
            val attacker = Attacker(
                id = 1,
                type = type,
                position = mutableStateOf(Position(0, 0)),
                level = mutableStateOf(10)
            )
            assertEquals(10, attacker.calculateTargetDamage(), 
                "${type.displayName} at level 10 should deal 10 HP damage")
        }
    }
}
