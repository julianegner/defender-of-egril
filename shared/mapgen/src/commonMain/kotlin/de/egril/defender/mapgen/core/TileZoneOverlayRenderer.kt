package de.egril.defender.mapgen.core

import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Prepares a transparent overlay from the full rendered map with one zone applied. Generating
 * the full map keeps the same noise and terrain blending as the base image; only the area around
 * changed tiles remains visible, with a soft transition to the original background.
 */
object TileZoneOverlayRenderer {
    private const val SOLID_RADIUS = 48.0
    private const val FADE_RADIUS = 150.0

    /**
     * Renders the overlay for a zone that replaces the tile kinds in [zoneTiles] on top of [input].
     * Zone positions outside the map grid are ignored.
     *
     * @return ARGB pixels (row-major) where alpha marks the area affected by the zone, image width and height
     */
    fun generateOverlayPixels(
        input: MapImageInput,
        zoneTiles: Map<Pair<Int, Int>, MapImageTileKind>,
    ): Triple<IntArray, Int, Int> {
        val zoneTilesInGrid =
            zoneTiles.filterKeys { (x, y) -> x in 0 until input.width && y in 0 until input.height }
        val tiles = input.tiles.toMutableMap()
        tiles.putAll(zoneTilesInGrid)
        val (width, height) = MapImageRenderer.imageSize(input.width, input.height)
        if (zoneTilesInGrid.isEmpty()) return Triple(IntArray(width * height), width, height)

        val (rendered, renderedWidth, renderedHeight) =
            MapImageRenderer.generatePixels(
                MapImageInput(input.seed, input.width, input.height, tiles, input.waterSpawnPoints),
            )
        val alpha = ByteArray(rendered.size)
        for (position in zoneTilesInGrid.keys) {
            val (centerX, centerY) = MapImageRenderer.hexCenter(position.first, position.second)
            val left = max(0, (centerX - FADE_RADIUS).toInt())
            val right = min(renderedWidth - 1, ceil(centerX + FADE_RADIUS).toInt())
            val top = max(0, (centerY - FADE_RADIUS).toInt())
            val bottom = min(renderedHeight - 1, ceil(centerY + FADE_RADIUS).toInt())
            for (y in top..bottom) {
                for (x in left..right) {
                    val dx = x - centerX
                    val dy = y - centerY
                    val distanceSquared = dx * dx + dy * dy
                    if (distanceSquared >= FADE_RADIUS * FADE_RADIUS) continue
                    val opacity =
                        if (distanceSquared <= SOLID_RADIUS * SOLID_RADIUS) {
                            255
                        } else {
                            val t = ((sqrt(distanceSquared) - SOLID_RADIUS) / (FADE_RADIUS - SOLID_RADIUS)).coerceIn(0.0, 1.0)
                            (255 * (1 - t * t * (3 - 2 * t))).roundToInt()
                        }
                    val index = y * renderedWidth + x
                    if (opacity > (alpha[index].toInt() and 0xFF)) alpha[index] = opacity.toByte()
                }
            }
        }
        for (index in rendered.indices) {
            rendered[index] = ((alpha[index].toInt() and 0xFF) shl 24) or (rendered[index] and 0xFFFFFF)
        }
        return Triple(rendered, renderedWidth, renderedHeight)
    }
}
