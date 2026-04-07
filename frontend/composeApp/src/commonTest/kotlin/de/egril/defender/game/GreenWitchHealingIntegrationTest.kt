package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Integration test for Green Witch healing ability through the full game turn cycle
 */
class GreenWitchHealingIntegrationTest {
    
    /**
     * Helper method to create a standard test level
     */
    private fun createTestLevel(): Level {
        return Level(
            id = 1,
            name = "Test Level",
            gridWidth = 10,
            gridHeight = 6,
            startPositions = listOf(Position(0, 3)),
            targetPositions = listOf(Position(9, 3)),
            pathCells = (0..9).map { Position(it, 3) }.toSet(),
            buildAreas = setOf(Position(2, 1), Position(2, 2)),
            attackerWaves = listOf(
                AttackerWave(listOf(AttackerType.GOBLIN))
            ),
            initialCoins = 1000,
            healthPoints = 10
        )
    }
    
    @Test
    fun testGreenWitchHealsAdjacentDamagedEnemyDuringEnemyTurn() {
        val level = createTestLevel()
        val state = GameState(level)
        val engine = GameEngine(state)
        
        // Start the game
        engine.startFirstPlayerTurn()
        
        // Manually add a green witch at position (3, 3)
        val greenWitch = Attacker(
            id = state.nextAttackerId.value++,
            type = AttackerType.GREEN_WITCH,
            position = mutableStateOf(Position(3, 3)),
            level = mutableStateOf(2)  // Level 2 green witch can heal 10 HP (5x2)
        )
        state.attackers.add(greenWitch)
        
        // Add a damaged goblin adjacent to the green witch
        val damagedGoblin = Attacker(
            id = state.nextAttackerId.value++,
            type = AttackerType.GOBLIN,
            position = mutableStateOf(Position(4, 3)),  // Adjacent to green witch
            level = mutableStateOf(1)
        )
        damagedGoblin.currentHealth.value = 15  // 20 max HP, damaged to 15
        state.attackers.add(damagedGoblin)
        
        // Verify initial state
        assertEquals(15, damagedGoblin.currentHealth.value, "Goblin should start with 15 HP")
        
        // End the player turn and complete enemy turn (this triggers healing)
        engine.startEnemyTurn()
        engine.completeEnemyTurn()
        
        // Verify the goblin was healed during the enemy turn
        // Level 2 witch heals 10 HP (5x2), but goblin only needs 5 HP to reach max
        assertEquals(20, damagedGoblin.currentHealth.value,
            "Level 2 green witch should heal goblin to full HP (15 + 5 = 20)")
    }
    
    @Test
    fun testGreenWitchHealingAcrossMultipleTurns() {
        val level = createTestLevel()
        val state = GameState(level)
        val engine = GameEngine(state)
        
        // Start the game
        engine.startFirstPlayerTurn()
        
        // Create a level 3 green witch
        val greenWitch = Attacker(
            id = state.nextAttackerId.value++,
            type = AttackerType.GREEN_WITCH,
            position = mutableStateOf(Position(3, 3)),
            level = mutableStateOf(3)  // Level 3 can heal 3 HP per turn
        )
        state.attackers.add(greenWitch)
        
        // Create a heavily damaged ogre adjacent to the witch
        val damagedOgre = Attacker(
            id = state.nextAttackerId.value++,
            type = AttackerType.OGRE,
            position = mutableStateOf(Position(4, 3)),
            level = mutableStateOf(1)
        )
        damagedOgre.currentHealth.value = 10  // 80 max HP, damaged to 10
        state.attackers.add(damagedOgre)
        
        // Turn 1: Heal 15 HP (5x3)
        engine.startEnemyTurn()
        engine.completeEnemyTurn()
        assertEquals(25, damagedOgre.currentHealth.value,
            "Turn 1: Ogre should be healed from 10 to 25 (10 + 15)")
        
        // Turn 2: Heal another 15 HP (5x3)
        engine.startEnemyTurn()
        engine.completeEnemyTurn()
        assertEquals(40, damagedOgre.currentHealth.value,
            "Turn 2: Ogre should be healed from 25 to 40 (25 + 15)")
        
        // Turn 3: Heal another 15 HP (5x3)
        engine.startEnemyTurn()
        engine.completeEnemyTurn()
        assertEquals(55, damagedOgre.currentHealth.value,
            "Turn 3: Ogre should be healed from 40 to 55 (40 + 15)")
    }
    
    @Test
    fun testGreenWitchWithTowerDamageAndHealing() {
        val level = createTestLevel()
        val state = GameState(level)
        val engine = GameEngine(state)
        
        // Place a bow tower at (2, 1) that has range 3 and can attack position (3, 3)
        assertTrue(engine.placeDefender(DefenderType.BOW_TOWER, Position(2, 1)))
        val tower = state.defenders.first()
        tower.buildTimeRemaining.value = 0  // Skip build time
        
        // Start the game
        engine.startFirstPlayerTurn()
        tower.resetActions()
        
        // Create a green witch at position (3, 3)
        val greenWitch = Attacker(
            id = state.nextAttackerId.value++,
            type = AttackerType.GREEN_WITCH,
            position = mutableStateOf(Position(3, 3)),
            level = mutableStateOf(3)
        )
        state.attackers.add(greenWitch)
        
        // Create a goblin adjacent to the green witch at (4, 3)
        val goblin = Attacker(
            id = state.nextAttackerId.value++,
            type = AttackerType.GOBLIN,
            position = mutableStateOf(Position(4, 3)),
            level = mutableStateOf(1)
        )
        state.attackers.add(goblin)
        
        // Attack the goblin with the tower (should deal damage)
        assertTrue(tower.canAttack(goblin), "Bow tower at ${tower.position} should be able to attack goblin at ${goblin.position.value}")
        assertTrue(engine.defenderAttack(tower.id, goblin.id), "Tower attack should succeed")
        
        val healthAfterAttack = goblin.currentHealth.value
        assertTrue(healthAfterAttack < 20, "Goblin should be damaged (HP < 20)")
        
        // End the player turn and complete enemy turn (green witch should heal)
        engine.startEnemyTurn()
        engine.completeEnemyTurn()
        
        // Goblin should be healed by green witch
        val healthAfterHealing = goblin.currentHealth.value
        assertTrue(healthAfterHealing > healthAfterAttack,
            "Green witch should heal the damaged goblin (before: $healthAfterAttack, after: $healthAfterHealing)")
        
        // Healing amount should be min(15 (5x3), missing health)
        val expectedHealing = minOf(15, 20 - healthAfterAttack)
        assertEquals(healthAfterAttack + expectedHealing, healthAfterHealing,
            "Healing should be $expectedHealing HP")
    }
}
