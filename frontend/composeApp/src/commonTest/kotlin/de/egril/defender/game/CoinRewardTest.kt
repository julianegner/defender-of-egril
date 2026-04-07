package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.*
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test that coin rewards are multiplied by enemy level when they are defeated
 */
class CoinRewardTest {
    
    @Test
    fun testCoinRewardLevel1() {
        // Test that level 1 enemy gives base reward
        val level = createTestLevel()
        val state = GameState(level = level)
        val bridgeSystem = BridgeSystem(state)
        val combatSystem = CombatSystem(state, bridgeSystem)
        
        // Create a level 1 goblin (5 coin base reward)
        val goblin = Attacker(
            id = 1,
            type = AttackerType.GOBLIN,
            position = mutableStateOf(Position(1, 1)),
            level = mutableStateOf(1)
        )
        state.attackers.add(goblin)
        
        val initialCoins = state.coins.value
        
        // Defeat the goblin
        goblin.isDefeated.value = true
        combatSystem.processDefeatedAttackers()
        
        // Should get 5 coins (base reward × level 1)
        assertEquals(initialCoins + 5, state.coins.value, "Level 1 goblin should give 5 coins")
    }
    
    @Test
    fun testCoinRewardLevel3() {
        // Test that level 3 enemy gives 3× base reward
        val level = createTestLevel()
        val state = GameState(level = level)
        val bridgeSystem = BridgeSystem(state)
        val combatSystem = CombatSystem(state, bridgeSystem)
        
        // Create a level 3 ork (10 coin base reward)
        val ork = Attacker(
            id = 1,
            type = AttackerType.ORK,
            position = mutableStateOf(Position(1, 1)),
            level = mutableStateOf(3)
        )
        state.attackers.add(ork)
        
        val initialCoins = state.coins.value
        
        // Defeat the ork
        ork.isDefeated.value = true
        combatSystem.processDefeatedAttackers()
        
        // Should get 30 coins (base reward 10 × level 3)
        assertEquals(initialCoins + 30, state.coins.value, "Level 3 ork should give 30 coins")
    }
    
    @Test
    fun testCoinRewardLevel5() {
        // Test that level 5 enemy gives 5× base reward
        val level = createTestLevel()
        val state = GameState(level = level)
        val bridgeSystem = BridgeSystem(state)
        val combatSystem = CombatSystem(state, bridgeSystem)
        
        // Create a level 5 ogre (20 coin base reward)
        val ogre = Attacker(
            id = 1,
            type = AttackerType.OGRE,
            position = mutableStateOf(Position(1, 1)),
            level = mutableStateOf(5)
        )
        state.attackers.add(ogre)
        
        val initialCoins = state.coins.value
        
        // Defeat the ogre
        ogre.isDefeated.value = true
        combatSystem.processDefeatedAttackers()
        
        // Should get 100 coins (base reward 20 × level 5)
        assertEquals(initialCoins + 100, state.coins.value, "Level 5 ogre should give 100 coins")
    }
    
    @Test
    fun testCoinRewardMultipleEnemies() {
        // Test multiple enemies with different levels
        val level = createTestLevel()
        val state = GameState(level = level)
        val bridgeSystem = BridgeSystem(state)
        val combatSystem = CombatSystem(state, bridgeSystem)
        
        // Create multiple enemies
        val goblin1 = Attacker(
            id = 1,
            type = AttackerType.GOBLIN,  // 5 coins base
            position = mutableStateOf(Position(1, 1)),
            level = mutableStateOf(1)  // 5 × 1 = 5 coins
        )
        val goblin2 = Attacker(
            id = 2,
            type = AttackerType.GOBLIN,  // 5 coins base
            position = mutableStateOf(Position(2, 1)),
            level = mutableStateOf(2)  // 5 × 2 = 10 coins
        )
        val ork = Attacker(
            id = 3,
            type = AttackerType.ORK,  // 10 coins base
            position = mutableStateOf(Position(3, 1)),
            level = mutableStateOf(3)  // 10 × 3 = 30 coins
        )
        
        state.attackers.addAll(listOf(goblin1, goblin2, ork))
        
        val initialCoins = state.coins.value
        
        // Defeat all enemies
        goblin1.isDefeated.value = true
        goblin2.isDefeated.value = true
        ork.isDefeated.value = true
        combatSystem.processDefeatedAttackers()
        
        // Should get 45 coins total (5 + 10 + 30)
        assertEquals(initialCoins + 45, state.coins.value, 
            "Multiple enemies should give sum of level-adjusted rewards")
    }
    
    @Test
    fun testCoinRewardBossEnemy() {
        // Test boss enemy with high level
        val level = createTestLevel()
        val state = GameState(level = level)
        val bridgeSystem = BridgeSystem(state)
        val combatSystem = CombatSystem(state, bridgeSystem)
        
        // Create a level 2 Ewhad (100 coin base reward)
        val ewhad = Attacker(
            id = 1,
            type = AttackerType.EWHAD,
            position = mutableStateOf(Position(1, 1)),
            level = mutableStateOf(2)
        )
        state.attackers.add(ewhad)
        
        val initialCoins = state.coins.value
        
        // Defeat the boss
        ewhad.isDefeated.value = true
        combatSystem.processDefeatedAttackers()
        
        // Should get 200 coins (base reward 100 × level 2)
        assertEquals(initialCoins + 200, state.coins.value, "Level 2 Ewhad should give 200 coins")
    }
    
    @Test
    fun testCoinRewardAtTargetPosition() {
        // Test that enemies reaching target position don't give coins
        val level = createTestLevel()
        val state = GameState(level = level)
        val bridgeSystem = BridgeSystem(state)
        val combatSystem = CombatSystem(state, bridgeSystem)
        
        // Create a goblin at target position
        val targetPos = level.targetPositions.first()
        val goblin = Attacker(
            id = 1,
            type = AttackerType.GOBLIN,
            position = mutableStateOf(targetPos),
            level = mutableStateOf(2)
        )
        state.attackers.add(goblin)
        
        val initialCoins = state.coins.value
        
        // Mark as defeated at target (reached target)
        goblin.isDefeated.value = true
        combatSystem.processDefeatedAttackers()
        
        // Should NOT get coins for enemies that reached target
        assertEquals(initialCoins, state.coins.value, 
            "Enemies reaching target should not give coins")
    }
    
    /**
     * Helper to create a minimal test level
     */
    private fun createTestLevel(): Level {
        return Level(
            id = 1,
            name = "Test Level",
            subtitle = "",
            gridWidth = 10,
            gridHeight = 10,
            startPositions = listOf(Position(0, 0)),
            targetPositions = listOf(Position(9, 9)),
            pathCells = (0..9).flatMap { x -> (0..9).map { y -> Position(x, y) } }.toSet(),
            attackerWaves = listOf(AttackerWave(emptyList())),
            initialCoins = 100,
            healthPoints = 10,
            availableTowers = emptySet()
        )
    }
}
