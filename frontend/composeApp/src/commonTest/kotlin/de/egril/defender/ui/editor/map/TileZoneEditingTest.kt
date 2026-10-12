package de.egril.defender.ui.editor.map

import de.egril.defender.editor.TileType
import de.egril.defender.model.Position
import de.egril.defender.model.RiverFlow
import de.egril.defender.model.RiverTile
import de.egril.defender.model.TileZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
    fun addingTileKeepsItsBaseTypeSoTheZoneDoesNotChangeTerrain() {
        val position = Position(3, 3)

        val updated = addTileToZone(TileZone(id = "zone"), position, baseType = TileType.PATH, baseRiver = null)

        assertEquals(mapOf(position to TileType.PATH), updated.tiles)
        assertEquals(emptyMap(), updated.riverTiles)
    }

    @Test
    fun addingRiverTileKeepsItsBaseFlow() {
        val position = Position(4, 4)
        val baseRiver = RiverTile(Position(0, 0), RiverFlow.SOUTH_WEST, flowSpeed = 2)

        val updated = addTileToZone(TileZone(id = "zone"), position, baseType = TileType.RIVER, baseRiver = baseRiver)

        assertEquals(TileType.RIVER, updated.tiles[position])
        assertEquals(RiverTile(position, RiverFlow.SOUTH_WEST, flowSpeed = 2), updated.riverTiles[position])
    }

    @Test
    fun addingTileWithoutBaseTypeUsesNoPlay() {
        val position = Position(0, 1)

        val updated = addTileToZone(TileZone(id = "zone"), position, baseType = null, baseRiver = null)

        assertEquals(TileType.NO_PLAY, updated.tiles[position])
    }

    @Test
    fun addingTileAlreadyInZoneKeepsPaintedType() {
        val position = Position(2, 2)
        val zone = TileZone(id = "zone", tiles = mapOf(position to TileType.BUILD_AREA))

        val updated = addTileToZone(zone, position, baseType = TileType.PATH, baseRiver = null)

        assertEquals(zone, updated)
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

    @Test
    fun overlapOnlyCountsOtherMovableZones() {
        val shared = Position(1, 1)
        val movable = TileZone(id = "a", tiles = mapOf(shared to TileType.PATH), movable = true)
        val otherMovable = TileZone(id = "b", tiles = mapOf(shared to TileType.PATH), movable = true)
        val fixed = TileZone(id = "c", tiles = mapOf(shared to TileType.PATH), movable = false)

        assertTrue(overlapsOtherMovableZone(listOf(movable, otherMovable), movable))
        assertFalse(overlapsOtherMovableZone(listOf(movable, fixed), movable), "Fixed zones may overlap freely")
        assertFalse(overlapsOtherMovableZone(listOf(movable, otherMovable), fixed), "Only movable zones are checked")
        assertFalse(overlapsOtherMovableZone(listOf(movable, TileZone(id = "d", tiles = mapOf(Position(5, 5) to TileType.PATH), movable = true)), movable))
    }
}
