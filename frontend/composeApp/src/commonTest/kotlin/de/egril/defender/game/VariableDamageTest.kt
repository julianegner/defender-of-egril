package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.*
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test variable damage points when enemies reach the target.
 * - Mighty enemies (wizards, witches, demons): 1 HP × enemy level
 * - Ewhad (boss): All remaining health points
 * - All other enemies: 1 HP
 */
class VariableDamageTest {
    
    @Test
    fun testGoblinCauses1Damage() {
        val goblin = Attacker(
            id = 1,
            type = AttackerType.GOBLIN,
            position = mutableStateOf(Position(0, 0)),
            level = mutableStateOf(1)
        )
        assertEquals(1, goblin.calculateTargetDamage(), "Goblin should cause 1 HP damage")
    }
    
    @Test
    fun testOrkCauses1Damage() {
        val ork = Attacker(
            id = 1,
            type = AttackerType.ORK,
            position = mutableStateOf(Position(0, 0)),
            level = mutableStateOf(1)
        )
        assertEquals(1, ork.calculateTargetDamage(), "Ork should cause 1 HP damage")
    }
    
    @Test
    fun testSkeletonCauses1Damage() {
        val skeleton = Attacker(
            id = 1,
            type = AttackerType.SKELETON,
            position = mutableStateOf(Position(0, 0)),
            level = mutableStateOf(1)
        )
        assertEquals(1, skeleton.calculateTargetDamage(), "Skeleton should cause 1 HP damage")
    }
    
    @Test
    fun testOgreCauses1Damage() {
        val ogre = Attacker(
            id = 1,
            type = AttackerType.OGRE,
            position = mutableStateOf(Position(0, 0)),
            level = mutableStateOf(1)
        )
        assertEquals(1, ogre.calculateTargetDamage(), "Ogre should cause 1 HP damage")
    }
    
    @Test
    fun testEvilWizardCausesLevelDamage() {
        val wizard1 = Attacker(
            id = 1,
            type = AttackerType.EVIL_WIZARD,
            position = mutableStateOf(Position(0, 0)),
            level = mutableStateOf(1)
        )
        assertEquals(1, wizard1.calculateTargetDamage(), "Evil Wizard level 1 should cause 1 HP damage")
        
        val wizard3 = Attacker(
            id = 2,
            type = AttackerType.EVIL_WIZARD,
            position = mutableStateOf(Position(0, 0)),
            level = mutableStateOf(3)
        )
        assertEquals(3, wizard3.calculateTargetDamage(), "Evil Wizard level 3 should cause 3 HP damage")
    }
    
    @Test
    fun testRedWitchCausesLevelDamage() {
        val redWitch = Attacker(
            id = 1,
            type = AttackerType.RED_WITCH,
            position = mutableStateOf(Position(0, 0)),
            level = mutableStateOf(2)
        )
        assertEquals(2, redWitch.calculateTargetDamage(), "Red Witch level 2 should cause 2 HP damage")
    }
    
    @Test
    fun testGreenWitchCausesLevelDamage() {
        val greenWitch = Attacker(
            id = 1,
            type = AttackerType.GREEN_WITCH,
            position = mutableStateOf(Position(0, 0)),
            level = mutableStateOf(3)
        )
        assertEquals(3, greenWitch.calculateTargetDamage(), "Green Witch level 3 should cause 3 HP damage")
    }
    
    @Test
    fun testBlueDemonCausesLevelDamage() {
        val blueDemon = Attacker(
            id = 1,
            type = AttackerType.BLUE_DEMON,
            position = mutableStateOf(Position(0, 0)),
            level = mutableStateOf(1)
        )
        assertEquals(1, blueDemon.calculateTargetDamage(), "Blue Demon level 1 should cause 1 HP damage")
    }
    
    @Test
    fun testRedDemonCausesLevelDamage() {
        val redDemon = Attacker(
            id = 1,
            type = AttackerType.RED_DEMON,
            position = mutableStateOf(Position(0, 0)),
            level = mutableStateOf(4)
        )
        assertEquals(4, redDemon.calculateTargetDamage(), "Red Demon level 4 should cause 4 HP damage")
    }
    
    @Test
    fun testDragonCausesLevelDamage() {
        val dragon = Attacker(
            id = 1,
            type = AttackerType.DRAGON,
            position = mutableStateOf(Position(0, 0)),
            level = mutableStateOf(5)
        )
        assertEquals(5, dragon.calculateTargetDamage(), "Dragon level 5 should cause 5 HP damage")
    }
    
    @Test
    fun testEwhadCausesAllDamage() {
        val ewhad = Attacker(
            id = 1,
            type = AttackerType.EWHAD,
            position = mutableStateOf(Position(0, 0)),
            level = mutableStateOf(1)
        )
        assertEquals(Int.MAX_VALUE, ewhad.calculateTargetDamage(), "Ewhad should cause all HP damage (Int.MAX_VALUE marker)")
    }
    
    @Test
    fun testActualDamageApplication() {
        // Create a simple level
        val level = Level(
            id = 1,
            name = "Variable Damage Test",
            gridWidth = 5,
            gridHeight = 5,
            startPositions = listOf(Position(0, 2)),
            targetPositions = listOf(Position(4, 2)),
            pathCells = setOf(Position(0, 2), Position(1, 2), Position(2, 2), Position(3, 2)),
            attackerWaves = emptyList(),
            initialCoins = 100,
            healthPoints = 20
        )
        
        val state = GameState(level)
        val engine = GameEngine(state)
        
        // Test: Evil Wizard level 3 causes 3 damage
        val wizard = Attacker(
            id = 1,
            type = AttackerType.EVIL_WIZARD,
            position = mutableStateOf(Position(3, 2)),
            level = mutableStateOf(3)
        )
        state.attackers.add(wizard)
        
        val initialHP = state.healthPoints.value
        engine.applyMovement(wizard.id, Position(4, 2))
        
        assertEquals(initialHP - 3, state.healthPoints.value, "Evil Wizard level 3 should reduce HP by 3")
        assertEquals(true, wizard.isDefeated.value, "Enemy should be defeated after reaching target")
    }
    
    @Test
    fun testEwhadCausesAllHealthLoss() {
        // Create a simple level
        val level = Level(
            id = 1,
            name = "Ewhad Damage Test",
            gridWidth = 5,
            gridHeight = 5,
            startPositions = listOf(Position(0, 2)),
            targetPositions = listOf(Position(4, 2)),
            pathCells = setOf(Position(0, 2), Position(1, 2), Position(2, 2), Position(3, 2)),
            attackerWaves = emptyList(),
            initialCoins = 100,
            healthPoints = 50
        )
        
        val state = GameState(level)
        val engine = GameEngine(state)
        
        // Test: Ewhad causes all damage
        val ewhad = Attacker(
            id = 1,
            type = AttackerType.EWHAD,
            position = mutableStateOf(Position(3, 2)),
            level = mutableStateOf(1)
        )
        state.attackers.add(ewhad)
        
        engine.applyMovement(ewhad.id, Position(4, 2))
        
        assertEquals(0, state.healthPoints.value, "Ewhad should reduce HP to 0")
        assertEquals(true, ewhad.isDefeated.value, "Enemy should be defeated after reaching target")
    }
}
