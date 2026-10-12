package de.egril.defender.mapgen

import de.egril.defender.editor.EditorMap
import de.egril.defender.model.TileZone
import de.egril.defender.mapgen.core.TileZoneOverlayRenderer

/** Generates the feathered zone overlay for an editor map. The rendering itself lives in the shared `:mapgen` module. */
object TileZoneImageGenerator {
    fun generateOverlayPixels(
        map: EditorMap,
        zone: TileZone,
    ): Triple<IntArray, Int, Int> = TileZoneOverlayRenderer.generateOverlayPixels(map.toMapImageInput(), zone.toMapImageTiles())
}
