package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for trap and barricade placement mode preservation.
 * 
 * Requirements:
 * - Dwarven mine trap placement: Button should stay enabled if tower has actions remaining
 * - Barricade placement: Button should stay enabled if tower has actions remaining
 * - Magical trap placement: Button should always be disabled after placement (has cooldown)
 */
class TrapBarricadePlacementModeTest {
    
    // Helper function to create a minimal test level
    private fun createTestLevel(): Level {
        val pathCells = setOf(
            Position(0, 0), Position(1, 0), Position(2, 0), Position(3, 0), 
            Position(4, 0), Position(5, 0), Position(6, 0)
        )
        val buildAreas = setOf(Position(2, 2), Position(2, 3), Position(3, 2), Position(3, 3))
        
        return Level(
            id = 1,
            name = "Test Level",
            subtitle = "Test",
            gridWidth = 10,
            gridHeight = 10,
            startPositions = listOf(Position(0, 0)),
            targetPositions = listOf(Position(6, 0)),
            pathCells = pathCells,
            buildAreas = buildAreas,
            attackerWaves = emptyList(),
            initialCoins = 1000,
            healthPoints = 10,
            availableTowers = setOf(DefenderType.DWARVEN_MINE, DefenderType.SPIKE_TOWER, DefenderType.WIZARD_TOWER)
        )
    }
    
    @Test
    fun testDwarvenMineTrapPlacementConsumesOneAction() {
        val level = createTestLevel()
        val gameState = GameState(level)
        val gameEngine = GameEngine(gameState)
        
        // Create a level 10 dwarven mine (has 2 actions per turn)
        val mine = Defender(
            id = 1,
            type = DefenderType.DWARVEN_MINE,
            position = mutableStateOf(Position(2, 2)),
            level = mutableStateOf(10)
        )
        mine.actionsRemaining.value = 2
        mine.buildTimeRemaining.value = 0 // Ready
        gameState.defenders.add(mine)
        
        // Verify initial state
        assertEquals(2, mine.actionsRemaining.value, "Mine should start with 2 actions")
        
        // Place a trap
        val trapPosition = Position(2, 0)
        val success = gameEngine.performMineBuildTrap(mine.id, trapPosition)
        
        // Verify trap was placed and action consumed
        assertTrue(success, "Trap placement should succeed")
        assertEquals(1, mine.actionsRemaining.value, "Mine should have 1 action remaining after placing trap")
        assertEquals(1, gameState.traps.size, "Trap should be added to game state")
    }
    
    @Test
    fun testBarricadePlacementConsumesOneAction() {
        val level = createTestLevel()
        val gameState = GameState(level)
        val gameEngine = GameEngine(gameState)
        
        // Create a level 20 spike tower (has 3 actions per turn, can build barricades)
        val tower = Defender(
            id = 1,
            type = DefenderType.SPIKE_TOWER,
            position = mutableStateOf(Position(2, 2)),
            level = mutableStateOf(20)
        )
        tower.actionsRemaining.value = 3
        tower.buildTimeRemaining.value = 0 // Ready
        gameState.defenders.add(tower)
        
        // Verify initial state
        assertEquals(3, tower.actionsRemaining.value, "Tower should start with 3 actions")
        
        // Place a barricade
        val barricadePosition = Position(2, 0)
        val success = gameEngine.performBuildBarricade(tower.id, barricadePosition)
        
        // Verify barricade was placed and action consumed
        assertTrue(success, "Barricade placement should succeed")
        assertEquals(2, tower.actionsRemaining.value, "Tower should have 2 actions remaining after placing barricade")
        assertEquals(1, gameState.barricades.size, "Barricade should be added to game state")
    }
    
    @Test
    fun testMagicalTrapPlacementConsumesOneActionAndSetsCooldown() {
        val level = createTestLevel()
        val gameState = GameState(level)
        val gameEngine = GameEngine(gameState)
        
        // Create a level 15 wizard tower (has 2 actions per turn, can place magical traps)
        val wizard = Defender(
            id = 1,
            type = DefenderType.WIZARD_TOWER,
            position = mutableStateOf(Position(2, 2)),
            level = mutableStateOf(15)
        )
        wizard.actionsRemaining.value = 2
        wizard.buildTimeRemaining.value = 0 // Ready
        wizard.trapCooldownRemaining.value = 0 // No cooldown
        gameState.defenders.add(wizard)
        
        // Verify initial state
        assertEquals(2, wizard.actionsRemaining.value, "Wizard should start with 2 actions")
        assertEquals(0, wizard.trapCooldownRemaining.value, "Wizard should have no cooldown initially")
        
        // Place a magical trap
        val trapPosition = Position(2, 0)
        val success = gameEngine.performWizardPlaceMagicalTrap(wizard.id, trapPosition)
        
        // Verify trap was placed, action consumed, and cooldown set
        assertTrue(success, "Magical trap placement should succeed")
        assertEquals(1, wizard.actionsRemaining.value, "Wizard should have 1 action remaining after placing trap")
        assertEquals(10, wizard.trapCooldownRemaining.value, "Wizard should have 10 turn cooldown after placing magical trap")
        assertEquals(1, gameState.traps.size, "Trap should be added to game state")
    }
    
    @Test
    fun testMultipleTrapPlacementsWithMultipleActions() {
        val level = createTestLevel()
        val gameState = GameState(level)
        val gameEngine = GameEngine(gameState)
        
        // Create a level 10 dwarven mine (has 2 actions per turn)
        val mine = Defender(
            id = 1,
            type = DefenderType.DWARVEN_MINE,
            position = mutableStateOf(Position(2, 2)),
            level = mutableStateOf(10)
        )
        mine.actionsRemaining.value = 2
        mine.buildTimeRemaining.value = 0 // Ready
        gameState.defenders.add(mine)
        
        // Place first trap
        val success1 = gameEngine.performMineBuildTrap(mine.id, Position(1, 0))
        assertTrue(success1, "First trap placement should succeed")
        assertEquals(1, mine.actionsRemaining.value, "Mine should have 1 action remaining")
        assertEquals(1, gameState.traps.size, "Should have 1 trap")
        
        // Place second trap
        val success2 = gameEngine.performMineBuildTrap(mine.id, Position(3, 0))
        assertTrue(success2, "Second trap placement should succeed")
        assertEquals(0, mine.actionsRemaining.value, "Mine should have 0 actions remaining")
        assertEquals(2, gameState.traps.size, "Should have 2 traps")
    }
    
    @Test
    fun testMultipleBarricadePlacementsWithMultipleActions() {
        val level = createTestLevel()
        val gameState = GameState(level)
        val gameEngine = GameEngine(gameState)
        
        // Create a level 25 spear tower (has 3 actions per turn, can build barricades)
        val tower = Defender(
            id = 1,
            type = DefenderType.SPEAR_TOWER,
            position = mutableStateOf(Position(2, 2)),
            level = mutableStateOf(25)
        )
        tower.actionsRemaining.value = 3
        tower.buildTimeRemaining.value = 0 // Ready
        gameState.defenders.add(tower)
        
        // Place first barricade
        val success1 = gameEngine.performBuildBarricade(tower.id, Position(1, 0))
        assertTrue(success1, "First barricade placement should succeed")
        assertEquals(2, tower.actionsRemaining.value, "Tower should have 2 actions remaining")
        assertEquals(1, gameState.barricades.size, "Should have 1 barricade")
        
        // Place second barricade
        val success2 = gameEngine.performBuildBarricade(tower.id, Position(3, 0))
        assertTrue(success2, "Second barricade placement should succeed")
        assertEquals(1, tower.actionsRemaining.value, "Tower should have 1 action remaining")
        assertEquals(2, gameState.barricades.size, "Should have 2 barricades")
        
        // Place third barricade
        val success3 = gameEngine.performBuildBarricade(tower.id, Position(4, 0))
        assertTrue(success3, "Third barricade placement should succeed")
        assertEquals(0, tower.actionsRemaining.value, "Tower should have 0 actions remaining")
        assertEquals(3, gameState.barricades.size, "Should have 3 barricades")
    }
    
    @Test
    fun testCannotPlaceTrapWithNoActionsRemaining() {
        val level = createTestLevel()
        val gameState = GameState(level)
        val gameEngine = GameEngine(gameState)
        
        // Create a dwarven mine with no actions remaining
        val mine = Defender(
            id = 1,
            type = DefenderType.DWARVEN_MINE,
            position = mutableStateOf(Position(2, 2)),
            level = mutableStateOf(10)
        )
        mine.actionsRemaining.value = 0
        mine.buildTimeRemaining.value = 0 // Ready
        gameState.defenders.add(mine)
        
        // Try to place a trap
        val success = gameEngine.performMineBuildTrap(mine.id, Position(2, 0))
        
        // Verify trap was NOT placed
        assertFalse(success, "Trap placement should fail with no actions remaining")
        assertEquals(0, gameState.traps.size, "No trap should be added")
    }
    
    @Test
    fun testCannotPlaceBarricadeWithNoActionsRemaining() {
        val level = createTestLevel()
        val gameState = GameState(level)
        val gameEngine = GameEngine(gameState)
        
        // Create a spike tower with no actions remaining
        val tower = Defender(
            id = 1,
            type = DefenderType.SPIKE_TOWER,
            position = mutableStateOf(Position(2, 2)),
            level = mutableStateOf(20)
        )
        tower.actionsRemaining.value = 0
        tower.buildTimeRemaining.value = 0 // Ready
        gameState.defenders.add(tower)
        
        // Try to place a barricade
        val success = gameEngine.performBuildBarricade(tower.id, Position(2, 0))
        
        // Verify barricade was NOT placed
        assertFalse(success, "Barricade placement should fail with no actions remaining")
        assertEquals(0, gameState.barricades.size, "No barricade should be added")
    }
    
    @Test
    fun testCannotPlaceMagicalTrapWithCooldown() {
        val level = createTestLevel()
        val gameState = GameState(level)
        val gameEngine = GameEngine(gameState)
        
        // Create a wizard tower with actions but on cooldown
        val wizard = Defender(
            id = 1,
            type = DefenderType.WIZARD_TOWER,
            position = mutableStateOf(Position(2, 2)),
            level = mutableStateOf(15)
        )
        wizard.actionsRemaining.value = 2
        wizard.buildTimeRemaining.value = 0 // Ready
        wizard.trapCooldownRemaining.value = 5 // On cooldown
        gameState.defenders.add(wizard)
        
        // Try to place a magical trap
        val success = gameEngine.performWizardPlaceMagicalTrap(wizard.id, Position(2, 0))
        
        // Verify trap was NOT placed
        assertFalse(success, "Magical trap placement should fail when on cooldown")
        assertEquals(0, gameState.traps.size, "No trap should be added")
        assertEquals(2, wizard.actionsRemaining.value, "Actions should not be consumed on failed placement")
    }
}
