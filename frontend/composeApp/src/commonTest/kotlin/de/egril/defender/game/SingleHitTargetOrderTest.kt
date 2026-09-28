package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.Attacker
import de.egril.defender.model.AttackerType
import de.egril.defender.model.AttackerWave
import de.egril.defender.model.GameState
import de.egril.defender.model.Level
import de.egril.defender.model.Position
import de.egril.defender.model.TargetInfo
import de.egril.defender.model.TargetType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SingleHitTargetOrderTest {
    @Test
    fun onlyTheNextUnclaimedSingleHitTargetIsActive() {
        val firstTarget = Position(4, 2)
        val secondTarget = Position(8, 2)
        val level =
            Level(
                id = 1,
                name = "Ordered targets",
                targetPositions = listOf(firstTarget, secondTarget),
                pathCells = emptySet(),
                attackerWaves = listOf(AttackerWave(emptyList())),
                targetInfoMap =
                    mapOf(
                        firstTarget to TargetInfo(type = TargetType.SINGLE_HIT),
                        secondTarget to TargetInfo(type = TargetType.SINGLE_HIT),
                    ),
                singleHitTargetOrder = listOf(secondTarget, firstTarget),
            )
        val state = GameState(level)

        assertEquals(secondTarget, state.getNextSingleHitTargetPosition())
        assertEquals(listOf(secondTarget), state.getActiveTargetPositions())
        assertFalse(state.isActiveTargetPosition(firstTarget))
        assertTrue(state.isActiveTargetPosition(secondTarget))

        state.takenTargets.add(secondTarget)

        assertEquals(firstTarget, state.getNextSingleHitTargetPosition())
        assertEquals(listOf(firstTarget), state.getActiveTargetPositions())
        assertTrue(state.isActiveTargetPosition(firstTarget))
        assertFalse(state.isActiveTargetPosition(secondTarget))
    }

    @Test
    fun noOrderKeepsAllUntakenTargetsActive() {
        val firstTarget = Position(4, 2)
        val secondTarget = Position(8, 2)
        val state =
            GameState(
                Level(
                    id = 1,
                    name = "Unordered targets",
                    targetPositions = listOf(firstTarget, secondTarget),
                    pathCells = emptySet(),
                    attackerWaves = listOf(AttackerWave(emptyList())),
                ),
            )

        assertEquals(listOf(firstTarget, secondTarget), state.getActiveTargetPositions())
        assertTrue(state.isActiveTargetPosition(firstTarget))
        assertTrue(state.isActiveTargetPosition(secondTarget))
    }

    @Test
    fun enemyPassesEarlierTargetWithoutCapturingIt() {
        val firstTarget = Position(4, 2)
        val secondTarget = Position(8, 2)
        val path = (0..7).map { Position(it, 2) }.toSet()
        val level =
            Level(
                id = 1,
                name = "Ordered target movement",
                gridWidth = 10,
                gridHeight = 5,
                startPositions = listOf(Position(0, 2)),
                targetPositions = listOf(firstTarget, secondTarget),
                pathCells = path,
                attackerWaves = emptyList(),
                targetInfoMap =
                    mapOf(
                        firstTarget to TargetInfo(type = TargetType.SINGLE_HIT),
                        secondTarget to TargetInfo(type = TargetType.SINGLE_HIT),
                    ),
                singleHitTargetOrder = listOf(secondTarget, firstTarget),
            )
        val state = GameState(level)
        val enemy =
            Attacker(
                id = 1,
                type = AttackerType.GOBLIN,
                position = mutableStateOf(Position(0, 2)),
                level = mutableStateOf(1),
            )
        state.attackers.add(enemy)
        val engine = GameEngine(state)

        for (turn in 0 until 10) {
            if (state.takenTargets.isNotEmpty()) break
            engine.calculateEnemyTurnMovements()
                .allMovementSteps
                .flatten()
                .forEach { (attackerId, position) -> engine.applyMovement(attackerId, position) }
        }

        assertEquals(listOf(secondTarget), state.takenTargets)
    }
}
