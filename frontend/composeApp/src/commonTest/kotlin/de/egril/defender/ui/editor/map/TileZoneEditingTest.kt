package de.egril.defender.ui.editor.map

import de.egril.defender.editor.TileType
import de.egril.defender.model.Position
import de.egril.defender.model.RiverFlow
import de.egril.defender.model.RiverTile
import de.egril.defender.model.TileZone
import kotlin.test.Test
import kotlin.test.assertEquals

class TileZoneEditingTest {
    @Test
    fun everyTileTypeCanReplaceEveryBaseTileType() {
        val position = Position(1, 2)

        for (baseType in TileType.entries) {
            for (paintType in TileType.entries) {
                val updated =
                    paintTileZone(
                        zone = TileZone(id = "zone"),
                        position = position,
                        baseType = baseType,
                        paintType = paintType,
                        riverFlow = RiverFlow.EAST,
                        riverSpeed = 1,
                    )

                if (paintType == baseType && paintType != TileType.RIVER) {
                    assertEquals(emptyMap(), updated.tiles)
                } else {
                    assertEquals(paintType, updated.tiles[position])
                }
            }
        }
    }

    @Test
    fun implicitNoPlayTileCanBeAddedToZone() {
        val position = Position(0, 0)
        val updated =
            paintTileZone(
                zone = TileZone(id = "zone"),
                position = position,
                baseType = null,
                paintType = TileType.PATH,
                riverFlow = RiverFlow.EAST,
                riverSpeed = 1,
            )

        assertEquals(TileType.PATH, updated.tiles[position])
    }

    @Test
    fun copiedZoneGetsANewIdAndKeepsItsTileAndRiverData() {
        val position = Position(1, 2)
        val riverTile = RiverTile(position, RiverFlow.NORTH_EAST, flowSpeed = 2)
        val source = TileZone(id = "zone_1", name = "Flooded", tiles = mapOf(position to TileType.RIVER), riverTiles = mapOf(position to riverTile))

        val copy = copyTileZone(listOf(source), source)

        assertEquals("zone_2", copy.id)
        assertEquals("", copy.name)
        assertEquals(source.tiles, copy.tiles)
        assertEquals(source.riverTiles, copy.riverTiles)
    }
}
