package de.egril.defender.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RegionMoveTest {
    /** A small irregular blob spanning both even and odd rows. */
    private val region: Set<Position> =
        setOf(
            Position(4, 3), Position(5, 3), Position(3, 4), Position(4, 4),
            Position(5, 4), Position(4, 5), Position(6, 5),
        )

    private fun hexDistance(
        a: Position,
        b: Position,
    ): Int {
        val ca = a.toCube()
        val cb = b.toCube()
        return maxOf(kotlin.math.abs(ca.q - cb.q), kotlin.math.abs(ca.r - cb.r), kotlin.math.abs(ca.s - cb.s))
    }

    @Test
    fun identityMoveKeepsEveryPosition() {
        val mapper = RegionMove(0, 0, 0).mapperFor(region)
        region.forEach { assertEquals(it, mapper(it)) }
        assertTrue(RegionMove(0, 0, 0).isIdentity)
    }

    @Test
    fun sixRotationsReturnToStart() {
        val mapper = RegionMove(1, 0, 0).mapperFor(region)
        region.forEach { start ->
            var current = start
            repeat(6) { current = mapper(current) }
            assertEquals(start, current)
        }
    }

    @Test
    fun rotationKeepsDistancesBetweenTiles() {
        val mapper = RegionMove(2, 0, 0).mapperFor(region)
        val mapped = region.map(mapper)
        for (a in region) for (b in region) {
            assertEquals(hexDistance(a, b), hexDistance(mapper(a), mapper(b)))
        }
        assertEquals(region.size, mapped.toSet().size, "Mapping is injective")
    }

    @Test
    fun translationKeepsShapeAndMapsToHexes() {
        for ((dx, dy) in listOf(1 to 0, 3 to -2, -2 to 1, 0 to 3)) {
            val mapper = RegionMove(0, dx, dy).mapperFor(region)
            val mapped = region.map(mapper)
            for (a in region) for (b in region) {
                assertEquals(hexDistance(a, b), hexDistance(mapper(a), mapper(b)), "dx=$dx dy=$dy")
            }
            assertEquals(region.size, mapped.toSet().size)
        }
    }

    @Test
    fun translationMovesAnchorByRequestedCells() {
        // The anchor is the top-most, then left-most tile, which is (4,3) for this region.
        val mapper = RegionMove(0, 1, 0).mapperFor(region)
        assertEquals(Position(5, 3), mapper(Position(4, 3)))
    }

    @Test
    fun negativeRotationStepsEqualTheirPositiveCounterpart() {
        val negative = RegionMove(-1, 0, 0).mapperFor(region)
        val positive = RegionMove(5, 0, 0).mapperFor(region)
        region.forEach { assertEquals(positive(it), negative(it)) }
    }
}
