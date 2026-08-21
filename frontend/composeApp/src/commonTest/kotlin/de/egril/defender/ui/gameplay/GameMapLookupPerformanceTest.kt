package de.egril.defender.ui.gameplay

import de.egril.defender.model.Position
import de.egril.defender.utils.isPlatformDesktop
import kotlin.random.Random
import kotlin.system.measureNanoTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameMapLookupPerformanceTest {
    private companion object {
        const val EFFECT_LIST_COUNT = 18
        const val TARGET_LIST_COUNT = 8
    }

    private data class TileEffect(
        val position: Position,
        val value: Int,
    )

    @Test
    fun precomputedLookupMapsAreFastEnoughOn40x40Grid() {
        if (!isPlatformDesktop) {
            return
        }

        val gridWidth = 40
        val gridHeight = 40
        val effectCountPerList = 220
        val targetCountPerList = 260
        val lists = buildEffectLists(gridWidth, gridHeight, effectCountPerList)
        val targets = buildTargetLists(gridWidth, gridHeight, targetCountPerList)
        val effectMaps = lists.map { list -> list.associateBy { it.position } }
        val targetSets = targets.map { it.toHashSet() }

        repeat(3) { legacyLookupPass(gridWidth, gridHeight, lists, targets) }
        repeat(3) { optimizedLookupPass(gridWidth, gridHeight, effectMaps, targetSets) }

        val legacyTimingsMs = mutableListOf<Double>()
        val optimizedTimingsMs = mutableListOf<Double>()
        var legacyChecksum = 0L
        var optimizedChecksum = 0L

        repeat(6) { iteration ->
            val legacyBlock = {
                legacyChecksum = legacyLookupPass(gridWidth, gridHeight, lists, targets)
            }
            val optimizedBlock = {
                optimizedChecksum = optimizedLookupPass(gridWidth, gridHeight, effectMaps, targetSets)
            }
            val first = if (iteration % 2 == 0) legacyBlock else optimizedBlock
            val second = if (iteration % 2 == 0) optimizedBlock else legacyBlock
            if (iteration % 2 == 0) {
                legacyTimingsMs += measureMillis(first)
                optimizedTimingsMs += measureMillis(second)
            } else {
                optimizedTimingsMs += measureMillis(first)
                legacyTimingsMs += measureMillis(second)
            }
        }

        assertEquals(
            legacyChecksum,
            optimizedChecksum,
            "Legacy and optimized lookup implementations must produce the same aggregate result",
        )

        val legacyMedianMs = median(legacyTimingsMs)
        val optimizedMedianMs = median(optimizedTimingsMs)
        val optimizedDivisor = optimizedMedianMs.coerceAtLeast(0.000_001)
        val speedup = legacyMedianMs / optimizedDivisor
        println(
            "GameMap lookup benchmark (40x40): " +
                "before=${"%.2f".format(legacyMedianMs)}ms, " +
                "after=${"%.2f".format(optimizedMedianMs)}ms, " +
                "speedup=${"%.2f".format(speedup)}x",
        )

        assertTrue(
            speedup >= 1.3,
            "Lookup performance regression on 40x40 grid: " +
                "before=${"%.2f".format(legacyMedianMs)}ms, " +
                "after=${"%.2f".format(optimizedMedianMs)}ms, " +
                "speedup=${"%.2f".format(speedup)}x (required >= 1.30x)",
        )
    }

    private fun measureMillis(block: () -> Unit): Double = measureNanoTime(block).toDouble() / 1_000_000.0

    private fun median(values: List<Double>): Double {
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 0) {
            (sorted[mid - 1] + sorted[mid]) / 2.0
        } else {
            sorted[mid]
        }
    }

    private fun buildEffectLists(
        gridWidth: Int,
        gridHeight: Int,
        countPerList: Int,
    ): List<List<TileEffect>> =
        List(EFFECT_LIST_COUNT) { listIndex ->
            val shuffledPositions = allPositions(gridWidth, gridHeight).shuffled(Random(listIndex))
            shuffledPositions.take(countPerList).mapIndexed { i, position ->
                TileEffect(position, listIndex * 1000 + i)
            }
        }

    private fun buildTargetLists(
        gridWidth: Int,
        gridHeight: Int,
        countPerList: Int,
    ): List<List<Position>> =
        List(TARGET_LIST_COUNT) { listIndex ->
            allPositions(gridWidth, gridHeight).shuffled(Random(100 + listIndex)).take(countPerList)
        }

    private fun allPositions(
        gridWidth: Int,
        gridHeight: Int,
    ): List<Position> =
        buildList(gridWidth * gridHeight) {
            for (y in 0 until gridHeight) {
                for (x in 0 until gridWidth) {
                    add(Position(x, y))
                }
            }
        }

    private fun legacyLookupPass(
        gridWidth: Int,
        gridHeight: Int,
        effectLists: List<List<TileEffect>>,
        targetLists: List<List<Position>>,
    ): Long {
        var checksum = 0L
        for (y in 0 until gridHeight) {
            for (x in 0 until gridWidth) {
                val position = Position(x, y)
                for (effects in effectLists) {
                    val found = effects.firstOrNull { it.position == position }
                    checksum += found?.value?.toLong() ?: 0L
                }
                for (targets in targetLists) {
                    if (targets.any { it == position }) {
                        checksum += 1L
                    }
                }
            }
        }
        return checksum
    }

    private fun optimizedLookupPass(
        gridWidth: Int,
        gridHeight: Int,
        effectMaps: List<Map<Position, TileEffect>>,
        targetSets: List<Set<Position>>,
    ): Long {
        var checksum = 0L
        for (y in 0 until gridHeight) {
            for (x in 0 until gridWidth) {
                val position = Position(x, y)
                for (effectMap in effectMaps) {
                    val found = effectMap[position]
                    checksum += found?.value?.toLong() ?: 0L
                }
                for (targetSet in targetSets) {
                    if (position in targetSet) {
                        checksum += 1L
                    }
                }
            }
        }
        return checksum
    }
}
