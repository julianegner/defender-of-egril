package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for Green Witch healing ability to ensure it works correctly
 */
class GreenWitchHealingTest {
    
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
    fun testGreenWitchHealsAdjacentEnemy() {
        val level = createTestLevel()
        val state = GameState(level)
        val engine = GameEngine(state)
        
        // Create a green witch at position (3, 3)
        val greenWitch = Attacker(
            id = state.nextAttackerId.value++,
            type = AttackerType.GREEN_WITCH,
            position = mutableStateOf(Position(3, 3)),
            level = mutableStateOf(1)
        )
        state.attackers.add(greenWitch)
        
        // Create a damaged goblin adjacent to the green witch (at position 4, 3)
        // Position(4, 3) is a hex neighbor of Position(3, 3)
        val damagedGoblin = Attacker(
            id = state.nextAttackerId.value++,
            type = AttackerType.GOBLIN,
            position = mutableStateOf(Position(4, 3)),
            level = mutableStateOf(1)
        )
        // Damage the goblin (20 max HP, damage it to 10 HP)
        damagedGoblin.currentHealth.value = 10
        state.attackers.add(damagedGoblin)
        
        // Record goblin's health before healing
        val healthBeforeHealing = damagedGoblin.currentHealth.value
        assertEquals(10, healthBeforeHealing, "Goblin should have 10 HP before healing")
        
        // Process enemy abilities (this simulates the enemy turn phase where green witch healing occurs)
        val enemyAbilities = EnemyAbilitySystem(state)
        enemyAbilities.processEnemyAbilities()
        
        // Check that the goblin was healed
        val healthAfterHealing = damagedGoblin.currentHealth.value
        assertTrue(healthAfterHealing > healthBeforeHealing, 
            "Goblin should be healed by green witch (before: $healthBeforeHealing, after: $healthAfterHealing)")
        
        // With a level 1 green witch, healing should be 5 HP (5x level)
        assertEquals(15, healthAfterHealing, 
            "Level 1 green witch should heal 5 HP (from 10 to 15)")
    }
    
    @Test
    fun testGreenWitchHealingLimitedByMissingHealth() {
        val level = createTestLevel()
        val state = GameState(level)
        
        // Create a level 5 green witch (can heal up to 25 HP per turn = 5x5)
        val greenWitch = Attacker(
            id = state.nextAttackerId.value++,
            type = AttackerType.GREEN_WITCH,
            position = mutableStateOf(Position(3, 3)),
            level = mutableStateOf(5)
        )
        state.attackers.add(greenWitch)
        
        // Create a goblin with only 2 HP missing (20 max, 18 current)
        val slightlyDamagedGoblin = Attacker(
            id = state.nextAttackerId.value++,
            type = AttackerType.GOBLIN,
            position = mutableStateOf(Position(4, 3)),
            level = mutableStateOf(1)
        )
        slightlyDamagedGoblin.currentHealth.value = 18  // Missing only 2 HP
        state.attackers.add(slightlyDamagedGoblin)
        
        // Process enemy abilities
        val enemyAbilities = EnemyAbilitySystem(state)
        enemyAbilities.processEnemyAbilities()
        
        // Healing should be capped at missing health (2 HP), not witch heal amount (25 HP)
        assertEquals(20, slightlyDamagedGoblin.currentHealth.value,
            "Goblin should be healed to max HP (18 + 2 = 20), not overheal")
    }
    
    @Test
    fun testGreenWitchDoesNotHealSelf() {
        val level = createTestLevel()
        val state = GameState(level)
        
        // Create a damaged green witch at position (3, 3)
        val greenWitch = Attacker(
            id = state.nextAttackerId.value++,
            type = AttackerType.GREEN_WITCH,
            position = mutableStateOf(Position(3, 3)),
            level = mutableStateOf(2)
        )
        greenWitch.currentHealth.value = 10  // 25 max HP, damaged to 10
        state.attackers.add(greenWitch)
        
        // No adjacent enemies - only the witch itself
        
        val healthBefore = greenWitch.currentHealth.value
        
        // Process enemy abilities
        val enemyAbilities = EnemyAbilitySystem(state)
        enemyAbilities.processEnemyAbilities()
        
        // Green witch should NOT heal itself
        assertEquals(healthBefore, greenWitch.currentHealth.value,
            "Green witch should not heal itself")
    }
    
    @Test
    fun testGreenWitchHealsMultipleAdjacentEnemies() {
        val level = createTestLevel()
        val state = GameState(level)
        
        // Create a level 3 green witch at position (3, 3)
        val greenWitch = Attacker(
            id = state.nextAttackerId.value++,
            type = AttackerType.GREEN_WITCH,
            position = mutableStateOf(Position(3, 3)),
            level = mutableStateOf(3)
        )
        state.attackers.add(greenWitch)
        
        // Get hex neighbors of position (3, 3) and filter to valid path positions
        val neighbors = Position(3, 3).getHexNeighbors()
        
        // Create damaged enemies at the first 3 valid neighbor positions (on the path)
        val damagedEnemies = mutableListOf<Attacker>()
        for (neighbor in neighbors.filter { level.isOnPath(it) }.take(3)) {
            val enemy = Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.GOBLIN,
                position = mutableStateOf(neighbor),
                level = mutableStateOf(1)
            )
            enemy.currentHealth.value = 10  // 20 max HP, damaged to 10
            state.attackers.add(enemy)
            damagedEnemies.add(enemy)
        }
        
        // Process enemy abilities
        val enemyAbilities = EnemyAbilitySystem(state)
        enemyAbilities.processEnemyAbilities()
        
        // All damaged adjacent enemies should be healed
        for (enemy in damagedEnemies) {
            assertTrue(enemy.currentHealth.value > 10,
                "Adjacent enemy at ${enemy.position.value} should be healed")
            assertEquals(20, enemy.currentHealth.value,
                "Level 3 green witch should heal 15 HP (5x3) to each adjacent enemy, bringing 10 to full 20 HP")
        }
    }
    
    @Test
    fun testGreenWitchDoesNotHealNonAdjacentEnemies() {
        val level = createTestLevel()
        val state = GameState(level)
        
        // Create a green witch at position (3, 3)
        val greenWitch = Attacker(
            id = state.nextAttackerId.value++,
            type = AttackerType.GREEN_WITCH,
            position = mutableStateOf(Position(3, 3)),
            level = mutableStateOf(2)
        )
        state.attackers.add(greenWitch)
        
        // Create a damaged goblin that is NOT adjacent (position 6, 3 is not a hex neighbor of 3, 3)
        val distantGoblin = Attacker(
            id = state.nextAttackerId.value++,
            type = AttackerType.GOBLIN,
            position = mutableStateOf(Position(6, 3)),
            level = mutableStateOf(1)
        )
        distantGoblin.currentHealth.value = 10  // Damaged to 10 HP
        state.attackers.add(distantGoblin)
        
        val healthBefore = distantGoblin.currentHealth.value
        
        // Process enemy abilities
        val enemyAbilities = EnemyAbilitySystem(state)
        enemyAbilities.processEnemyAbilities()
        
        // Distant goblin should NOT be healed
        assertEquals(healthBefore, distantGoblin.currentHealth.value,
            "Green witch should only heal adjacent enemies, not distant ones")
    }
    
    @Test
    fun testGreenWitchDoesNotHealDefeatedEnemies() {
        val level = createTestLevel()
        val state = GameState(level)
        
        // Create a green witch at position (3, 3)
        val greenWitch = Attacker(
            id = state.nextAttackerId.value++,
            type = AttackerType.GREEN_WITCH,
            position = mutableStateOf(Position(3, 3)),
            level = mutableStateOf(2)
        )
        state.attackers.add(greenWitch)
        
        // Create a defeated goblin adjacent to the green witch
        val defeatedGoblin = Attacker(
            id = state.nextAttackerId.value++,
            type = AttackerType.GOBLIN,
            position = mutableStateOf(Position(4, 3)),
            level = mutableStateOf(1)
        )
        defeatedGoblin.currentHealth.value = 0
        defeatedGoblin.isDefeated.value = true
        state.attackers.add(defeatedGoblin)
        
        // Process enemy abilities
        val enemyAbilities = EnemyAbilitySystem(state)
        enemyAbilities.processEnemyAbilities()
        
        // Defeated goblin should NOT be healed (stays at 0 HP)
        assertEquals(0, defeatedGoblin.currentHealth.value,
            "Green witch should not heal defeated enemies")
        assertTrue(defeatedGoblin.isDefeated.value,
            "Defeated enemy should remain defeated")
    }
}
