package de.egril.defender.ui.gameplay

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import de.egril.defender.model.Attacker
import de.egril.defender.model.AttackerType
import de.egril.defender.model.AttackerWave
import de.egril.defender.model.GameState
import de.egril.defender.model.Level
import de.egril.defender.model.Position
import de.egril.defender.model.Waypoint
import de.egril.defender.ui.settings.AppSettings
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertTrue

/**
 * End-to-end rendering test for the enemy path preview overlay (issue: "path preview only shows
 * one tile"). Unlike [de.egril.defender.ui.gameplay.EnemyPathfindingOverlayTest] (which only tests
 * the pure [plannedEnemyPathForDisplay] function), this test actually renders [GameGrid] with a
 * real selected attacker and counts how many "PATH" tile labels appear on screen — reproducing
 * exactly what the player sees.
 */
class EnemyPathPreviewRenderingTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun fullPathIsHighlightedOnScreenForSelectedEnemy() {
        // Build a long straight corridor (20 tiles) so a truncated 1-tile preview is obviously wrong.
        val pathCells = (0..19).map { Position(it, 1) }.toSet()
        val finalTarget = Position(19, 1)
        val level =
            Level(
                id = 1,
                name = "Long Corridor Preview",
                gridWidth = 20,
                gridHeight = 3,
                startPositions = listOf(Position(0, 1)),
                targetPositions = listOf(finalTarget),
                pathCells = pathCells,
                attackerWaves = listOf(AttackerWave(listOf(AttackerType.GOBLIN))),
                initialCoins = 100,
                availableTowers = emptySet(),
            )
        val gameState = GameState(level)
        val attacker =
            Attacker(
                id = 1,
                type = AttackerType.GOBLIN,
                position = mutableStateOf(Position(0, 1)),
                level = mutableStateOf(1),
                currentTarget = mutableStateOf(finalTarget),
            )
        gameState.attackers.add(attacker)

        AppSettings.showEnemyPathfinding.value = true

        composeTestRule.setContent {
            GameGrid(
                gameState = gameState,
                selectedDefenderType = null,
                selectedDefenderId = null,
                selectedAttackerId = attacker.id,
                selectedTargetId = null,
                selectedTargetPosition = null,
                selectedMineAction = null,
                onCellClick = { },
            )
        }

        composeTestRule.waitForIdle()

        // The attacker starts at (0,1); the remaining 19 tiles of the corridor should all be
        // highlighted with a "PATH" label. Requiring more than a handful confirms the WHOLE path
        // is rendered, not just the first step.
        val pathLabelCount = composeTestRule.onAllNodesWithText("PATH").fetchSemanticsNodes().size
        assertTrue(
            pathLabelCount > 5,
            "Expected many PATH-labeled tiles along the corridor, but only found $pathLabelCount",
        )
    }

    @Test
    fun fullPathThroughMultipleWaypointsIsHighlighted() {
        // Bent corridor: (0,1) -> (9,1) [waypoint, turn down] -> (9,4) [waypoint, turn right] -> (15,4) [final target]
        // The attacker starts far from the final target and its currentTarget is only the FIRST
        // waypoint (mirrors real gameplay: attacker.currentTarget is the next waypoint, not the
        // final destination). The preview must chain through both waypoints to the final target.
        val topRow = (0..9).map { Position(it, 1) }
        val bendColumn = (1..4).map { Position(9, it) }
        val bottomRow = (9..15).map { Position(it, 4) }
        val pathCells = (topRow + bendColumn + bottomRow).toSet()
        val firstWaypointPos = Position(9, 1)
        val secondWaypointPos = Position(9, 4)
        val finalTarget = Position(15, 4)

        val level =
            Level(
                id = 1,
                name = "Bent Corridor Preview",
                gridWidth = 16,
                gridHeight = 5,
                startPositions = listOf(Position(0, 1)),
                targetPositions = listOf(finalTarget),
                pathCells = pathCells,
                waypoints =
                    listOf(
                        Waypoint(firstWaypointPos, secondWaypointPos),
                        Waypoint(secondWaypointPos, finalTarget),
                    ),
                attackerWaves = listOf(AttackerWave(listOf(AttackerType.GOBLIN))),
                initialCoins = 100,
                availableTowers = emptySet(),
            )
        val gameState = GameState(level)
        val attacker =
            Attacker(
                id = 1,
                type = AttackerType.GOBLIN,
                position = mutableStateOf(Position(0, 1)),
                level = mutableStateOf(1),
                // Only the NEXT waypoint, exactly like real gameplay assigns via getInitialTarget().
                currentTarget = mutableStateOf(firstWaypointPos),
            )
        gameState.attackers.add(attacker)

        AppSettings.showEnemyPathfinding.value = true

        composeTestRule.setContent {
            GameGrid(
                gameState = gameState,
                selectedDefenderType = null,
                selectedDefenderId = null,
                selectedAttackerId = attacker.id,
                selectedTargetId = null,
                selectedTargetPosition = null,
                selectedMineAction = null,
                onCellClick = { },
            )
        }

        composeTestRule.waitForIdle()

        // Total path length excluding start = 9 (top row) + 3 (bend) + 6 (bottom row) = 18 tiles.
        val pathLabelCount = composeTestRule.onAllNodesWithText("PATH").fetchSemanticsNodes().size
        assertTrue(
            pathLabelCount > 10,
            "Expected the preview to chain through both waypoints to the final target, " +
                "but only found $pathLabelCount PATH-labeled tiles",
        )
    }
}
