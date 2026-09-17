package de.egril.defender.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class HexDirectionTest {
    @Test
    fun samePositionReturnsNull() {
        assertNull(hexDirectionBetween(Position(3, 3), Position(3, 3)))
    }

    @Test
    fun adjacentDirectionsEvenRow() {
        // Even row (y = 2, parity 0) neighbour offsets: E, NE, NW, W, SW, SE
        val from = Position(2, 2)
        assertEquals(HexDirection.E, hexDirectionBetween(from, Position(3, 2)))
        assertEquals(HexDirection.NE, hexDirectionBetween(from, Position(2, 1)))
        assertEquals(HexDirection.NW, hexDirectionBetween(from, Position(1, 1)))
        assertEquals(HexDirection.W, hexDirectionBetween(from, Position(1, 2)))
        assertEquals(HexDirection.SW, hexDirectionBetween(from, Position(1, 3)))
        assertEquals(HexDirection.SE, hexDirectionBetween(from, Position(2, 3)))
    }

    @Test
    fun adjacentDirectionsOddRow() {
        // Odd row (y = 1, parity 1) neighbour offsets.
        val from = Position(2, 1)
        assertEquals(HexDirection.E, hexDirectionBetween(from, Position(3, 1)))
        assertEquals(HexDirection.NE, hexDirectionBetween(from, Position(3, 0)))
        assertEquals(HexDirection.NW, hexDirectionBetween(from, Position(2, 0)))
        assertEquals(HexDirection.W, hexDirectionBetween(from, Position(1, 1)))
        assertEquals(HexDirection.SW, hexDirectionBetween(from, Position(2, 2)))
        assertEquals(HexDirection.SE, hexDirectionBetween(from, Position(3, 2)))
    }

    @Test
    fun matchesNeighborOrder() {
        // hexDirectionBetween must agree with the neighbour ordering used across the codebase.
        val order = listOf(
            HexDirection.E,
            HexDirection.NE,
            HexDirection.NW,
            HexDirection.W,
            HexDirection.SW,
            HexDirection.SE,
        )
        val from = Position(4, 4)
        from.getHexNeighbors().forEachIndexed { index, neighbor ->
            assertEquals(order[index], hexDirectionBetween(from, neighbor))
        }
    }

    @Test
    fun nonAdjacentMoveFallsBackToClosestDirection() {
        // A far away target to the east should resolve to an eastward-ish direction.
        val direction = hexDirectionBetween(Position(0, 0), Position(10, 0))
        assertNotNull(direction)
        assertEquals(HexDirection.E, direction)
    }

    @Test
    fun defaultFacingIsStable() {
        assertEquals(HexDirection.SW, HexDirection.DEFAULT)
    }
}
