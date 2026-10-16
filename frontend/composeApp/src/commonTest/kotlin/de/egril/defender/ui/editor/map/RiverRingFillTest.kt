package de.egril.defender.ui.editor.map

import de.egril.defender.editor.TileType
import de.egril.defender.model.Position
import de.egril.defender.model.RiverFlow
import de.egril.defender.model.RiverTile
import de.egril.defender.model.getHexNeighbors
import de.egril.defender.model.hexDistanceTo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RiverRingFillTest {
    /** Builds a square pond of river tiles (no flow yet) of the given size, centered near [center]. */
    private fun pond(
        center: Position,
        radius: Int,
    ): MutableMap<String, TileType> {
        val tiles = mutableMapOf<String, TileType>()
        for (x in (center.x - radius)..(center.x + radius)) {
            for (y in (center.y - radius)..(center.y + radius)) {
                tiles["$x,$y"] = TileType.RIVER
            }
        }
        return tiles
    }

    @Test
    fun spiralFlowsContinuouslyBetweenRingsInsteadOfClosingEachRing() {
        val center = Position(5, 5)
        val tiles = pond(center, radius = 3)
        val riverTiles = mutableMapOf<String, RiverTile>()

        val (_, updatedRivers) =
            applyRiverRing(
                tiles = tiles,
                riverTiles = riverTiles,
                mapWidth = 20,
                mapHeight = 20,
                start = center,
                clockwise = true,
                innerToOuter = true,
                flowSpeed = 1,
            )

        // Every tile that received a flow must point to an actual hex-neighbor - i.e. flow
        // directions form a single connected, continuously advancing spiral (no teleporting).
        updatedRivers.forEach { (key, riverTile) ->
            if (riverTile.flowDirection == RiverFlow.NONE) return@forEach
            val (x, y) = key.split(",").map { it.toInt() }
            val position = Position(x, y)
            val target =
                when (riverTile.flowDirection) {
                    RiverFlow.EAST -> position.getHexNeighbors()[0]
                    RiverFlow.NORTH_EAST -> position.getHexNeighbors()[1]
                    RiverFlow.NORTH_WEST -> position.getHexNeighbors()[2]
                    RiverFlow.WEST -> position.getHexNeighbors()[3]
                    RiverFlow.SOUTH_WEST -> position.getHexNeighbors()[4]
                    RiverFlow.SOUTH_EAST -> position.getHexNeighbors()[5]
                    else -> position
                }
            assertTrue(
                position.getHexNeighbors().contains(target),
                "Flow direction from $position must point to an adjacent hex tile",
            )
        }

        // At least one tile beyond the anchor's immediate ring should have received a flow,
        // proving that the spiral continued past the first ring instead of closing on itself.
        val distantKey = "${center.x + 3},${center.y}"
        assertTrue(updatedRivers.containsKey(distantKey))
        assertTrue(updatedRivers[distantKey]?.flowDirection != RiverFlow.NONE)

        // Follow the flow chain from the anchor outward: the hex-distance from the center must
        // never decrease. A regression where the flow reverses direction partway around a ring
        // (pointing back inward instead of continuing outward) would show up as a decrease here.
        var current = center
        var previousDistance = 0
        val visited = mutableSetOf(current)
        var guard = 0
        while (guard++ < tiles.size) {
            val flow = updatedRivers["${current.x},${current.y}"]?.flowDirection ?: break
            if (flow == RiverFlow.NONE) break
            val directionIndex =
                when (flow) {
                    RiverFlow.EAST -> 0
                    RiverFlow.NORTH_EAST -> 1
                    RiverFlow.NORTH_WEST -> 2
                    RiverFlow.WEST -> 3
                    RiverFlow.SOUTH_WEST -> 4
                    RiverFlow.SOUTH_EAST -> 5
                    else -> break
                }
            val next = current.getHexNeighbors()[directionIndex]
            val nextDistance = center.hexDistanceTo(next)
            assertTrue(
                nextDistance >= previousDistance,
                "Flow from $current to $next must not move closer to the center " +
                    "($nextDistance < $previousDistance) - the spiral must only expand outward",
            )
            previousDistance = nextDistance
            if (!visited.add(next)) break
            current = next
        }
    }

    @Test
    fun existingFlowTilesAreNotOverwritten() {
        val center = Position(5, 5)
        val tiles = pond(center, radius = 2)
        val riverTiles =
            mutableMapOf(
                "${center.x + 1},${center.y}" to
                    RiverTile(position = Position(center.x + 1, center.y), flowDirection = RiverFlow.WEST, flowSpeed = 2),
            )

        val (_, updatedRivers) =
            applyRiverRing(
                tiles = tiles,
                riverTiles = riverTiles,
                mapWidth = 20,
                mapHeight = 20,
                start = center,
                clockwise = true,
                innerToOuter = true,
                flowSpeed = 1,
            )

        val preserved = updatedRivers["${center.x + 1},${center.y}"]
        assertEquals(RiverFlow.WEST, preserved?.flowDirection)
        assertEquals(2, preserved?.flowSpeed)
    }
}
