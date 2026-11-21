package de.egril.defender.ui.editor.map

import de.egril.defender.editor.EditorMap
import de.egril.defender.editor.TileType
import de.egril.defender.model.Position
import de.egril.defender.model.getHexNeighbors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NarrowPathDetectionTest {

    private fun mapOf(width: Int, height: Int, tiles: Map<String, TileType>) =
        EditorMap(id = "test", width = width, height = height, tiles = tiles)

    @Test
    fun straightOneWideCorridorInteriorIsNarrow() {
        val tiles = (0..5).associate { "$it,0" to TileType.PATH }
        val narrow = mapOf(6, 1, tiles).findNarrowPathPositions()
        // Ends are dead ends and do not cut the path; the middle tiles do
        assertEquals((1..4).map { Position(it, 0) }.toSet(), narrow)
    }

    @Test
    fun junctionWithFourPathNeighborsIsNarrowWhenNeighborsOnlyConnectThroughIt() {
        val center = Position(3, 3)
        val ring = center.getHexNeighbors()
        // Two separate pairs of ring neighbors (0-1 and 3-4): they only meet through the center
        val path = listOf(center, ring[0], ring[1], ring[3], ring[4])
        val tiles = path.associate { "${it.x},${it.y}" to TileType.PATH }
        val narrow = mapOf(8, 8, tiles).findNarrowPathPositions()
        assertTrue(center in narrow, "Center with 4 path neighbors should be a bottleneck")
    }

    @Test
    fun tileWithRingConnectionIsNotNarrow() {
        val center = Position(3, 3)
        val all = listOf(center) + center.getHexNeighbors()
        val tiles = all.associate { "${it.x},${it.y}" to TileType.PATH }
        val narrow = mapOf(8, 8, tiles).findNarrowPathPositions()
        assertFalse(center in narrow, "Center is bypassed by the ring of neighbors")
    }

    @Test
    fun interiorTilesOfTwoWideStripAreNotNarrow() {
        // Two rows of path: removing any single tile leaves the rest connected
        val tiles = (1..6).flatMap { x -> listOf("$x,2" to TileType.PATH, "$x,3" to TileType.PATH) }.toMap()
        val narrow = mapOf(8, 6, tiles).findNarrowPathPositions()
        assertTrue(narrow.isEmpty(), "Expected no narrow tiles but got $narrow")
    }

    @Test
    fun deadEndTileIsNotNarrow() {
        val tiles = mapOf(
            "2,2" to TileType.PATH,
            "3,2" to TileType.PATH,
        )
        assertTrue(mapOf(6, 6, tiles).findNarrowPathPositions().isEmpty())
    }

    @Test
    fun nonWalkableTilesAreIgnored() {
        val tiles = mapOf(
            "2,2" to TileType.BUILD_AREA,
            "3,2" to TileType.NO_PLAY,
            "2,3" to TileType.RIVER,
        )
        assertTrue(mapOf(6, 6, tiles).findNarrowPathPositions().isEmpty())
    }

    @Test
    fun spawnAndTargetCountAsWalkable() {
        val tiles = mapOf(
            "0,0" to TileType.SPAWN_POINT,
            "1,0" to TileType.TARGET,
        )
        val narrow = mapOf(3, 3, tiles).findNarrowPathPositions()
        assertTrue(narrow.isEmpty())
    }

    @Test
    fun emptyMapHasNoNarrowPaths() {
        assertFalse(mapOf(5, 5, emptyMap()).findNarrowPathPositions().any())
    }

    private fun parse(key: String): Position {
        val (x, y) = key.split(",").map { it.toInt() }
        return Position(x, y)
    }
}
