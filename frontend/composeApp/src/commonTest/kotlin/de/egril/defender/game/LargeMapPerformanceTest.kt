package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.Attacker
import de.egril.defender.model.AttackerType
import de.egril.defender.model.Defender
import de.egril.defender.model.DefenderType
import de.egril.defender.model.GameState
import de.egril.defender.model.Level
import de.egril.defender.model.Position
import de.egril.defender.utils.isPlatformDesktop
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.TimeSource

/**
 * Performance regression tests for very large maps that consist almost entirely of path tiles
 * (issue #791). Such maps defeat both the NO_PLAY shortcut and the "only render visible tiles"
 * optimisation, so the remaining hot spots are the pathfinding based computations that run
 * whenever the player places a tower or a tower attacks.
 *
 * The budgets are intentionally generous (they only have to catch multi-second regressions),
 * and the tests only run on desktop where timings are stable enough to be meaningful.
 */
class LargeMapPerformanceTest {
    private companion object {
        const val GRID_WIDTH = 40
        const val GRID_HEIGHT = 60
        const val ATTACKER_COUNT = 45
        const val DEFENDER_COUNT = 40

        /** Budget for recomputing the "enemy reaches the target next turn" hints for all enemies. */
        const val DANGER_HINT_BUDGET_MS = 250.0

        /** Budget for a full enemy movement pathfinding pass over all enemies. */
        const val PATHFINDING_BUDGET_MS = 1500.0
    }

    /**
     * Builds a 40x60 map where every tile is a path tile, apart from a column of build areas
     * that hold the towers. Three spawn points and three targets, just like the level from the
     * issue report.
     */
    private fun createLargeAllPathState(): GameState {
        val buildColumn = GRID_WIDTH / 2
        val pathCells = mutableSetOf<Position>()
        val buildAreas = mutableSetOf<Position>()
        for (y in 0 until GRID_HEIGHT) {
            for (x in 0 until GRID_WIDTH) {
                val position = Position(x, y)
                if (x == buildColumn && y % 3 == 0) {
                    buildAreas.add(position)
                } else {
                    pathCells.add(position)
                }
            }
        }
        val spawnPoints = listOf(Position(0, 0), Position(0, GRID_HEIGHT / 2), Position(0, GRID_HEIGHT - 1))
        val targets =
            listOf(
                Position(GRID_WIDTH - 1, 0),
                Position(GRID_WIDTH - 1, GRID_HEIGHT / 2),
                Position(GRID_WIDTH - 1, GRID_HEIGHT - 1),
            )
        val level =
            Level(
                id = 791,
                name = "The barricades (performance)",
                gridWidth = GRID_WIDTH,
                gridHeight = GRID_HEIGHT,
                startPositions = spawnPoints,
                targetPositions = targets,
                pathCells = pathCells - buildAreas,
                buildAreas = buildAreas,
                attackerWaves = emptyList(),
                initialCoins = 1000,
                healthPoints = 20,
            )

        val state = GameState(level)
        val buildPositions = buildAreas.sortedWith(compareBy({ it.y }, { it.x }))
        buildPositions.take(DEFENDER_COUNT).forEachIndexed { index, position ->
            state.defenders.add(
                Defender(
                    id = index + 1,
                    type = if (index % 2 == 0) DefenderType.BOW_TOWER else DefenderType.WIZARD_TOWER,
                    position = mutableStateOf(position),
                    level = mutableStateOf(20),
                    buildTimeRemaining = mutableStateOf(0),
                ),
            )
        }
        repeat(ATTACKER_COUNT) { index ->
            state.attackers.add(
                Attacker(
                    id = index + 1,
                    type = AttackerType.SNOTLING,
                    position = mutableStateOf(Position(1 + index % 6, (index * 7) % GRID_HEIGHT)),
                    level = mutableStateOf(20),
                ),
            )
        }
        return state
    }

    @Test
    fun dangerHintsForAllEnemiesStayFastOnLargeAllPathMap() {
        if (!isPlatformDesktop) return

        val state = createLargeAllPathState()
        val movementSystem = EnemyMovementSystem(state, PathfindingSystem(state))

        // Warm-up so JIT compilation is not part of the measurement.
        repeat(2) { state.attackers.forEach { movementSystem.canReachTargetNextTurn(it) } }

        val elapsedMs = measureMillis { state.attackers.forEach { movementSystem.canReachTargetNextTurn(it) } }
        println("Danger hint recomputation for $ATTACKER_COUNT enemies on ${GRID_WIDTH}x$GRID_HEIGHT: $elapsedMs ms")

        assertTrue(
            elapsedMs < DANGER_HINT_BUDGET_MS,
            "Recomputing danger hints for all enemies took $elapsedMs ms " +
                "(budget ${DANGER_HINT_BUDGET_MS} ms). This runs on every tower placement and attack.",
        )
    }

    @Test
    fun pathfindingForAllEnemiesStaysFastOnLargeAllPathMap() {
        if (!isPlatformDesktop) return

        val state = createLargeAllPathState()
        val pathfinding = PathfindingSystem(state)
        val goal = state.level.targetPositions.last()

        repeat(2) { state.attackers.forEach { pathfinding.findPath(it.position.value, goal, it) } }

        val elapsedMs = measureMillis { state.attackers.forEach { pathfinding.findPath(it.position.value, goal, it) } }
        println("Pathfinding for $ATTACKER_COUNT enemies on ${GRID_WIDTH}x$GRID_HEIGHT: $elapsedMs ms")

        assertTrue(
            elapsedMs < PATHFINDING_BUDGET_MS,
            "Pathfinding for all enemies took $elapsedMs ms (budget ${PATHFINDING_BUDGET_MS} ms).",
        )
    }

    private fun measureMillis(block: () -> Unit): Double {
        val mark = TimeSource.Monotonic.markNow()
        block()
        return mark.elapsedNow().inWholeNanoseconds.toDouble() / 1_000_000.0
    }
}
