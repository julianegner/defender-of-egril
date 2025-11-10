package com.defenderofegril.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.defenderofegril.game.LevelData
import com.defenderofegril.model.GameState
import com.defenderofegril.model.GamePhase
import com.defenderofegril.ui.gameplay.GamePlayScreen
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for the Game Play screen.
 * 
 * These tests verify that the Game Play screen renders correctly
 * in different game states and captures screenshots for visual verification.
 */
class GamePlayScreenTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun testGamePlayScreenInitialState() {
        // Create a game state for testing - using level 1
        val level = LevelData.createLevels().first { it.id == 1 }
        val gameState = GameState(level)
        
        composeTestRule.setContent {
            GamePlayScreen(
                gameState = gameState,
                onPlaceDefender = { _, _ -> true },
                onUpgradeDefender = { true },
                onUndoTower = { true },
                onSellTower = { true },
                onStartFirstPlayerTurn = {},
                onDefenderAttack = { _, _ -> true },
                onDefenderAttackPosition = { _, _ -> true },
                onEndPlayerTurn = {},
                onBackToMap = {}
            )
        }
        
        composeTestRule.waitForIdle()
        
        // Verify the screen renders (basic check)
        composeTestRule.onRoot().assertExists()
        
        // Capture screenshot of initial state
        ScreenshotTestUtils.captureScreenshot(
            composeTestRule,
            "gameplay-screen-initial",
            width = 1400,
            height = 900
        )
    }
    
    @Test
    fun testGamePlayScreenWithEnemies() {
        // Create a game state with spawned enemies
        val level = LevelData.createLevels().first { it.id == 1 }
        val gameState = GameState(level)
        
        // Simulate starting the game to spawn enemies
        gameState.phase.value = GamePhase.PLAYER_TURN
        gameState.currentWaveIndex.value = 1
        
        composeTestRule.setContent {
            GamePlayScreen(
                gameState = gameState,
                onPlaceDefender = { _, _ -> true },
                onUpgradeDefender = { true },
                onUndoTower = { true },
                onSellTower = { true },
                onStartFirstPlayerTurn = {},
                onDefenderAttack = { _, _ -> true },
                onDefenderAttackPosition = { _, _ -> true },
                onEndPlayerTurn = {},
                onBackToMap = {}
            )
        }
        
        composeTestRule.waitForIdle()
        
        // Verify the screen renders
        composeTestRule.onRoot().assertExists()
        
        // Capture screenshot with game started
        ScreenshotTestUtils.captureScreenshot(
            composeTestRule,
            "gameplay-screen-with-enemies",
            width = 1400,
            height = 900
        )
    }
    
    @Test
    fun testGamePlayScreenInitialBuildingPhase() {
        // Create a game state in initial building phase
        val level = LevelData.createLevels().first { it.id == 1 }
        val gameState = GameState(level)
        // gameState.phase should be GamePhase.INITIAL_BUILDING by default
        
        composeTestRule.setContent {
            GamePlayScreen(
                gameState = gameState,
                onPlaceDefender = { _, _ -> true },
                onUpgradeDefender = { true },
                onUndoTower = { true },
                onSellTower = { true },
                onStartFirstPlayerTurn = {},
                onDefenderAttack = { _, _ -> true },
                onDefenderAttackPosition = { _, _ -> true },
                onEndPlayerTurn = {},
                onBackToMap = {}
            )
        }
        
        composeTestRule.waitForIdle()
        
        // Verify the screen renders
        composeTestRule.onRoot().assertExists()
        
        // Look for "Start Battle" or similar text that appears in initial phase
        // This text should be present in the initial building phase
        composeTestRule.onNodeWithText("Start", substring = true, ignoreCase = true)
            .assertExists()
        
        // Capture screenshot of building phase
        ScreenshotTestUtils.captureScreenshot(
            composeTestRule,
            "gameplay-screen-building-phase",
            width = 1400,
            height = 900
        )
    }
    
    @Test
    fun testGamePlayScreenRendersMap() {
        val level = LevelData.createLevels().first { it.id == 1 }
        val gameState = GameState(level)
        
        composeTestRule.setContent {
            GamePlayScreen(
                gameState = gameState,
                onPlaceDefender = { _, _ -> true },
                onUpgradeDefender = { true },
                onUndoTower = { true },
                onSellTower = { true },
                onStartFirstPlayerTurn = {},
                onDefenderAttack = { _, _ -> true },
                onDefenderAttackPosition = { _, _ -> true },
                onEndPlayerTurn = {},
                onBackToMap = {}
            )
        }
        
        composeTestRule.waitForIdle()
        
        // Verify the screen renders
        composeTestRule.onRoot().assertExists()
        
        // The game map should be present (it's always rendered)
        // We can verify this by checking that the root exists and has content
        composeTestRule.onRoot().assertIsDisplayed()
    }
    
    @Test
    fun testGamePlayScreenWithThreeTowersAtTurn3() {
        // Create a game state at turn 3 with 3 built towers
        val level = LevelData.createLevels().first { it.id == 1 }
        val gameState = GameState(level)
        
        // Set game to turn 3 and player turn phase
        gameState.phase.value = GamePhase.PLAYER_TURN
        gameState.turnNumber.value = 3
        
        // Add 3 towers at different positions
        // Tower 1: Spike Tower at position (8, 3)
        val tower1 = com.defenderofegril.model.Defender(
            id = 1,
            type = com.defenderofegril.model.DefenderType.SPIKE_TOWER,
            position = com.defenderofegril.model.Position(8, 3),
            level = androidx.compose.runtime.mutableStateOf(1),
            buildTimeRemaining = androidx.compose.runtime.mutableStateOf(0),
            actionsRemaining = androidx.compose.runtime.mutableStateOf(1),
            placedOnTurn = 1
        )
        
        // Tower 2: Bow Tower at position (12, 2)
        val tower2 = com.defenderofegril.model.Defender(
            id = 2,
            type = com.defenderofegril.model.DefenderType.BOW_TOWER,
            position = com.defenderofegril.model.Position(12, 2),
            level = androidx.compose.runtime.mutableStateOf(1),
            buildTimeRemaining = androidx.compose.runtime.mutableStateOf(0),
            actionsRemaining = androidx.compose.runtime.mutableStateOf(1),
            placedOnTurn = 1
        )
        
        // Tower 3: Spear Tower at position (16, 5)
        val tower3 = com.defenderofegril.model.Defender(
            id = 3,
            type = com.defenderofegril.model.DefenderType.SPEAR_TOWER,
            position = com.defenderofegril.model.Position(16, 5),
            level = androidx.compose.runtime.mutableStateOf(1),
            buildTimeRemaining = androidx.compose.runtime.mutableStateOf(0),
            actionsRemaining = androidx.compose.runtime.mutableStateOf(1),
            placedOnTurn = 2
        )
        
        // Add towers to game state
        gameState.defenders.add(tower1)
        gameState.defenders.add(tower2)
        gameState.defenders.add(tower3)
        
        // Add some enemies to make it more interesting
        val enemy1 = com.defenderofegril.model.Attacker(
            id = 1,
            type = com.defenderofegril.model.AttackerType.GOBLIN,
            position = androidx.compose.runtime.mutableStateOf(com.defenderofegril.model.Position(5, 4)),
            level = 1
        )
        gameState.attackers.add(enemy1)
        
        composeTestRule.setContent {
            GamePlayScreen(
                gameState = gameState,
                onPlaceDefender = { _, _ -> true },
                onUpgradeDefender = { true },
                onUndoTower = { true },
                onSellTower = { true },
                onStartFirstPlayerTurn = {},
                onDefenderAttack = { _, _ -> true },
                onDefenderAttackPosition = { _, _ -> true },
                onEndPlayerTurn = {},
                onBackToMap = {}
            )
        }
        
        composeTestRule.waitForIdle()
        
        // Verify the screen renders
        composeTestRule.onRoot().assertExists()
        
        // Capture screenshot with 3 towers at turn 3
        ScreenshotTestUtils.captureScreenshot(
            composeTestRule,
            "gameplay-screen-three-towers-turn3",
            width = 1400,
            height = 900
        )
    }
    
    @Test
    fun testGamePlayScreenWithBowTowerSelectedAndEnemyTargeted() {
        // Create a game state with a bow tower selected and an enemy targeted for attack
        val level = LevelData.createLevels().first { it.id == 1 }
        val gameState = GameState(level)
        
        // Set game to player turn phase
        gameState.phase.value = GamePhase.PLAYER_TURN
        gameState.turnNumber.value = 2
        
        // Add a Bow Tower
        val bowTower = com.defenderofegril.model.Defender(
            id = 1,
            type = com.defenderofegril.model.DefenderType.BOW_TOWER,
            position = com.defenderofegril.model.Position(10, 4),
            level = androidx.compose.runtime.mutableStateOf(1),
            buildTimeRemaining = androidx.compose.runtime.mutableStateOf(0),
            actionsRemaining = androidx.compose.runtime.mutableStateOf(1),
            placedOnTurn = 1
        )
        gameState.defenders.add(bowTower)
        
        // Add an enemy within range of the bow tower
        val enemy1 = com.defenderofegril.model.Attacker(
            id = 1,
            type = com.defenderofegril.model.AttackerType.GOBLIN,
            position = androidx.compose.runtime.mutableStateOf(com.defenderofegril.model.Position(7, 4)),
            level = 1
        )
        
        // Add a second enemy
        val enemy2 = com.defenderofegril.model.Attacker(
            id = 2,
            type = com.defenderofegril.model.AttackerType.ORK,
            position = androidx.compose.runtime.mutableStateOf(com.defenderofegril.model.Position(9, 3)),
            level = 1
        )
        
        gameState.attackers.add(enemy1)
        gameState.attackers.add(enemy2)
        
        composeTestRule.setContent {
            GamePlayScreen(
                gameState = gameState,
                onPlaceDefender = { _, _ -> true },
                onUpgradeDefender = { true },
                onUndoTower = { true },
                onSellTower = { true },
                onStartFirstPlayerTurn = {},
                onDefenderAttack = { _, _ -> true },
                onDefenderAttackPosition = { _, _ -> true },
                onEndPlayerTurn = {},
                onBackToMap = {}
            )
        }
        
        composeTestRule.waitForIdle()
        
        // Verify the screen renders
        composeTestRule.onRoot().assertExists()
        
        // Capture screenshot with bow tower ready to attack enemies
        ScreenshotTestUtils.captureScreenshot(
            composeTestRule,
            "gameplay-screen-bow-tower-with-enemies",
            width = 1400,
            height = 900
        )
    }
    
    @Test
    fun testGamePlayScreenWithWizardTowerTargetingPath() {
        // Create a game state with a wizard tower that can target a path tile (area attack)
        val level = LevelData.createLevels().first { it.id == 1 }
        val gameState = GameState(level)
        
        // Set game to player turn phase
        gameState.phase.value = GamePhase.PLAYER_TURN
        gameState.turnNumber.value = 3
        
        // Add a Wizard Tower (area attack with fireball)
        val wizardTower = com.defenderofegril.model.Defender(
            id = 1,
            type = com.defenderofegril.model.DefenderType.WIZARD_TOWER,
            position = com.defenderofegril.model.Position(10, 3),
            level = androidx.compose.runtime.mutableStateOf(1),
            buildTimeRemaining = androidx.compose.runtime.mutableStateOf(0),
            actionsRemaining = androidx.compose.runtime.mutableStateOf(1),
            placedOnTurn = 1
        )
        gameState.defenders.add(wizardTower)
        
        // Add multiple enemies in a cluster for area effect
        val enemy1 = com.defenderofegril.model.Attacker(
            id = 1,
            type = com.defenderofegril.model.AttackerType.GOBLIN,
            position = androidx.compose.runtime.mutableStateOf(com.defenderofegril.model.Position(7, 4)),
            level = 1
        )
        
        val enemy2 = com.defenderofegril.model.Attacker(
            id = 2,
            type = com.defenderofegril.model.AttackerType.GOBLIN,
            position = androidx.compose.runtime.mutableStateOf(com.defenderofegril.model.Position(8, 4)),
            level = 1
        )
        
        val enemy3 = com.defenderofegril.model.Attacker(
            id = 3,
            type = com.defenderofegril.model.AttackerType.ORK,
            position = androidx.compose.runtime.mutableStateOf(com.defenderofegril.model.Position(7, 3)),
            level = 1
        )
        
        gameState.attackers.add(enemy1)
        gameState.attackers.add(enemy2)
        gameState.attackers.add(enemy3)
        
        composeTestRule.setContent {
            GamePlayScreen(
                gameState = gameState,
                onPlaceDefender = { _, _ -> true },
                onUpgradeDefender = { true },
                onUndoTower = { true },
                onSellTower = { true },
                onStartFirstPlayerTurn = {},
                onDefenderAttack = { _, _ -> true },
                onDefenderAttackPosition = { _, _ -> true },
                onEndPlayerTurn = {},
                onBackToMap = {}
            )
        }
        
        composeTestRule.waitForIdle()
        
        // Verify the screen renders
        composeTestRule.onRoot().assertExists()
        
        // Capture screenshot with wizard tower ready for area attack
        ScreenshotTestUtils.captureScreenshot(
            composeTestRule,
            "gameplay-screen-wizard-tower-area-attack",
            width = 1400,
            height = 900
        )
    }
    
    @Test
    fun testGamePlayScreenWithAlchemyTowerTargetingPath() {
        // Create a game state with an alchemy tower that can target a path tile (acid/DoT attack)
        val level = LevelData.createLevels().first { it.id == 1 }
        val gameState = GameState(level)
        
        // Set game to player turn phase
        gameState.phase.value = GamePhase.PLAYER_TURN
        gameState.turnNumber.value = 4
        
        // Add an Alchemy Tower (acid/lasting damage)
        val alchemyTower = com.defenderofegril.model.Defender(
            id = 1,
            type = com.defenderofegril.model.DefenderType.ALCHEMY_TOWER,
            position = com.defenderofegril.model.Position(12, 4),
            level = androidx.compose.runtime.mutableStateOf(1),
            buildTimeRemaining = androidx.compose.runtime.mutableStateOf(0),
            actionsRemaining = androidx.compose.runtime.mutableStateOf(1),
            placedOnTurn = 2
        )
        gameState.defenders.add(alchemyTower)
        
        // Add enemies on the path within range
        val enemy1 = com.defenderofegril.model.Attacker(
            id = 1,
            type = com.defenderofegril.model.AttackerType.OGRE,
            position = androidx.compose.runtime.mutableStateOf(com.defenderofegril.model.Position(10, 4)),
            level = 1
        )
        
        val enemy2 = com.defenderofegril.model.Attacker(
            id = 2,
            type = com.defenderofegril.model.AttackerType.SKELETON,
            position = androidx.compose.runtime.mutableStateOf(com.defenderofegril.model.Position(11, 3)),
            level = 1
        )
        
        // Add an enemy that already has acid damage (to show DoT effect)
        val enemy3 = com.defenderofegril.model.Attacker(
            id = 3,
            type = com.defenderofegril.model.AttackerType.GOBLIN,
            position = androidx.compose.runtime.mutableStateOf(com.defenderofegril.model.Position(9, 4)),
            level = 1,
            currentHealth = androidx.compose.runtime.mutableStateOf(15) // Reduced health to show damage
        )
        
        gameState.attackers.add(enemy1)
        gameState.attackers.add(enemy2)
        gameState.attackers.add(enemy3)
        
        composeTestRule.setContent {
            GamePlayScreen(
                gameState = gameState,
                onPlaceDefender = { _, _ -> true },
                onUpgradeDefender = { true },
                onUndoTower = { true },
                onSellTower = { true },
                onStartFirstPlayerTurn = {},
                onDefenderAttack = { _, _ -> true },
                onDefenderAttackPosition = { _, _ -> true },
                onEndPlayerTurn = {},
                onBackToMap = {}
            )
        }
        
        composeTestRule.waitForIdle()
        
        // Verify the screen renders
        composeTestRule.onRoot().assertExists()
        
        // Capture screenshot with alchemy tower ready for acid attack
        ScreenshotTestUtils.captureScreenshot(
            composeTestRule,
            "gameplay-screen-alchemy-tower-acid-attack",
            width = 1400,
            height = 900
        )
    }
    
    @Test
    fun testGamePlayScreenWithDwarvenMineActions() {
        // Create a game state with a dwarven mine that has placed a trap and can dig for coins
        val level = LevelData.createLevels().first { it.id == 1 }
        val gameState = GameState(level)
        
        // Set game to player turn phase
        gameState.phase.value = GamePhase.PLAYER_TURN
        gameState.turnNumber.value = 5
        
        // Add a Dwarven Mine
        val dwarvenMine = com.defenderofegril.model.Defender(
            id = 1,
            type = com.defenderofegril.model.DefenderType.DWARVEN_MINE,
            position = com.defenderofegril.model.Position(9, 5),
            level = androidx.compose.runtime.mutableStateOf(1),
            buildTimeRemaining = androidx.compose.runtime.mutableStateOf(0),
            actionsRemaining = androidx.compose.runtime.mutableStateOf(2), // Mines have special actions
            placedOnTurn = 2,
            coinsGenerated = androidx.compose.runtime.mutableStateOf(20) // Show it has generated coins
        )
        gameState.defenders.add(dwarvenMine)
        
        // Add a trap placed by the mine on the path
        val trap = com.defenderofegril.model.Trap(
            position = com.defenderofegril.model.Position(7, 4),
            damage = 10,
            mineId = 1
        )
        gameState.traps.add(trap)
        
        // Add some other towers to show a developed game state
        val spearTower = com.defenderofegril.model.Defender(
            id = 2,
            type = com.defenderofegril.model.DefenderType.SPEAR_TOWER,
            position = com.defenderofegril.model.Position(11, 3),
            level = androidx.compose.runtime.mutableStateOf(1),
            buildTimeRemaining = androidx.compose.runtime.mutableStateOf(0),
            actionsRemaining = androidx.compose.runtime.mutableStateOf(1),
            placedOnTurn = 1
        )
        gameState.defenders.add(spearTower)
        
        // Add enemies
        val enemy1 = com.defenderofegril.model.Attacker(
            id = 1,
            type = com.defenderofegril.model.AttackerType.GOBLIN,
            position = androidx.compose.runtime.mutableStateOf(com.defenderofegril.model.Position(5, 4)),
            level = 1
        )
        
        val enemy2 = com.defenderofegril.model.Attacker(
            id = 2,
            type = com.defenderofegril.model.AttackerType.ORK,
            position = androidx.compose.runtime.mutableStateOf(com.defenderofegril.model.Position(6, 3)),
            level = 1
        )
        
        gameState.attackers.add(enemy1)
        gameState.attackers.add(enemy2)
        
        composeTestRule.setContent {
            GamePlayScreen(
                gameState = gameState,
                onPlaceDefender = { _, _ -> true },
                onUpgradeDefender = { true },
                onUndoTower = { true },
                onSellTower = { true },
                onStartFirstPlayerTurn = {},
                onDefenderAttack = { _, _ -> true },
                onDefenderAttackPosition = { _, _ -> true },
                onEndPlayerTurn = {},
                onBackToMap = {},
                onMineDig = { com.defenderofegril.model.DigOutcome.GOLD } // Simulate dig returning GOLD
            )
        }
        
        composeTestRule.waitForIdle()
        
        // Verify the screen renders
        composeTestRule.onRoot().assertExists()
        
        // Capture screenshot with dwarven mine showing both actions
        ScreenshotTestUtils.captureScreenshot(
            composeTestRule,
            "gameplay-screen-dwarven-mine-with-actions",
            width = 1400,
            height = 900
        )
    }
}
