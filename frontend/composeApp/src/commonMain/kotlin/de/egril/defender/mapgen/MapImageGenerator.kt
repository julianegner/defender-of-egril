package de.egril.defender.mapgen

import de.egril.defender.editor.EditorMap
import de.egril.defender.editor.MapSizeLimits
import de.egril.defender.mapgen.core.MapImageRenderer

/** Generates background images for editor maps. The rendering itself lives in the shared `:mapgen` module. */
object MapImageGenerator {
    fun hexCenter(
        gx: Int,
        gy: Int,
    ): Pair<Double, Double> = MapImageRenderer.hexCenter(gx, gy)

    fun imageSize(
        gridWidth: Int,
        gridHeight: Int,
    ): Pair<Int, Int> = MapImageRenderer.imageSize(gridWidth, gridHeight)

    fun nearestTileIndex(
        px: Double,
        py: Double,
        cX: DoubleArray,
        cY: DoubleArray,
    ): Int = MapImageRenderer.nearestTileIndex(px, py, cX, cY)

    fun generatePixels(map: EditorMap): Triple<IntArray, Int, Int> {
        require(map.canRenderMinimap()) {
            "Map preview generation requires a valid map within ${MapSizeLimits.MAX_WIDTH}x${MapSizeLimits.MAX_HEIGHT}"
        }
        return MapImageRenderer.generatePixels(map.toMapImageInput())
    }
}
