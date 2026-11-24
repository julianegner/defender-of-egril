package de.egril.defender.editor

import de.egril.defender.model.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TileReplacementUtilsTest {
    @Test
    fun replaceTilesByType_replacesNoPlayAndUnsetTilesOnWholeMap() {
        val originalTiles =
            mapOf(
                "0,0" to TileType.SPAWN_POINT,
                "1,0" to TileType.NO_PLAY,
            )

        val (updatedTiles, changedKeys) =
            replaceTilesByType(
                tiles = originalTiles,
                mapWidth = 3,
                mapHeight = 2,
                sourceTileType = TileType.NO_PLAY,
                targetTileType = TileType.PATH,
            )

        assertEquals(TileType.SPAWN_POINT, updatedTiles["0,0"])
        assertEquals(TileType.PATH, updatedTiles["1,0"])
        assertEquals(TileType.PATH, updatedTiles["2,1"])
        assertEquals(5, changedKeys.size)
    }

    @Test
    fun replaceTilesByType_replacesOnlyInsideArea() {
        val originalTiles =
            mapOf(
                "0,0" to TileType.PATH,
                "1,1" to TileType.PATH,
                "2,2" to TileType.PATH,
                "3,3" to TileType.PATH,
            )

        val (updatedTiles, changedKeys) =
            replaceTilesByType(
                tiles = originalTiles,
                mapWidth = 5,
                mapHeight = 5,
                sourceTileType = TileType.PATH,
                targetTileType = TileType.BUILD_AREA,
                area = TileReplacementArea(from = Position(1, 1), to = Position(2, 2)),
            )

        assertEquals(TileType.PATH, updatedTiles["0,0"])
        assertEquals(TileType.BUILD_AREA, updatedTiles["1,1"])
        assertEquals(TileType.BUILD_AREA, updatedTiles["2,2"])
        assertEquals(TileType.PATH, updatedTiles["3,3"])
        assertEquals(setOf("1,1", "2,2"), changedKeys)
    }

    @Test
    fun replaceTilesByType_normalizesAndClampsAreaBounds() {
        val originalTiles =
            mapOf(
                "0,0" to TileType.PATH,
                "1,1" to TileType.PATH,
                "2,2" to TileType.PATH,
            )

        val (updatedTiles, changedKeys) =
            replaceTilesByType(
                tiles = originalTiles,
                mapWidth = 3,
                mapHeight = 3,
                sourceTileType = TileType.PATH,
                targetTileType = TileType.NO_PLAY,
                area = TileReplacementArea(from = Position(5, 5), to = Position(1, 1)),
            )

        assertEquals(TileType.PATH, updatedTiles["0,0"])
        assertEquals(TileType.NO_PLAY, updatedTiles["1,1"])
        assertEquals(TileType.NO_PLAY, updatedTiles["2,2"])
        assertTrue(changedKeys.containsAll(listOf("1,1", "2,2")))
    }
}
