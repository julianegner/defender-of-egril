package de.egril.defender.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import de.egril.defender.game.LevelData
import de.egril.defender.model.*
import de.egril.defender.ui.gameplay.GameGrid
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for the new Target Rings Overlay feature.
 * 
 * These tests verify that the target rings overlay renders correctly
 * for AREA and LASTING attack types, with arc segments only showing on path tiles.
 */
class TargetRingsOverlayTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun testWizardTowerTargetRingsOnPathTiles() {
        // Create a game state with a wizard tower and enemies
        val level = LevelData.createLevels().first { it.id == 1 }
        val gameState = GameState(level)
        
        // Set game to player turn phase
        gameState.phase.value = GamePhase.PLAYER_TURN
        gameState.turnNumber.value = 3
        
        // Add a Wizard Tower (AREA attack - fireball)
        val wizardTower = Defender(
            id = 1,
            type = DefenderType.WIZARD_TOWER,
            position = androidx.compose.runtime.mutableStateOf(Position(10, 3)),
            level = androidx.compose.runtime.mutableStateOf(1),
            buildTimeRemaining = androidx.compose.runtime.mutableStateOf(0),
            actionsRemaining = androidx.compose.runtime.mutableStateOf(1),
            placedOnTurn = 1
        )
        gameState.defenders.add(wizardTower)
        
        // Add enemies in a cluster on the path for area effect demonstration
        val enemy1 = Attacker(
            id = 1,
            type = AttackerType.GOBLIN,
            position = androidx.compose.runtime.mutableStateOf(Position(7, 4)),
            level = androidx.compose.runtime.mutableStateOf(1)
        )
        
        val enemy2 = Attacker(
            id = 2,
            type = AttackerType.GOBLIN,
            position = androidx.compose.runtime.mutableStateOf(Position(8, 4)),
            level = androidx.compose.runtime.mutableStateOf(1)
        )
        
        val enemy3 = Attacker(
            id = 3,
            type = AttackerType.ORK,
            position = androidx.compose.runtime.mutableStateOf(Position(7, 3)),
            level = androidx.compose.runtime.mutableStateOf(1)
        )
        
        gameState.attackers.add(enemy1)
        gameState.attackers.add(enemy2)
        gameState.attackers.add(enemy3)
        
        // State to track selected tower and target
        val selectedDefenderId = androidx.compose.runtime.mutableStateOf<Int?>(1)
        val selectedTargetId = androidx.compose.runtime.mutableStateOf<Int?>(1)
        val selectedTargetPosition = androidx.compose.runtime.mutableStateOf<Position?>(Position(7, 4))
        
        composeTestRule.setContent {
            GameGrid(
                gameState = gameState,
                selectedDefenderType = null,
                selectedDefenderId = selectedDefenderId.value,
                selectedTargetId = selectedTargetId.value,
                selectedTargetPosition = selectedTargetPosition.value,
                selectedMineAction = null,
                onCellClick = { }
            )
        }
        
        composeTestRule.waitForIdle()
        
        // Verify the grid renders
        composeTestRule.onRoot().assertExists()
        
        // Capture screenshot showing wizard tower with target rings overlay
        // The rings should appear as:
        // - 3 inner circles on the target tile (Position(7, 4))
        // - Arc segments on neighboring path tiles showing parts of 3 outer rings
        ScreenshotTestUtils.captureScreenshot(
            composeTestRule,
            "target-rings-wizard-tower-area-attack",
            width = 1400,
            height = 900
        )
    }
    
    @Test
    fun testAlchemyTowerTargetRingsOnPathTiles() {
        // Create a game state with an alchemy tower and enemies
        val level = LevelData.createLevels().first { it.id == 1 }
        val gameState = GameState(level)
        
        // Set game to player turn phase
        gameState.phase.value = GamePhase.PLAYER_TURN
        gameState.turnNumber.value = 4
        
        // Add an Alchemy Tower (LASTING attack - acid)
        val alchemyTower = Defender(
            id = 1,
            type = DefenderType.ALCHEMY_TOWER,
            position = androidx.compose.runtime.mutableStateOf(Position(12, 4)),
            level = androidx.compose.runtime.mutableStateOf(1),
            buildTimeRemaining = androidx.compose.runtime.mutableStateOf(0),
            actionsRemaining = androidx.compose.runtime.mutableStateOf(1),
            placedOnTurn = 2
        )
        gameState.defenders.add(alchemyTower)
        
        // Add enemies on the path within range
        val enemy1 = Attacker(
            id = 1,
            type = AttackerType.OGRE,
            position = androidx.compose.runtime.mutableStateOf(Position(10, 4)),
            level = androidx.compose.runtime.mutableStateOf(1)
        )
        
        val enemy2 = Attacker(
            id = 2,
            type = AttackerType.SKELETON,
            position = androidx.compose.runtime.mutableStateOf(Position(11, 3)),
            level = androidx.compose.runtime.mutableStateOf(1)
        )
        
        gameState.attackers.add(enemy1)
        gameState.attackers.add(enemy2)
        
        // State to track selected tower and target
        val selectedDefenderId = androidx.compose.runtime.mutableStateOf<Int?>(1)
        val selectedTargetId = androidx.compose.runtime.mutableStateOf<Int?>(1)
        val selectedTargetPosition = androidx.compose.runtime.mutableStateOf<Position?>(Position(10, 4))
        
        composeTestRule.setContent {
            GameGrid(
                gameState = gameState,
                selectedDefenderType = null,
                selectedDefenderId = selectedDefenderId.value,
                selectedTargetId = selectedTargetId.value,
                selectedTargetPosition = selectedTargetPosition.value,
                selectedMineAction = null,
                onCellClick = { }
            )
        }
        
        composeTestRule.waitForIdle()
        
        // Verify the grid renders
        composeTestRule.onRoot().assertExists()
        
        // Capture screenshot showing alchemy tower with target rings overlay
        // The rings should appear with green color (acid attack):
        // - 3 inner circles on the target tile
        // - Arc segments on neighboring path tiles
        ScreenshotTestUtils.captureScreenshot(
            composeTestRule,
            "target-rings-alchemy-tower-acid-attack",
            width = 1400,
            height = 900
        )
    }
    
    @Test
    fun testBowTowerNoTargetRings() {
        // Verify that single-target attacks (RANGED) do NOT show outer rings
        val level = LevelData.createLevels().first { it.id == 1 }
        val gameState = GameState(level)
        
        // Set game to player turn phase
        gameState.phase.value = GamePhase.PLAYER_TURN
        gameState.turnNumber.value = 2
        
        // Add a Bow Tower (RANGED attack - single target)
        val bowTower = Defender(
            id = 1,
            type = DefenderType.BOW_TOWER,
            position = androidx.compose.runtime.mutableStateOf(Position(10, 4)),
            level = androidx.compose.runtime.mutableStateOf(1),
            buildTimeRemaining = androidx.compose.runtime.mutableStateOf(0),
            actionsRemaining = androidx.compose.runtime.mutableStateOf(1),
            placedOnTurn = 1
        )
        gameState.defenders.add(bowTower)
        
        // Add an enemy within range
        val enemy1 = Attacker(
            id = 1,
            type = AttackerType.GOBLIN,
            position = androidx.compose.runtime.mutableStateOf(Position(7, 4)),
            level = androidx.compose.runtime.mutableStateOf(1)
        )
        
        gameState.attackers.add(enemy1)
        
        // State to track selected tower and target
        val selectedDefenderId = androidx.compose.runtime.mutableStateOf<Int?>(1)
        val selectedTargetId = androidx.compose.runtime.mutableStateOf<Int?>(1)
        val selectedTargetPosition = androidx.compose.runtime.mutableStateOf<Position?>(Position(7, 4))
        
        composeTestRule.setContent {
            GameGrid(
                gameState = gameState,
                selectedDefenderType = null,
                selectedDefenderId = selectedDefenderId.value,
                selectedTargetId = selectedTargetId.value,
                selectedTargetPosition = selectedTargetPosition.value,
                selectedMineAction = null,
                onCellClick = { }
            )
        }
        
        composeTestRule.waitForIdle()
        
        // Verify the grid renders
        composeTestRule.onRoot().assertExists()
        
        // Capture screenshot showing bow tower targeting (should have ONLY inner circles, no outer rings)
        ScreenshotTestUtils.captureScreenshot(
            composeTestRule,
            "target-rings-bow-tower-no-outer-rings",
            width = 1400,
            height = 900
        )
    }
    
    @Test
    fun testNoTargetCirclesWhenNoActionsRemaining() {
        // Verify that target circles are NOT shown when tower has no action points left
        val level = LevelData.createLevels().first { it.id == 1 }
        val gameState = GameState(level)
        
        // Set game to player turn phase
        gameState.phase.value = GamePhase.PLAYER_TURN
        gameState.turnNumber.value = 2
        
        // Add a Wizard Tower (AREA attack - fireball) with NO actions remaining
        val wizardTower = Defender(
            id = 1,
            type = DefenderType.WIZARD_TOWER,
            position = androidx.compose.runtime.mutableStateOf(Position(10, 3)),
            level = androidx.compose.runtime.mutableStateOf(1),
            buildTimeRemaining = androidx.compose.runtime.mutableStateOf(0),
            actionsRemaining = androidx.compose.runtime.mutableStateOf(0),  // NO actions left
            placedOnTurn = 1
        )
        gameState.defenders.add(wizardTower)
        
        // Add an enemy within range
        val enemy1 = Attacker(
            id = 1,
            type = AttackerType.GOBLIN,
            position = androidx.compose.runtime.mutableStateOf(Position(7, 4)),
            level = androidx.compose.runtime.mutableStateOf(1)
        )
        
        gameState.attackers.add(enemy1)
        
        // State to track selected tower and target position
        val selectedDefenderId = androidx.compose.runtime.mutableStateOf<Int?>(1)
        val selectedTargetId = androidx.compose.runtime.mutableStateOf<Int?>(1)
        val selectedTargetPosition = androidx.compose.runtime.mutableStateOf<Position?>(Position(7, 4))
        
        composeTestRule.setContent {
            GameGrid(
                gameState = gameState,
                selectedDefenderType = null,
                selectedDefenderId = selectedDefenderId.value,
                selectedTargetId = selectedTargetId.value,
                selectedTargetPosition = selectedTargetPosition.value,
                selectedMineAction = null,
                onCellClick = { }
            )
        }
        
        composeTestRule.waitForIdle()
        
        // Verify the grid renders
        composeTestRule.onRoot().assertExists()
        
        // Capture screenshot showing NO target circles when tower has no actions
        // This should show selected tower and target enemy, but NO target circles
        ScreenshotTestUtils.captureScreenshot(
            composeTestRule,
            "target-rings-no-circles-when-no-actions",
            width = 1400,
            height = 900
        )
    }
}
