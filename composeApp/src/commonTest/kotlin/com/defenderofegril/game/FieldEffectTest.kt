package com.defenderofegril.game

import androidx.compose.runtime.mutableStateOf
import com.defenderofegril.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for field effects (fireball, acid) to ensure they are properly managed
 */
class FieldEffectTest {
    
    @Test
    fun testFireballEffectsRemovedAfterTurnEnd() {
        // Create a simple level for testing
        val level = Level(
            id = 1,
            name = "Test Level",
            gridWidth = 10,
            gridHeight = 6,
            startPositions = listOf(Position(0, 3)),
            targetPosition = Position(9, 3),
            pathCells = (0..9).map { Position(it, 3) }.toSet(),
            buildIslands = setOf(Position(2, 1), Position(2, 2)),
            attackerWaves = listOf(
                AttackerWave(listOf(AttackerType.GOBLIN, AttackerType.GOBLIN))
            ),
            initialCoins = 1000,
            healthPoints = 10,
            availableTowers = listOf(DefenderType.WIZARD_TOWER)
        )
        
        val state = GameState(level)
        val engine = GameEngine(state)
        
        // Place a wizard tower (fireball)
        assertTrue(engine.placeDefender(DefenderType.WIZARD_TOWER, Position(2, 1)))
        val wizard = state.defenders.first()
        wizard.buildTimeRemaining.value = 0  // Skip build time
        
        // Start the game
        engine.startFirstPlayerTurn()
        wizard.resetActions()
        
        // Verify wizard is ready and has actions
        assertTrue(wizard.isReady, "Wizard should be ready")
        assertTrue(wizard.actionsRemaining.value > 0, "Wizard should have actions")
        
        // Spawn an enemy at a position the wizard can attack
        val enemy = Attacker(
            id = state.nextAttackerId.value++,
            type = AttackerType.GOBLIN,
            position = mutableStateOf(Position(4, 3)),  // On the path, in range
            level = 1
        )
        state.attackers.add(enemy)
        
        // Attack with wizard to create fireball effects
        val targetPos = Position(4, 3)
        assertTrue(engine.defenderAttackPosition(wizard.id, targetPos), "Attack should succeed")
        
        // Verify fireball effects were created
        val fireballEffects = state.fieldEffects.filter { it.type == FieldEffectType.FIREBALL }
        assertTrue(fireballEffects.isNotEmpty(), "Fireball effects should be created")
        assertTrue(fireballEffects.all { it.turnsRemaining == 1 }, "Fireball effects should have 1 turn remaining")
        
        // End the player turn
        engine.endPlayerTurn()
        
        // Verify field effects were removed after turn end
        val remainingFireballEffects = state.fieldEffects.filter { it.type == FieldEffectType.FIREBALL }
        assertEquals(0, remainingFireballEffects.size, "Fireball effects should be removed after turn end")
    }
    
    @Test
    fun testAcidEffectsPersistMultipleTurns() {
        // Create a simple level for testing
        val level = Level(
            id = 1,
            name = "Test Level",
            gridWidth = 10,
            gridHeight = 6,
            startPositions = listOf(Position(0, 3)),
            targetPosition = Position(9, 3),
            pathCells = (0..9).map { Position(it, 3) }.toSet(),
            buildIslands = setOf(Position(2, 1), Position(2, 2)),
            attackerWaves = listOf(
                AttackerWave(listOf(AttackerType.GOBLIN, AttackerType.GOBLIN))
            ),
            initialCoins = 1000,
            healthPoints = 10,
            availableTowers = listOf(DefenderType.ALCHEMY_TOWER)
        )
        
        val state = GameState(level)
        val engine = GameEngine(state)
        
        // Place an alchemy tower (acid DOT)
        assertTrue(engine.placeDefender(DefenderType.ALCHEMY_TOWER, Position(2, 1)))
        val alchemy = state.defenders.first()
        alchemy.buildTimeRemaining.value = 0  // Skip build time
        
        // Start the game
        engine.startFirstPlayerTurn()
        alchemy.resetActions()
        
        // Spawn an enemy
        val enemy = Attacker(
            id = state.nextAttackerId.value++,
            type = AttackerType.GOBLIN,
            position = mutableStateOf(Position(4, 3)),
            level = 1
        )
        state.attackers.add(enemy)
        
        // Attack with alchemy tower to create acid effects
        val targetPos = Position(4, 3)
        assertTrue(engine.defenderAttackPosition(alchemy.id, targetPos), "Attack should succeed")
        
        // Verify acid effects were created
        val acidEffects = state.fieldEffects.filter { it.type == FieldEffectType.ACID }
        assertTrue(acidEffects.isNotEmpty(), "Acid effects should be created")
        val initialTurns = acidEffects.first().turnsRemaining
        assertTrue(initialTurns > 1, "Acid effects should last multiple turns")
        
        // End the player turn
        engine.endPlayerTurn()
        
        // Verify acid effects still exist but with decremented turns
        val remainingAcidEffects = state.fieldEffects.filter { it.type == FieldEffectType.ACID }
        assertTrue(remainingAcidEffects.isNotEmpty(), "Acid effects should persist after one turn")
        assertTrue(remainingAcidEffects.all { it.turnsRemaining == initialTurns - 1 }, 
            "Acid effects should have decremented turn count")
    }
    
    @Test
    fun testMultipleFireballEffectsFromSameTower() {
        // Create a simple level for testing
        val level = Level(
            id = 1,
            name = "Test Level",
            gridWidth = 10,
            gridHeight = 6,
            startPositions = listOf(Position(0, 3)),
            targetPosition = Position(9, 3),
            pathCells = (0..9).map { Position(it, 3) }.toSet(),
            buildIslands = setOf(Position(2, 1), Position(2, 2)),
            attackerWaves = listOf(
                AttackerWave(listOf(AttackerType.GOBLIN, AttackerType.GOBLIN))
            ),
            initialCoins = 1000,
            healthPoints = 10,
            availableTowers = listOf(DefenderType.WIZARD_TOWER)
        )
        
        val state = GameState(level)
        val engine = GameEngine(state)
        
        // Place a wizard tower
        assertTrue(engine.placeDefender(DefenderType.WIZARD_TOWER, Position(2, 1)))
        val wizard = state.defenders.first()
        wizard.buildTimeRemaining.value = 0
        
        // Start the game
        engine.startFirstPlayerTurn()
        wizard.resetActions()
        
        // Give wizard multiple actions for testing
        wizard.actionsRemaining.value = 2
        
        // Spawn enemies at different positions
        state.attackers.add(Attacker(
            id = state.nextAttackerId.value++,
            type = AttackerType.GOBLIN,
            position = mutableStateOf(Position(4, 3)),
            level = 1
        ))
        state.attackers.add(Attacker(
            id = state.nextAttackerId.value++,
            type = AttackerType.GOBLIN,
            position = mutableStateOf(Position(6, 3)),
            level = 1
        ))
        
        // First attack
        assertTrue(engine.defenderAttackPosition(wizard.id, Position(4, 3)), "First attack should succeed")
        val firstEffectCount = state.fieldEffects.filter { it.type == FieldEffectType.FIREBALL }.size
        assertTrue(firstEffectCount > 0, "First attack should create fireball effects")
        
        // Second attack - old effects from same wizard should be cleared
        assertTrue(engine.defenderAttackPosition(wizard.id, Position(6, 3)), "Second attack should succeed")
        val secondEffectCount = state.fieldEffects.filter { it.type == FieldEffectType.FIREBALL }.size
        
        // After second attack, only effects from second attack should remain (old ones cleared)
        assertTrue(secondEffectCount > 0, "Second attack should create new fireball effects")
        
        // All remaining fireball effects should be from the same defender (wizard)
        val fireballEffects = state.fieldEffects.filter { it.type == FieldEffectType.FIREBALL }
        assertTrue(fireballEffects.all { it.defenderId == wizard.id }, 
            "All fireball effects should be from the wizard")
    }
}
