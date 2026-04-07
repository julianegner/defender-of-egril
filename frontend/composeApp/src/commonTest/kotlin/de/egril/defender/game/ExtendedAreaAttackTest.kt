package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Tests for extended area attack mechanics (level 20+ towers)
 */
class ExtendedAreaAttackTest {
    
    /**
     * Helper method to create a test level with a wider path for area attack testing
     */
    private fun createTestLevel(): Level {
        // Create a path with multiple adjacent cells to test area effects
        val pathCells = mutableSetOf<Position>()
        // Main path from x=0 to x=9 at y=3
        for (x in 0..9) {
            pathCells.add(Position(x, 3))
        }
        // Additional path cells above and below to test area of effect
        for (x in 2..7) {
            pathCells.add(Position(x, 2))
            pathCells.add(Position(x, 4))
        }
        // Even more cells for radius 2 testing
        for (x in 3..6) {
            pathCells.add(Position(x, 1))
            pathCells.add(Position(x, 5))
        }
        
        return Level(
            id = 1,
            name = "Test Level",
            gridWidth = 12,
            gridHeight = 8,
            startPositions = listOf(Position(0, 3)),
            targetPositions = listOf(Position(11, 3)),
            pathCells = pathCells,
            buildAreas = setOf(Position(5, 0), Position(5, 6)),
            attackerWaves = listOf(
                AttackerWave(listOf(AttackerType.GOBLIN, AttackerType.GOBLIN))
            ),
            initialCoins = 10000,
            healthPoints = 10
        )
    }
    
    @Test
    fun testAreaEffectRadiusForLowLevelTower() {
        val level = createTestLevel()
        val state = GameState(level)
        val engine = GameEngine(state)
        
        // Place a wizard tower (fireball)
        assertTrue(engine.placeDefender(DefenderType.WIZARD_TOWER, Position(5, 0)))
        val wizard = state.defenders.first()
        wizard.buildTimeRemaining.value = 0  // Skip build time
        wizard.level.value = 1  // Level 1 should have radius 1
        
        // Verify the area effect radius is 1 at low level
        assertEquals(1, wizard.areaEffectRadius, "Area effect radius should be 1 for level 1 tower")
        assertFalse(wizard.hasExtendedAreaEffect, "Should not have extended area effect at level 1")
    }
    
    @Test
    fun testAreaEffectRadiusForLevel19Tower() {
        val level = createTestLevel()
        val state = GameState(level)
        val engine = GameEngine(state)
        
        // Place a wizard tower
        assertTrue(engine.placeDefender(DefenderType.WIZARD_TOWER, Position(5, 0)))
        val wizard = state.defenders.first()
        wizard.buildTimeRemaining.value = 0
        wizard.level.value = 19  // Level 19 should still have radius 1
        
        assertEquals(1, wizard.areaEffectRadius, "Area effect radius should be 1 for level 19 tower")
        assertFalse(wizard.hasExtendedAreaEffect, "Should not have extended area effect at level 19")
    }
    
    @Test
    fun testAreaEffectRadiusForLevel20Tower() {
        val level = createTestLevel()
        val state = GameState(level)
        val engine = GameEngine(state)
        
        // Place a wizard tower
        assertTrue(engine.placeDefender(DefenderType.WIZARD_TOWER, Position(5, 0)))
        val wizard = state.defenders.first()
        wizard.buildTimeRemaining.value = 0
        wizard.level.value = 20  // Level 20 should have radius 2
        
        assertEquals(2, wizard.areaEffectRadius, "Area effect radius should be 2 for level 20 tower")
        assertTrue(wizard.hasExtendedAreaEffect, "Should have extended area effect at level 20")
    }
    
    @Test
    fun testAreaEffectRadiusForHighLevelTower() {
        val level = createTestLevel()
        val state = GameState(level)
        val engine = GameEngine(state)
        
        // Place a wizard tower
        assertTrue(engine.placeDefender(DefenderType.WIZARD_TOWER, Position(5, 0)))
        val wizard = state.defenders.first()
        wizard.buildTimeRemaining.value = 0
        wizard.level.value = 30  // Level 30 should also have radius 2
        
        assertEquals(2, wizard.areaEffectRadius, "Area effect radius should be 2 for level 30 tower")
        assertTrue(wizard.hasExtendedAreaEffect, "Should have extended area effect at level 30")
    }
    
    @Test
    fun testAlchemyTowerExtendedAreaEffect() {
        val level = createTestLevel()
        val state = GameState(level)
        val engine = GameEngine(state)
        
        // Place an alchemy tower
        assertTrue(engine.placeDefender(DefenderType.ALCHEMY_TOWER, Position(5, 0)))
        val alchemy = state.defenders.first()
        alchemy.buildTimeRemaining.value = 0
        alchemy.level.value = 20  // Level 20 should have radius 2
        
        assertEquals(2, alchemy.areaEffectRadius, "Area effect radius should be 2 for level 20 alchemy tower")
        assertTrue(alchemy.hasExtendedAreaEffect, "Should have extended area effect at level 20")
    }
    
    @Test
    fun testNonAreaTowerHasNoAreaEffectRadius() {
        val level = createTestLevel()
        val state = GameState(level)
        val engine = GameEngine(state)
        
        // Place a bow tower (ranged, not area)
        assertTrue(engine.placeDefender(DefenderType.BOW_TOWER, Position(5, 0)))
        val bow = state.defenders.first()
        bow.buildTimeRemaining.value = 0
        bow.level.value = 20
        
        assertEquals(0, bow.areaEffectRadius, "Area effect radius should be 0 for non-area tower")
        assertFalse(bow.hasExtendedAreaEffect, "Non-area tower should not have extended area effect")
    }
    
    @Test
    fun testFireballAffectsMoreEnemiesWithExtendedRadius() {
        val level = createTestLevel()
        val state = GameState(level)
        val engine = GameEngine(state)
        
        // Place a level 20 wizard tower
        assertTrue(engine.placeDefender(DefenderType.WIZARD_TOWER, Position(5, 0)))
        val wizard = state.defenders.first()
        wizard.buildTimeRemaining.value = 0
        wizard.level.value = 20
        
        // Start the game
        engine.startFirstPlayerTurn()
        wizard.resetActions()
        
        // Spawn enemies at different distances from the target position (5, 3)
        // Distance 1: immediate neighbors
        val enemy1 = Attacker(
            id = state.nextAttackerId.value++,
            type = AttackerType.GOBLIN,
            position = mutableStateOf(Position(5, 3)),  // Center
            level = mutableStateOf(1)
        )
        state.attackers.add(enemy1)
        
        val enemy2 = Attacker(
            id = state.nextAttackerId.value++,
            type = AttackerType.GOBLIN,
            position = mutableStateOf(Position(5, 4)),  // Distance 1
            level = mutableStateOf(1)
        )
        state.attackers.add(enemy2)
        
        // Distance 2: outer ring
        val enemy3 = Attacker(
            id = state.nextAttackerId.value++,
            type = AttackerType.GOBLIN,
            position = mutableStateOf(Position(5, 5)),  // Distance 2
            level = mutableStateOf(1)
        )
        state.attackers.add(enemy3)
        
        // Attack with wizard at center position
        assertTrue(engine.defenderAttackPosition(wizard.id, Position(5, 3)), "Attack should succeed")
        
        // All three enemies should be damaged since radius is 2
        assertTrue(enemy1.currentHealth.value < enemy1.type.health, "Enemy at center should be damaged")
        assertTrue(enemy2.currentHealth.value < enemy2.type.health, "Enemy at distance 1 should be damaged")
        assertTrue(enemy3.currentHealth.value < enemy3.type.health, "Enemy at distance 2 should be damaged")
    }
    
    @Test
    fun testFireballEffectsCreatedForExtendedRadius() {
        val level = createTestLevel()
        val state = GameState(level)
        val engine = GameEngine(state)
        
        // Place a level 20 wizard tower
        assertTrue(engine.placeDefender(DefenderType.WIZARD_TOWER, Position(5, 0)))
        val wizard = state.defenders.first()
        wizard.buildTimeRemaining.value = 0
        wizard.level.value = 20
        
        // Start the game
        engine.startFirstPlayerTurn()
        wizard.resetActions()
        
        // Attack a position on the path
        val targetPos = Position(5, 3)
        assertTrue(engine.defenderAttackPosition(wizard.id, targetPos), "Attack should succeed")
        
        // Verify fireball effects were created
        val fireballEffects = state.fieldEffects.filter { it.type == FieldEffectType.FIREBALL }
        assertTrue(fireballEffects.isNotEmpty(), "Fireball effects should be created")
        
        // Count how many effects are at distance 2
        val distance2Effects = fireballEffects.filter {
            targetPos.distanceTo(it.position) == 2
        }
        assertTrue(distance2Effects.isNotEmpty(), "Effects should be created at distance 2 for level 20 tower")
    }
    
    @Test
    fun testShowExtendedAreaTutorialOnUpgradeToLevel20() {
        val level = createTestLevel()
        val state = GameState(level)
        
        // Place a wizard tower
        assertTrue(state.canPlaceDefender(DefenderType.WIZARD_TOWER))
        val towerManager = TowerManager(state)
        towerManager.placeDefender(DefenderType.WIZARD_TOWER, Position(5, 0))
        
        val wizard = state.defenders.first()
        wizard.buildTimeRemaining.value = 0
        wizard.level.value = 19  // Start at level 19
        
        // Start the game and give coins for upgrades
        state.phase.value = GamePhase.PLAYER_TURN
        state.coins.value = 10000  // Enough for upgrades
        
        // Verify tutorial hasn't been shown
        assertFalse(wizard.hasShownExtendedAreaTutorial.value, "Tutorial should not be shown initially")
        
        // Upgrade to level 20
        assertTrue(towerManager.upgradeDefender(wizard.id), "Upgrade should succeed")
        assertEquals(20, wizard.level.value, "Tower should be level 20")
        
        // Verify tutorial was triggered
        assertEquals(InfoType.EXTENDED_AREA_INFO, state.infoState.value.currentInfo,
            "Extended area info should be shown")
        assertTrue(wizard.hasShownExtendedAreaTutorial.value, "Tutorial should be marked as shown")
    }
    
    @Test
    fun testExtendedAreaTutorialNotShownTwice() {
        val level = createTestLevel()
        val state = GameState(level)
        
        val towerManager = TowerManager(state)
        towerManager.placeDefender(DefenderType.WIZARD_TOWER, Position(5, 0))
        
        val wizard = state.defenders.first()
        wizard.buildTimeRemaining.value = 0
        wizard.level.value = 19
        wizard.hasShownExtendedAreaTutorial.value = true  // Mark as already shown
        
        state.phase.value = GamePhase.PLAYER_TURN
        state.coins.value = 10000
        
        // Upgrade to level 20
        assertTrue(towerManager.upgradeDefender(wizard.id))
        
        // Verify tutorial was not triggered again
        assertEquals(InfoType.NONE, state.infoState.value.currentInfo, 
            "Extended area info should not be shown twice")
    }
}
