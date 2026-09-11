package de.egril.defender.ui.gameplay

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.Attacker
import de.egril.defender.model.AttackerType
import de.egril.defender.model.AttackerWave
import de.egril.defender.model.GameState
import de.egril.defender.model.Level
import de.egril.defender.model.Position
import de.egril.defender.ui.settings.AppSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class EnemyPathfindingOverlayTest {
    @Test
    fun plannedPathUsesCurrentEnemyTarget() {
        val pathCells = (0..8).map { Position(it, 1) }.toSet()
        val level =
            Level(
                id = 1,
                name = "Path Preview",
                gridWidth = 10,
                gridHeight = 3,
                startPositions = listOf(Position(0, 1)),
                targetPositions = listOf(Position(8, 1)),
                pathCells = pathCells,
                attackerWaves = listOf(AttackerWave(listOf(AttackerType.GOBLIN))),
                initialCoins = 100,
                availableTowers = emptySet(),
            )
        val gameState = GameState(level)
        val attacker =
            Attacker(
                1,
                AttackerType.GOBLIN,
                mutableStateOf(Position(0, 1)),
                mutableStateOf(1),
                currentTarget = mutableStateOf(Position(8, 1)),
            )
        gameState.attackers.add(attacker)

        val path = plannedEnemyPathForDisplay(gameState, attacker)

        assertNotNull(path)
        assertTrue(path.size > 2)
        assertEquals(Position(0, 1), path.first())
        assertEquals(Position(8, 1), path.last())
    }

    @Test
    fun plannedPathIncludesWholePathWhenWaypointTargetIsAlsoFinalTarget() {
        val pathCells =
            setOf(
                Position(0, 1),
                Position(1, 1),
                Position(2, 1),
                Position(3, 1),
                Position(4, 1),
            )
        val finalTarget = Position(4, 1)
        val level =
            Level(
                id = 1,
                name = "Direct Target Preview",
                gridWidth = 6,
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
                1,
                AttackerType.GOBLIN,
                mutableStateOf(Position(0, 1)),
                mutableStateOf(1),
                currentTarget = mutableStateOf(finalTarget),
            )
        gameState.attackers.add(attacker)

        val path = plannedEnemyPathForDisplay(gameState, attacker)

        assertNotNull(path)
        assertEquals(
            listOf(Position(0, 1), Position(1, 1), Position(2, 1), Position(3, 1), Position(4, 1)),
            path,
        )
    }

    @Test
    fun enemyPathOverlayToggleRecomputesFullPath() {
        val pathCells = (0..4).map { Position(it, 1) }.toSet()
        val finalTarget = Position(4, 1)
        val level =
            Level(
                id = 1,
                name = "Toggle Path Preview",
                gridWidth = 6,
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
                1,
                AttackerType.GOBLIN,
                mutableStateOf(Position(0, 1)),
                mutableStateOf(1),
                currentTarget = mutableStateOf(finalTarget),
            )
        gameState.attackers.add(attacker)

        AppSettings.showEnemyPathfinding.value = false
        val hiddenPath =
            if (!AppSettings.showEnemyPathfinding.value) {
                emptySet()
            } else {
                plannedEnemyPathForDisplay(gameState, attacker)?.drop(1)?.toSet() ?: emptySet()
            }

        AppSettings.showEnemyPathfinding.value = true
        val visiblePath =
            if (!AppSettings.showEnemyPathfinding.value) {
                emptySet()
            } else {
                plannedEnemyPathForDisplay(gameState, attacker)?.drop(1)?.toSet() ?: emptySet()
            }

        assertTrue(hiddenPath.isEmpty())
        assertEquals(
            setOf(Position(1, 1), Position(2, 1), Position(3, 1), Position(4, 1)),
            visiblePath,
        )
    }
}
