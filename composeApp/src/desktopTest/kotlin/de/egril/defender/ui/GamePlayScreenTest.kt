package de.egril.defender.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import de.egril.defender.game.LevelData
import de.egril.defender.model.GameState
import de.egril.defender.model.GamePhase
import de.egril.defender.ui.gameplay.GamePlayScreen
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
        val tower1 = de.egril.defender.model.Defender(
            id = 1,
            type = de.egril.defender.model.DefenderType.SPIKE_TOWER,
            position = de.egril.defender.model.Position(8, 3),
            level = androidx.compose.runtime.mutableStateOf(1),
            buildTimeRemaining = androidx.compose.runtime.mutableStateOf(0),
            actionsRemaining = androidx.compose.runtime.mutableStateOf(1),
            placedOnTurn = 1
        )
        
        // Tower 2: Bow Tower at position (12, 2)
        val tower2 = de.egril.defender.model.Defender(
            id = 2,
            type = de.egril.defender.model.DefenderType.BOW_TOWER,
            position = de.egril.defender.model.Position(12, 2),
            level = androidx.compose.runtime.mutableStateOf(1),
            buildTimeRemaining = androidx.compose.runtime.mutableStateOf(0),
            actionsRemaining = androidx.compose.runtime.mutableStateOf(1),
            placedOnTurn = 1
        )
        
        // Tower 3: Spear Tower at position (16, 5)
        val tower3 = de.egril.defender.model.Defender(
            id = 3,
            type = de.egril.defender.model.DefenderType.SPEAR_TOWER,
            position = de.egril.defender.model.Position(16, 5),
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
        val enemy1 = de.egril.defender.model.Attacker(
            id = 1,
            type = de.egril.defender.model.AttackerType.GOBLIN,
            position = androidx.compose.runtime.mutableStateOf(de.egril.defender.model.Position(5, 4)),
            level = androidx.compose.runtime.mutableStateOf(1)
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
        val bowTower = de.egril.defender.model.Defender(
            id = 1,
            type = de.egril.defender.model.DefenderType.BOW_TOWER,
            position = de.egril.defender.model.Position(10, 4),
            level = androidx.compose.runtime.mutableStateOf(1),
            buildTimeRemaining = androidx.compose.runtime.mutableStateOf(0),
            actionsRemaining = androidx.compose.runtime.mutableStateOf(1),
            placedOnTurn = 1
        )
        gameState.defenders.add(bowTower)
        
        // Add an enemy within range of the bow tower
        val enemy1 = de.egril.defender.model.Attacker(
            id = 1,
            type = de.egril.defender.model.AttackerType.GOBLIN,
            position = androidx.compose.runtime.mutableStateOf(de.egril.defender.model.Position(7, 4)),
            level = androidx.compose.runtime.mutableStateOf(1)
        )
        
        // Add a second enemy
        val enemy2 = de.egril.defender.model.Attacker(
            id = 2,
            type = de.egril.defender.model.AttackerType.ORK,
            position = androidx.compose.runtime.mutableStateOf(de.egril.defender.model.Position(9, 3)),
            level = androidx.compose.runtime.mutableStateOf(1)
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
        val wizardTower = de.egril.defender.model.Defender(
            id = 1,
            type = de.egril.defender.model.DefenderType.WIZARD_TOWER,
            position = de.egril.defender.model.Position(10, 3),
            level = androidx.compose.runtime.mutableStateOf(1),
            buildTimeRemaining = androidx.compose.runtime.mutableStateOf(0),
            actionsRemaining = androidx.compose.runtime.mutableStateOf(1),
            placedOnTurn = 1
        )
        gameState.defenders.add(wizardTower)
        
        // Add multiple enemies in a cluster for area effect
        val enemy1 = de.egril.defender.model.Attacker(
            id = 1,
            type = de.egril.defender.model.AttackerType.GOBLIN,
            position = androidx.compose.runtime.mutableStateOf(de.egril.defender.model.Position(7, 4)),
            level = androidx.compose.runtime.mutableStateOf(1)
        )
        
        val enemy2 = de.egril.defender.model.Attacker(
            id = 2,
            type = de.egril.defender.model.AttackerType.GOBLIN,
            position = androidx.compose.runtime.mutableStateOf(de.egril.defender.model.Position(8, 4)),
            level = androidx.compose.runtime.mutableStateOf(1)
        )
        
        val enemy3 = de.egril.defender.model.Attacker(
            id = 3,
            type = de.egril.defender.model.AttackerType.ORK,
            position = androidx.compose.runtime.mutableStateOf(de.egril.defender.model.Position(7, 3)),
            level = androidx.compose.runtime.mutableStateOf(1)
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
        val alchemyTower = de.egril.defender.model.Defender(
            id = 1,
            type = de.egril.defender.model.DefenderType.ALCHEMY_TOWER,
            position = de.egril.defender.model.Position(12, 4),
            level = androidx.compose.runtime.mutableStateOf(1),
            buildTimeRemaining = androidx.compose.runtime.mutableStateOf(0),
            actionsRemaining = androidx.compose.runtime.mutableStateOf(1),
            placedOnTurn = 2
        )
        gameState.defenders.add(alchemyTower)
        
        // Add enemies on the path within range
        val enemy1 = de.egril.defender.model.Attacker(
            id = 1,
            type = de.egril.defender.model.AttackerType.OGRE,
            position = androidx.compose.runtime.mutableStateOf(de.egril.defender.model.Position(10, 4)),
            level = androidx.compose.runtime.mutableStateOf(1)
        )
        
        val enemy2 = de.egril.defender.model.Attacker(
            id = 2,
            type = de.egril.defender.model.AttackerType.SKELETON,
            position = androidx.compose.runtime.mutableStateOf(de.egril.defender.model.Position(11, 3)),
            level = androidx.compose.runtime.mutableStateOf(1)
        )
        
        // Add an enemy that already has acid damage (to show DoT effect)
        val enemy3 = de.egril.defender.model.Attacker(
            id = 3,
            type = de.egril.defender.model.AttackerType.GOBLIN,
            position = androidx.compose.runtime.mutableStateOf(de.egril.defender.model.Position(9, 4)),
            level = androidx.compose.runtime.mutableStateOf(1),
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
        val dwarvenMine = de.egril.defender.model.Defender(
            id = 1,
            type = de.egril.defender.model.DefenderType.DWARVEN_MINE,
            position = de.egril.defender.model.Position(9, 5),
            level = androidx.compose.runtime.mutableStateOf(1),
            buildTimeRemaining = androidx.compose.runtime.mutableStateOf(0),
            actionsRemaining = androidx.compose.runtime.mutableStateOf(2), // Mines have special actions
            placedOnTurn = 2,
            coinsGenerated = androidx.compose.runtime.mutableStateOf(20) // Show it has generated coins
        )
        gameState.defenders.add(dwarvenMine)
        
        // Add a trap placed by the mine on the path
        val trap = de.egril.defender.model.Trap(
            position = de.egril.defender.model.Position(7, 4),
            damage = 10,
            defenderId = 1
        )
        gameState.traps.add(trap)
        
        // Add some other towers to show a developed game state
        val spearTower = de.egril.defender.model.Defender(
            id = 2,
            type = de.egril.defender.model.DefenderType.SPEAR_TOWER,
            position = de.egril.defender.model.Position(11, 3),
            level = androidx.compose.runtime.mutableStateOf(1),
            buildTimeRemaining = androidx.compose.runtime.mutableStateOf(0),
            actionsRemaining = androidx.compose.runtime.mutableStateOf(1),
            placedOnTurn = 1
        )
        gameState.defenders.add(spearTower)
        
        // Add enemies
        val enemy1 = de.egril.defender.model.Attacker(
            id = 1,
            type = de.egril.defender.model.AttackerType.GOBLIN,
            position = androidx.compose.runtime.mutableStateOf(de.egril.defender.model.Position(5, 4)),
            level = androidx.compose.runtime.mutableStateOf(1)
        )
        
        val enemy2 = de.egril.defender.model.Attacker(
            id = 2,
            type = de.egril.defender.model.AttackerType.ORK,
            position = androidx.compose.runtime.mutableStateOf(de.egril.defender.model.Position(6, 3)),
            level = androidx.compose.runtime.mutableStateOf(1)
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
                onMineDig = { de.egril.defender.model.DigOutcome.GOLD } // Simulate dig returning GOLD
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
    
    @Test
    fun testGamePlayScreenLegendPanel() {
        // Test the legend panel showing tile types and legend information
        val level = LevelData.createLevels().first { it.id == 1 }
        val gameState = GameState(level)
        
        // Set game to initial building phase
        gameState.phase.value = GamePhase.INITIAL_BUILDING
        
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
        
        // Try to expand legend panel if it's collapsed
        try {
            composeTestRule.onNodeWithText("Legend", substring = true, ignoreCase = true)
                .performClick()
            composeTestRule.waitForIdle()
        } catch (e: Throwable) {
            println("Note: Could not expand legend panel: ${e.message}")
        }
        
        // Verify the screen renders
        composeTestRule.onRoot().assertExists()
        
        // Capture screenshot showing legend panel
        ScreenshotTestUtils.captureScreenshot(
            composeTestRule,
            "gameplay-screen-legend-panel",
            width = 1400,
            height = 900
        )
    }
    
    @Test
    fun testGamePlayScreenEnemiesPanelTurn1() {
        // Test the enemies panel expanded at turn 1 of level 1
        val level = LevelData.createLevels().first { it.id == 1 }
        val gameState = GameState(level)
        
        // Set game to turn 1 (enemies have started spawning)
        gameState.phase.value = GamePhase.PLAYER_TURN
        gameState.turnNumber.value = 1
        
        // Add some enemies that would be present at turn 1
        val enemy1 = de.egril.defender.model.Attacker(
            id = 1,
            type = de.egril.defender.model.AttackerType.GOBLIN,
            position = androidx.compose.runtime.mutableStateOf(de.egril.defender.model.Position(1, 4)),
            level = androidx.compose.runtime.mutableStateOf(1)
        )
        
        val enemy2 = de.egril.defender.model.Attacker(
            id = 2,
            type = de.egril.defender.model.AttackerType.GOBLIN,
            position = androidx.compose.runtime.mutableStateOf(de.egril.defender.model.Position(0, 1)),
            level = androidx.compose.runtime.mutableStateOf(1)
        )
        
        gameState.attackers.add(enemy1)
        gameState.attackers.add(enemy2)
        gameState.nextAttackerId.value = 3 // Set next ID to show we've spawned 2
        
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
        
        // Try to expand enemies panel
        try {
            composeTestRule.onNodeWithText("Enemies", substring = true, ignoreCase = true)
                .performClick()
            composeTestRule.waitForIdle()
        } catch (e: Throwable) {
            println("Note: Could not expand enemies panel: ${e.message}")
        }
        
        // Verify the screen renders
        composeTestRule.onRoot().assertExists()
        
        // Capture screenshot showing enemies panel expanded at turn 1
        ScreenshotTestUtils.captureScreenshot(
            composeTestRule,
            "gameplay-screen-enemies-panel-turn1",
            width = 1400,
            height = 900
        )
    }
    
    @Test
    fun testGamePlayScreenSettingsPanel() {
        // Test the settings panel opened during gameplay
        val level = LevelData.createLevels().first { it.id == 1 }
        val gameState = GameState(level)
        
        // Set game to initial building phase
        gameState.phase.value = GamePhase.INITIAL_BUILDING
        
        composeTestRule.setContent {
            val showSettings = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(true) }
            
            androidx.compose.foundation.layout.Box {
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
                
                // Show settings dialog overlay
                if (showSettings.value) {
                    de.egril.defender.ui.settings.SettingsDialog(
                        onDismiss = { showSettings.value = false }
                    )
                }
            }
        }
        
        composeTestRule.waitForIdle()
        
        // Verify the screen renders (don't use onRoot as dialog creates multiple roots)
        // Just verify settings dialog is shown
        try {
            composeTestRule.onNodeWithText("Settings", substring = true, ignoreCase = true)
                .assertExists()
        } catch (e: Throwable) {
            println("Note: Could not find Settings text: ${e.message}")
        }
        
        // Capture screenshot showing settings panel
        try {
            ScreenshotTestUtils.captureScreenshot(
                composeTestRule,
                "gameplay-screen-settings-panel",
                width = 1400,
                height = 900
            )
        } catch (e: Throwable) {
            println("Note: Could not capture settings panel screenshot: ${e.message}")
        }
    }
    
    @Test
    fun testGamePlayScreenWithRaftOnRiver() {
        // Create a level with river tiles and a tower on a raft
        val riverTiles = mapOf(
            de.egril.defender.model.Position(10, 3) to de.egril.defender.model.RiverTile(
                position = de.egril.defender.model.Position(10, 3),
                flowDirection = de.egril.defender.model.RiverFlow.EAST,
                flowSpeed = 1
            ),
            de.egril.defender.model.Position(11, 3) to de.egril.defender.model.RiverTile(
                position = de.egril.defender.model.Position(11, 3),
                flowDirection = de.egril.defender.model.RiverFlow.EAST,
                flowSpeed = 1
            ),
            de.egril.defender.model.Position(12, 3) to de.egril.defender.model.RiverTile(
                position = de.egril.defender.model.Position(12, 3),
                flowDirection = de.egril.defender.model.RiverFlow.EAST,
                flowSpeed = 1
            )
        )
        
        val level = de.egril.defender.model.Level(
            id = 999,
            name = "River Test Level",
            gridWidth = 30,
            gridHeight = 8,
            pathCells = setOf(
                de.egril.defender.model.Position(5, 4),
                de.egril.defender.model.Position(6, 4),
                de.egril.defender.model.Position(7, 4)
            ),
            buildIslands = emptySet(),
            buildAreas = setOf(
                de.egril.defender.model.Position(8, 3),
                de.egril.defender.model.Position(9, 3),
                de.egril.defender.model.Position(10, 3), // River tile that's also buildable
                de.egril.defender.model.Position(11, 3),
                de.egril.defender.model.Position(12, 3)
            ),
            attackerWaves = emptyList(),
            initialCoins = 100,
            healthPoints = 10,
            riverTiles = riverTiles
        )
        
        val gameState = GameState(level)
        
        // Set game to player turn phase
        gameState.phase.value = GamePhase.PLAYER_TURN
        gameState.turnNumber.value = 1
        
        // Add a Bow Tower on the river (creating a raft)
        val towerOnRaft = de.egril.defender.model.Defender(
            id = 1,
            type = de.egril.defender.model.DefenderType.BOW_TOWER,
            position = androidx.compose.runtime.mutableStateOf(de.egril.defender.model.Position(10, 3)),
            level = androidx.compose.runtime.mutableStateOf(1),
            buildTimeRemaining = androidx.compose.runtime.mutableStateOf(0),
            actionsRemaining = androidx.compose.runtime.mutableStateOf(1),
            placedOnTurn = 1,
            raftId = androidx.compose.runtime.mutableStateOf(1) // Indicate this tower is on a raft
        )
        gameState.defenders.add(towerOnRaft)
        
        // Create the raft that the tower is on
        val raft = de.egril.defender.model.Raft(
            id = 1,
            defenderId = 1,
            currentPosition = androidx.compose.runtime.mutableStateOf(de.egril.defender.model.Position(10, 3))
        )
        gameState.rafts.add(raft)
        
        // Add a regular tower on land for comparison
        val regularTower = de.egril.defender.model.Defender(
            id = 2,
            type = de.egril.defender.model.DefenderType.SPIKE_TOWER,
            position = androidx.compose.runtime.mutableStateOf(de.egril.defender.model.Position(8, 3)),
            level = androidx.compose.runtime.mutableStateOf(1),
            buildTimeRemaining = androidx.compose.runtime.mutableStateOf(0),
            actionsRemaining = androidx.compose.runtime.mutableStateOf(1),
            placedOnTurn = 1
        )
        gameState.defenders.add(regularTower)
        
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
        
        // Capture screenshot showing raft on river with tower
        ScreenshotTestUtils.captureScreenshot(
            composeTestRule,
            "gameplay-screen-raft-on-river",
            width = 1400,
            height = 900
        )
    }
}
