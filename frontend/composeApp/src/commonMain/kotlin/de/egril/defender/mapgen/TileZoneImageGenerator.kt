package de.egril.defender.mapgen

import de.egril.defender.editor.EditorMap
import de.egril.defender.model.TileZone
import de.egril.defender.mapgen.core.TileZoneOverlayRenderer

/** Generates the zone image for an editor map. The rendering itself lives in the shared `:mapgen` module. */
object TileZoneImageGenerator {
    fun generateOverlayPixels(
        map: EditorMap,
        zone: TileZone,
    ): Triple<IntArray, Int, Int> = TileZoneOverlayRenderer.generateOverlayPixels(map.toMapImageInput(), zone.toMapImageTiles())

    /**
     * Movable zones are drawn at a moved position at runtime, so their image holds only the zone's own
     * tiles without the soft fade. Fixed zones keep the feathered overlay.
     */
    fun generateZonePixels(
        map: EditorMap,
        zone: TileZone,
    ): Triple<IntArray, Int, Int> =
        if (zone.movable) {
            TileZoneOverlayRenderer.generateZoneOnlyPixels(map.toMapImageInput(), zone.toMapImageTiles())
        } else {
            generateOverlayPixels(map, zone)
        }
}
