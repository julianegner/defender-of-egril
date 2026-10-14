package de.egril.defender.memory

import de.egril.defender.game.GameEngine
import de.egril.defender.game.LevelData
import de.egril.defender.model.GamePhase
import de.egril.defender.model.GameState
import de.egril.defender.model.Level
import org.junit.Assume
import org.junit.Test
import java.io.File
import java.lang.ref.WeakReference
import kotlin.test.assertTrue

/**
 * Memory leak detection for the game engine.
 *
 * Disabled unless the environment variable `MEMORY_LEAK_TEST=true` is set, because it plays many
 * full games and is intended for the scheduled memory leak workflow.
 *
 * Two complementary checks:
 * 1. Retention: after a simulated game is released, its [GameState] must be garbage-collected.
 *    If it stays reachable, something global is holding on to game state.
 * 2. Growth: repeatedly playing the same level must not steadily increase post-GC heap usage.
 *
 * Tuning (environment variables):
 * - `MEMORY_LEAK_ITERATIONS` (default 40): games played for the growth check
 * - `MEMORY_LEAK_MAX_GROWTH_KB` (default 256): allowed heap growth per game, in KB
 * - `MEMORY_LEAK_BRANCH`: branch name written into the report
 *
 * A report is written to `build/memory-leak-report/result.txt` (relative to the module directory)
 * so the workflow can read the findings even when the test fails.
 */
class MemoryLeakTest {
    private var turnsSimulated = 0

    @Test
    fun releasedGamesAreGarbageCollectedForEveryLevel() {
        Assume.assumeTrue("Set MEMORY_LEAK_TEST=true to run the memory leak scan", System.getenv("MEMORY_LEAK_TEST") == "true")
        val levels = LevelData.createLevels()
        val findings = mutableListOf<String>()
        val info = mutableListOf<String>()

        for (level in levels) {
            val stateRef = playAndRelease(level)
            if (!awaitCollected(stateRef)) {
                findings += "RETENTION: game state of level '${level.name}' (id ${level.id}) is still reachable after the game was released"
            }
        }
        info += "Checked retention for ${levels.size} levels, simulated $turnsSimulated turns in total"

        val iterations = System.getenv("MEMORY_LEAK_ITERATIONS")?.toIntOrNull() ?: DEFAULT_ITERATIONS
        val maxGrowthKb = System.getenv("MEMORY_LEAK_MAX_GROWTH_KB")?.toDoubleOrNull() ?: DEFAULT_MAX_GROWTH_KB
        val growthLevel = levels.first()
        val samples = mutableListOf<Pair<Double, Double>>()
        for (i in 0 until iterations) {
            playAndRelease(growthLevel)
            if (i >= WARMUP_ITERATIONS) {
                samples += (i.toDouble() to usedHeapAfterGc().toDouble())
            }
        }
        val kbPerGame = linearSlope(samples.map { it.first }, samples.map { it.second }) / 1024.0
        info += "Heap growth on level '${growthLevel.name}': %.1f KB per game over %d games".format(kbPerGame, samples.size)
        if (kbPerGame > maxGrowthKb) {
            findings +=
                "GROWTH: heap usage grows by %.1f KB per game (limit %.1f KB) when repeatedly playing level '%s'"
                    .format(kbPerGame, maxGrowthKb, growthLevel.name)
        }

        writeReport(findings, info)
        assertTrue(findings.isEmpty(), "Memory leaks found:\n" + findings.joinToString("\n"))
    }

    /** Plays a game and returns only a weak reference so no strong reference survives the call. */
    private fun playAndRelease(level: Level): WeakReference<GameState> {
        val state = GameState(level)
        state.initializePrePlacedElements()
        val engine = GameEngine(state)
        val ref = WeakReference(state)
        playSimulatedGame(engine, state)
        return ref
    }

    private fun playSimulatedGame(
        engine: GameEngine,
        state: GameState,
    ) {
        engine.addCoins(SIMULATED_COINS)
        placeTowers(engine, state.level)
        engine.startFirstPlayerTurn()
        for (turn in 0 until MAX_TURNS_PER_GAME) {
            if (state.isLevelWon() || state.isLevelLost()) break
            if (state.phase.value != GamePhase.PLAYER_TURN) break
            turnsSimulated++
            engine.autoDefenderAttacks()
            engine.startEnemyTurn()
            val movements = engine.calculateEnemyTurnMovements()
            for (step in movements.allMovementSteps) {
                for ((attackerId, position) in step) {
                    engine.applyMovement(attackerId, position)
                }
            }
            engine.completeEnemyTurn()
        }
    }

    private fun placeTowers(
        engine: GameEngine,
        level: Level,
    ) {
        val positions = level.buildAreas.sortedWith(compareBy({ it.y }, { it.x }))
        val towerTypes = level.availableTowers.toList()
        if (towerTypes.isEmpty()) return
        var placed = 0
        for ((index, position) in positions.withIndex()) {
            if (placed >= MAX_TOWERS_PER_GAME) break
            val type = towerTypes[index % towerTypes.size]
            if (engine.placeDefender(type, position, instantDeploy = true)) placed++
        }
    }

    private fun awaitCollected(ref: WeakReference<*>): Boolean {
        repeat(GC_ATTEMPTS) {
            if (ref.get() == null) return true
            System.gc()
            Thread.sleep(GC_WAIT_MS)
        }
        return ref.get() == null
    }

    private fun usedHeapAfterGc(): Long {
        repeat(3) {
            System.gc()
            Thread.sleep(GC_WAIT_MS)
        }
        val runtime = Runtime.getRuntime()
        return runtime.totalMemory() - runtime.freeMemory()
    }

    /** Least-squares slope of ys over xs; used to detect a steady upward trend in heap usage. */
    private fun linearSlope(
        xs: List<Double>,
        ys: List<Double>,
    ): Double {
        if (xs.size < 2) return 0.0
        val meanX = xs.average()
        val meanY = ys.average()
        var numerator = 0.0
        var denominator = 0.0
        for (i in xs.indices) {
            numerator += (xs[i] - meanX) * (ys[i] - meanY)
            denominator += (xs[i] - meanX) * (xs[i] - meanX)
        }
        return if (denominator == 0.0) 0.0 else numerator / denominator
    }

    private fun writeReport(
        findings: List<String>,
        info: List<String>,
    ) {
        val status = if (findings.isEmpty()) "CLEAN" else "LEAKS_FOUND"
        val lines =
            buildList {
                add("STATUS: $status")
                add("BRANCH: ${System.getenv("MEMORY_LEAK_BRANCH") ?: "unknown"}")
                info.forEach { add("INFO: $it") }
                findings.forEach { add("FINDING: $it") }
            }
        File(REPORT_DIR).mkdirs()
        File(REPORT_DIR, REPORT_FILE).writeText(lines.joinToString("\n") + "\n")
    }

    private companion object {
        const val REPORT_DIR = "build/memory-leak-report"
        const val REPORT_FILE = "result.txt"
        const val DEFAULT_ITERATIONS = 40
        const val DEFAULT_MAX_GROWTH_KB = 256.0
        const val WARMUP_ITERATIONS = 10
        const val MAX_TURNS_PER_GAME = 60
        const val MAX_TOWERS_PER_GAME = 8
        const val SIMULATED_COINS = 10_000
        const val GC_ATTEMPTS = 10
        const val GC_WAIT_MS = 20L
    }
}
