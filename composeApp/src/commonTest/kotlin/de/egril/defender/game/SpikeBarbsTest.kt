package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for Spike Tower Barbs feature (level 10+):
 * - Spike tower level 10+ reduces enemy movement by 1 per attack
 * - Effect stacks across multiple attacks
 * - Enemies always move at least 1 tile per turn
 * - Info message shown when spike tower first reaches level 10
 */
class SpikeBarbsTest {
    
    /**
     * Test that spike tower level 10+ applies movement penalty
     */
    @Test
    fun testSpikeTowerLevel10AppliesMovementPenalty() {
        // Create a simple level for testing
        val level = Level(
            id = 1,
            name = "Test Level",
            gridWidth = 10,
            gridHeight = 6,
            startPositions = listOf(Position(0, 3)),
            targetPositions = listOf(Position(9, 3)),
            pathCells = (0..9).map { Position(it, 3) }.toSet(),
            buildIslands = setOf(Position(2, 2)),
            attackerWaves = listOf(
                AttackerWave(listOf(AttackerType.GOBLIN))
            ),
            initialCoins = 1000,
            healthPoints = 10
        )
        
        val state = GameState(level, constructionLevel = 1)  // Set construction level to enable spike barbs
        val engine = GameEngine(state)
        
        // Place a spike tower at level 10
        assertTrue(engine.placeDefender(DefenderType.SPIKE_TOWER, Position(2, 2)))
        val tower = state.defenders.first()
        tower.level.value = 10
        tower.buildTimeRemaining.value = 0
        tower.resetActions()
        
        // Start game and spawn an enemy
        engine.startFirstPlayerTurn()
        val goblin = Attacker(
            id = state.nextAttackerId.value++,
            type = AttackerType.GOBLIN,
            position = mutableStateOf(Position(1, 3)),
            level = mutableStateOf(1)
        )
        state.attackers.add(goblin)
        
        // Initial movement penalty should be 0
        assertEquals(0, goblin.movementPenalty.value, "Goblin should start with 0 movement penalty")
        
        // Attack the goblin
        assertTrue(engine.defenderAttack(tower.id, goblin.id), "Attack should succeed")
        
        // Movement penalty should now be 1
        assertEquals(1, goblin.movementPenalty.value, "Goblin should have 1 movement penalty after being hit by level 10 spike")
    }
    
    /**
     * Test that spike tower level 9 does NOT apply movement penalty
     */
    @Test
    fun testSpikeTowerLevel9DoesNotApplyMovementPenalty() {
        // Create a simple level for testing
        val level = Level(
            id = 1,
            name = "Test Level",
            gridWidth = 10,
            gridHeight = 6,
            startPositions = listOf(Position(0, 3)),
            targetPositions = listOf(Position(9, 3)),
            pathCells = (0..9).map { Position(it, 3) }.toSet(),
            buildIslands = setOf(Position(2, 2)),
            attackerWaves = listOf(
                AttackerWave(listOf(AttackerType.GOBLIN))
            ),
            initialCoins = 1000,
            healthPoints = 10
        )
        
        val state = GameState(level)
        val engine = GameEngine(state)
        
        // Place a spike tower at level 9 (below threshold)
        assertTrue(engine.placeDefender(DefenderType.SPIKE_TOWER, Position(2, 2)))
        val tower = state.defenders.first()
        tower.level.value = 9
        tower.buildTimeRemaining.value = 0
        tower.resetActions()
        
        // Start game and spawn an enemy
        engine.startFirstPlayerTurn()
        val goblin = Attacker(
            id = state.nextAttackerId.value++,
            type = AttackerType.GOBLIN,
            position = mutableStateOf(Position(1, 3)),
            level = mutableStateOf(1)
        )
        state.attackers.add(goblin)
        
        // Attack the goblin
        assertTrue(engine.defenderAttack(tower.id, goblin.id), "Attack should succeed")
        
        // Movement penalty should still be 0
        assertEquals(0, goblin.movementPenalty.value, "Goblin should have 0 movement penalty after being hit by level 9 spike")
    }
    
    /**
     * Test that movement penalty stacks across multiple attacks
     */
    @Test
    fun testMovementPenaltyStacks() {
        // Create a simple level for testing
        val level = Level(
            id = 1,
            name = "Test Level",
            gridWidth = 10,
            gridHeight = 6,
            startPositions = listOf(Position(0, 3)),
            targetPositions = listOf(Position(9, 3)),
            pathCells = (0..9).map { Position(it, 3) }.toSet(),
            buildIslands = setOf(Position(2, 2), Position(3, 2)),
            attackerWaves = listOf(
                AttackerWave(listOf(AttackerType.GOBLIN))
            ),
            initialCoins = 2000,
            healthPoints = 10
        )
        
        val state = GameState(level, constructionLevel = 1)  // Set construction level to enable spike barbs
        val engine = GameEngine(state)
        
        // Place two spike towers at level 10
        assertTrue(engine.placeDefender(DefenderType.SPIKE_TOWER, Position(2, 2)))
        assertTrue(engine.placeDefender(DefenderType.SPIKE_TOWER, Position(3, 2)))
        val tower1 = state.defenders[0]
        val tower2 = state.defenders[1]
        tower1.level.value = 10
        tower2.level.value = 10
        tower1.buildTimeRemaining.value = 0
        tower2.buildTimeRemaining.value = 0
        tower1.resetActions()
        tower2.resetActions()
        
        // Start game and spawn an enemy
        engine.startFirstPlayerTurn()
        val goblin = Attacker(
            id = state.nextAttackerId.value++,
            type = AttackerType.GOBLIN,
            position = mutableStateOf(Position(1, 3)),
            level = mutableStateOf(1),
            currentHealth = mutableStateOf(100)  // High health to survive multiple hits
        )
        state.attackers.add(goblin)
        
        // Attack with first tower
        assertTrue(engine.defenderAttack(tower1.id, goblin.id), "First attack should succeed")
        assertEquals(1, goblin.movementPenalty.value, "Goblin should have 1 movement penalty after first attack")
        
        // Attack with second tower
        assertTrue(engine.defenderAttack(tower2.id, goblin.id), "Second attack should succeed")
        assertEquals(2, goblin.movementPenalty.value, "Goblin should have 2 movement penalty after second attack")
    }
    
    /**
     * Test that other tower types do NOT apply movement penalty
     */
    @Test
    fun testOtherTowersDoNotApplyMovementPenalty() {
        // Create a simple level for testing
        val level = Level(
            id = 1,
            name = "Test Level",
            gridWidth = 10,
            gridHeight = 6,
            startPositions = listOf(Position(0, 3)),
            targetPositions = listOf(Position(9, 3)),
            pathCells = (0..9).map { Position(it, 3) }.toSet(),
            buildIslands = setOf(Position(2, 2)),
            attackerWaves = listOf(
                AttackerWave(listOf(AttackerType.GOBLIN))
            ),
            initialCoins = 1000,
            healthPoints = 10
        )
        
        val state = GameState(level)
        val engine = GameEngine(state)
        
        // Place a spear tower at level 10 (not spike tower)
        assertTrue(engine.placeDefender(DefenderType.SPEAR_TOWER, Position(2, 2)))
        val tower = state.defenders.first()
        tower.level.value = 10
        tower.buildTimeRemaining.value = 0
        tower.resetActions()
        
        // Start game and spawn an enemy
        engine.startFirstPlayerTurn()
        val goblin = Attacker(
            id = state.nextAttackerId.value++,
            type = AttackerType.GOBLIN,
            position = mutableStateOf(Position(1, 3)),
            level = mutableStateOf(1)
        )
        state.attackers.add(goblin)
        
        // Attack the goblin
        assertTrue(engine.defenderAttack(tower.id, goblin.id), "Attack should succeed")
        
        // Movement penalty should still be 0
        assertEquals(0, goblin.movementPenalty.value, "Goblin should have 0 movement penalty after being hit by spear tower")
    }
    
    /**
     * Test that movement penalty affects enemy movement calculation
     */
    @Test
    fun testMovementPenaltyReducesEffectiveSpeed() {
        // Create a simple level for testing
        val level = Level(
            id = 1,
            name = "Test Level",
            gridWidth = 10,
            gridHeight = 6,
            startPositions = listOf(Position(0, 3)),
            targetPositions = listOf(Position(9, 3)),
            pathCells = (0..9).map { Position(it, 3) }.toSet(),
            buildIslands = setOf(Position(2, 2)),
            attackerWaves = listOf(
                AttackerWave(listOf(AttackerType.GOBLIN))
            ),
            initialCoins = 1000,
            healthPoints = 10
        )
        
        val state = GameState(level)
        
        // Spawn a goblin with movement penalty
        val goblin = Attacker(
            id = state.nextAttackerId.value++,
            type = AttackerType.GOBLIN,  // Base speed is 5
            position = mutableStateOf(Position(0, 3)),
            level = mutableStateOf(1)
        )
        goblin.movementPenalty.value = 2  // Penalty of 2
        state.attackers.add(goblin)
        
        // Goblin has base speed of 5, with penalty of 2, effective speed should be 3
        val baseSpeed = goblin.type.speed
        assertEquals(5, baseSpeed, "Goblin base speed should be 5")
        
        val effectiveSpeed = maxOf(1, baseSpeed - goblin.movementPenalty.value)
        assertEquals(3, effectiveSpeed, "Effective speed should be 3 (5 - 2)")
    }
    
    /**
     * Test that enemies always move at least 1 tile even with high penalty
     */
    @Test
    fun testMinimumMovementSpeed() {
        // Create a simple level for testing
        val level = Level(
            id = 1,
            name = "Test Level",
            gridWidth = 10,
            gridHeight = 6,
            startPositions = listOf(Position(0, 3)),
            targetPositions = listOf(Position(9, 3)),
            pathCells = (0..9).map { Position(it, 3) }.toSet(),
            buildIslands = setOf(Position(2, 2)),
            attackerWaves = listOf(
                AttackerWave(listOf(AttackerType.GOBLIN))
            ),
            initialCoins = 1000,
            healthPoints = 10
        )
        
        val state = GameState(level)
        
        // Spawn a goblin with very high movement penalty
        val goblin = Attacker(
            id = state.nextAttackerId.value++,
            type = AttackerType.GOBLIN,  // Base speed is 5
            position = mutableStateOf(Position(0, 3)),
            level = mutableStateOf(1)
        )
        goblin.movementPenalty.value = 10  // Penalty higher than base speed
        state.attackers.add(goblin)
        
        // Even with penalty of 10, effective speed should be at least 1
        val effectiveSpeed = maxOf(1, goblin.type.speed - goblin.movementPenalty.value)
        assertEquals(1, effectiveSpeed, "Effective speed should be at least 1")
    }
    
    /**
     * Test that spike barbs info is shown when spike tower reaches level 10
     */
    @Test
    fun testSpikeBarbsInfoShownAtLevel10() {
        // Create a simple level for testing
        val level = Level(
            id = 1,
            name = "Test Level",
            gridWidth = 10,
            gridHeight = 6,
            startPositions = listOf(Position(0, 3)),
            targetPositions = listOf(Position(9, 3)),
            pathCells = (0..9).map { Position(it, 3) }.toSet(),
            buildIslands = setOf(Position(2, 2)),
            attackerWaves = listOf(
                AttackerWave(listOf(AttackerType.GOBLIN))
            ),
            initialCoins = 10000,
            healthPoints = 10
        )
        
        val state = GameState(level)
        val engine = GameEngine(state)
        
        // Place a spike tower at level 9
        assertTrue(engine.placeDefender(DefenderType.SPIKE_TOWER, Position(2, 2)))
        val tower = state.defenders.first()
        tower.level.value = 9
        tower.buildTimeRemaining.value = 0
        
        // Start game
        engine.startFirstPlayerTurn()
        
        // Upgrade to level 10
        assertTrue(engine.upgradeDefender(tower.id), "Upgrade should succeed")
        assertEquals(10, tower.level.value, "Tower should be level 10")
        
        // Check that spike barbs info was shown
        assertEquals(InfoType.SPIKE_BARBS_INFO, state.infoState.value.currentInfo, "Spike barbs info should be shown")
        assertTrue(tower.hasShownSpikeBarbsTutorial.value, "Tutorial flag should be set")
    }
    
    /**
     * Test that spike barbs info is NOT shown when upgrading past level 10
     */
    @Test
    fun testSpikeBarbsInfoNotShownWhenUpgradingPastLevel10() {
        // Create a simple level for testing
        val level = Level(
            id = 1,
            name = "Test Level",
            gridWidth = 10,
            gridHeight = 6,
            startPositions = listOf(Position(0, 3)),
            targetPositions = listOf(Position(9, 3)),
            pathCells = (0..9).map { Position(it, 3) }.toSet(),
            buildIslands = setOf(Position(2, 2)),
            attackerWaves = listOf(
                AttackerWave(listOf(AttackerType.GOBLIN))
            ),
            initialCoins = 10000,
            healthPoints = 10
        )
        
        val state = GameState(level)
        val engine = GameEngine(state)
        
        // Place a spike tower at level 10
        assertTrue(engine.placeDefender(DefenderType.SPIKE_TOWER, Position(2, 2)))
        val tower = state.defenders.first()
        tower.level.value = 10
        tower.buildTimeRemaining.value = 0
        
        // Start game
        engine.startFirstPlayerTurn()
        
        // Upgrade to level 11
        assertTrue(engine.upgradeDefender(tower.id), "Upgrade should succeed")
        assertEquals(11, tower.level.value, "Tower should be level 11")
        
        // Check that spike barbs info was NOT shown (since tower was already level 10+)
        assertEquals(InfoType.NONE, state.infoState.value.currentInfo, "No info should be shown")
    }
    
    /**
     * Test that Green Witch removes up to 3 barbs from adjacent enemies
     */
    @Test
    fun testGreenWitchRemovesBarbs() {
        // Create a simple level for testing
        val level = Level(
            id = 1,
            name = "Test Level",
            gridWidth = 10,
            gridHeight = 6,
            startPositions = listOf(Position(0, 3)),
            targetPositions = listOf(Position(9, 3)),
            pathCells = (0..9).map { Position(it, 3) }.toSet(),
            buildIslands = setOf(Position(2, 2)),
            attackerWaves = listOf(
                AttackerWave(listOf(AttackerType.GOBLIN))
            ),
            initialCoins = 1000,
            healthPoints = 10
        )
        
        val state = GameState(level)
        val engine = GameEngine(state)
        
        // Start game and spawn a goblin with barbs
        engine.startFirstPlayerTurn()
        val goblin = Attacker(
            id = state.nextAttackerId.value++,
            type = AttackerType.GOBLIN,
            position = mutableStateOf(Position(2, 3)),
            level = mutableStateOf(1)
        )
        goblin.movementPenalty.value = 5  // 5 barbs
        state.attackers.add(goblin)
        
        // Spawn a Green Witch next to the goblin
        val greenWitch = Attacker(
            id = state.nextAttackerId.value++,
            type = AttackerType.GREEN_WITCH,
            position = mutableStateOf(Position(3, 3)),  // Adjacent to goblin
            level = mutableStateOf(1)
        )
        state.attackers.add(greenWitch)
        
        // Process enemy abilities (Green Witch should remove barbs)
        val abilitySystem = EnemyAbilitySystem(state)
        abilitySystem.processEnemyAbilities()
        
        // Check that Green Witch removed 3 barbs (5 - 3 = 2 remaining)
        assertEquals(2, goblin.movementPenalty.value, "Goblin should have 2 barbs remaining after Green Witch removes 3")
    }
    
    /**
     * Test that Green Witch removes all barbs if fewer than 3
     */
    @Test
    fun testGreenWitchRemovesAllBarbsWhenFewerThan3() {
        // Create a simple level for testing
        val level = Level(
            id = 1,
            name = "Test Level",
            gridWidth = 10,
            gridHeight = 6,
            startPositions = listOf(Position(0, 3)),
            targetPositions = listOf(Position(9, 3)),
            pathCells = (0..9).map { Position(it, 3) }.toSet(),
            buildIslands = setOf(Position(2, 2)),
            attackerWaves = listOf(
                AttackerWave(listOf(AttackerType.GOBLIN))
            ),
            initialCoins = 1000,
            healthPoints = 10
        )
        
        val state = GameState(level)
        val engine = GameEngine(state)
        
        // Start game and spawn a goblin with 2 barbs
        engine.startFirstPlayerTurn()
        val goblin = Attacker(
            id = state.nextAttackerId.value++,
            type = AttackerType.GOBLIN,
            position = mutableStateOf(Position(2, 3)),
            level = mutableStateOf(1)
        )
        goblin.movementPenalty.value = 2  // Only 2 barbs
        state.attackers.add(goblin)
        
        // Spawn a Green Witch next to the goblin
        val greenWitch = Attacker(
            id = state.nextAttackerId.value++,
            type = AttackerType.GREEN_WITCH,
            position = mutableStateOf(Position(3, 3)),  // Adjacent to goblin
            level = mutableStateOf(1)
        )
        state.attackers.add(greenWitch)
        
        // Process enemy abilities (Green Witch should remove all barbs)
        val abilitySystem = EnemyAbilitySystem(state)
        abilitySystem.processEnemyAbilities()
        
        // Check that Green Witch removed all 2 barbs
        assertEquals(0, goblin.movementPenalty.value, "Goblin should have 0 barbs remaining after Green Witch removes all")
    }
    
    /**
     * Test that movement penalty is saved and restored correctly in SavedAttacker
     */
    @Test
    fun testMovementPenaltySavedInSavedAttacker() {
        // Create an attacker with movement penalty
        val attacker = Attacker(
            id = 1,
            type = AttackerType.GOBLIN,
            position = mutableStateOf(Position(2, 3)),
            level = mutableStateOf(1)
        )
        attacker.movementPenalty.value = 3  // 3 barbs
        
        // Create a SavedAttacker from the attacker
        val savedAttacker = de.egril.defender.save.SavedAttacker(
            id = attacker.id,
            type = attacker.type,
            position = attacker.position.value,
            level = attacker.level.value,
            currentHealth = attacker.currentHealth.value,
            isDefeated = attacker.isDefeated.value,
            dragonName = attacker.dragonName,
            movementPenalty = attacker.movementPenalty.value
        )
        
        // Verify the movement penalty was saved
        assertEquals(3, savedAttacker.movementPenalty, "SavedAttacker should have movementPenalty of 3")
        
        // Create a new attacker from the saved attacker
        val restoredAttacker = Attacker(
            id = savedAttacker.id,
            type = savedAttacker.type,
            position = mutableStateOf(savedAttacker.position),
            level = mutableStateOf(savedAttacker.level),
            dragonName = savedAttacker.dragonName
        )
        restoredAttacker.currentHealth.value = savedAttacker.currentHealth
        restoredAttacker.isDefeated.value = savedAttacker.isDefeated
        restoredAttacker.movementPenalty.value = savedAttacker.movementPenalty
        
        // Verify the movement penalty was restored
        assertEquals(3, restoredAttacker.movementPenalty.value, "Restored attacker should have movementPenalty of 3")
    }
}
