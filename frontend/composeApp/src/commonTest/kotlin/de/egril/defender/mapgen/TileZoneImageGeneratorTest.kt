package de.egril.defender.mapgen

import de.egril.defender.editor.EditorMap
import de.egril.defender.editor.TileType
import de.egril.defender.model.Position
import de.egril.defender.model.TileZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TileZoneImageGeneratorTest {
    private fun map(zone: TileZone): EditorMap =
        EditorMap(
            id = "zone_render_test",
            width = 8,
            height = 8,
            tiles = (0..7).flatMap { y -> (0..7).map { x -> "$x,$y" to TileType.PATH } }.toMap(),
            tileZones = listOf(zone),
        )

    @Test
    fun zoneRendersAFeatheredFullSizeTransparentOverlay() {
        val zone = TileZone(id = "flood", tiles = mapOf(Position(4, 4) to TileType.RIVER))
        val (pixels, width, height) = TileZoneImageGenerator.generateOverlayPixels(map(zone), zone)
        val (expectedWidth, expectedHeight) = MapImageGenerator.imageSize(8, 8)
        assertEquals(expectedWidth, width)
        assertEquals(expectedHeight, height)
        assertEquals(0, pixels[0] ushr 24, "Unchanged distant terrain stays transparent")

        val (cx, cy) = MapImageGenerator.hexCenter(4, 4)
        val center = cy.toInt() * width + cx.toInt()
        val edge = cy.toInt() * width + cx.toInt() + 90
        assertEquals(255, pixels[center] ushr 24, "Zone interior must fully cover the base map")
        assertTrue((pixels[edge] ushr 24) in 1..254, "Zone boundary must fade instead of ending abruptly")
    }

    @Test
    fun baseTypeZoneCoversEarlierOverlappingZone() {
        val zone = TileZone(id = "ebb", tiles = mapOf(Position(4, 4) to TileType.PATH))
        val (pixels, width, _) = TileZoneImageGenerator.generateOverlayPixels(map(zone), zone)
        val (cx, cy) = MapImageGenerator.hexCenter(4, 4)
        assertEquals(255, pixels[cy.toInt() * width + cx.toInt()] ushr 24)
        assertEquals(0, pixels[0] ushr 24)
    }

    @Test
    fun renderingUsesTheSameSeedAsBaseImage() {
        val zone = TileZone(id = "flood", tiles = mapOf(Position(4, 4) to TileType.RIVER))
        val map = map(zone)
        val (overlay, width, _) = TileZoneImageGenerator.generateOverlayPixels(map, zone)
        val (variant, _, _) = MapImageGenerator.generatePixels(
            map.copy(tiles = map.tiles + ("4,4" to TileType.RIVER)),
        )
        val (cx, cy) = MapImageGenerator.hexCenter(4, 4)
        val center = cy.toInt() * width + cx.toInt()
        assertEquals(variant[center], overlay[center])
    }
}
