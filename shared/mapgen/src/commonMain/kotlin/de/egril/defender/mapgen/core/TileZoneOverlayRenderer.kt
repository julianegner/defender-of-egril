package de.egril.defender.mapgen.core

import kotlin.math.ceil
import kotlin.math.floor
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

    /**
     * Renders only the tiles of a zone, with hard edges: a pixel is opaque when the hex centre nearest to
     * it belongs to [zoneTiles]. Used for movable zones, which are drawn at a rotated/shifted position
     * at runtime and must not carry the soft surrounding fade of [generateOverlayPixels].
     *
     * @return ARGB pixels (row-major) with the zone tiles opaque and everything else transparent, image width and height
     */
    fun generateZoneOnlyPixels(
        input: MapImageInput,
        zoneTiles: Map<Pair<Int, Int>, MapImageTileKind>,
    ): Triple<IntArray, Int, Int> {
        val zoneTilesInGrid =
            zoneTiles.filterKeys { (x, y) -> x in 0 until input.width && y in 0 until input.height }
        val (width, height) = MapImageRenderer.imageSize(input.width, input.height)
        if (zoneTilesInGrid.isEmpty()) return Triple(IntArray(width * height), width, height)

        val tiles = input.tiles.toMutableMap()
        tiles.putAll(zoneTilesInGrid)
        val (rendered, renderedWidth, renderedHeight) =
            MapImageRenderer.generatePixels(
                MapImageInput(input.seed, input.width, input.height, tiles, input.waterSpawnPoints),
            )
        val (origin0X, origin0Y) = MapImageRenderer.hexCenter(0, 0)
        val (columnX, _) = MapImageRenderer.hexCenter(1, 0)
        val (_, rowY) = MapImageRenderer.hexCenter(0, 1)
        val columnStep = columnX - origin0X
        val rowStep = rowY - origin0Y

        val centres = zoneTilesInGrid.keys.map { MapImageRenderer.hexCenter(it.first, it.second) }
        val left = max(0, floor(centres.minOf { it.first } - columnStep).toInt())
        val right = min(renderedWidth - 1, ceil(centres.maxOf { it.first } + columnStep).toInt())
        val top = max(0, floor(centres.minOf { it.second } - rowStep).toInt())
        val bottom = min(renderedHeight - 1, ceil(centres.maxOf { it.second } + rowStep).toInt())

        for (index in rendered.indices) rendered[index] = rendered[index] and 0xFFFFFF
        for (y in top..bottom) {
            val baseRow = floor((y - origin0Y) / rowStep).toInt()
            for (x in left..right) {
                var nearest: Pair<Int, Int>? = null
                var nearestDistance = Double.MAX_VALUE
                for (row in baseRow - 2..baseRow + 2) {
                    val rowOriginX = MapImageRenderer.hexCenter(0, row).first
                    val baseColumn = floor((x - rowOriginX) / columnStep).toInt()
                    for (column in baseColumn - 2..baseColumn + 2) {
                        val (cx, cy) = MapImageRenderer.hexCenter(column, row)
                        val dx = x - cx
                        val dy = y - cy
                        val distance = dx * dx + dy * dy
                        if (distance < nearestDistance) {
                            nearestDistance = distance
                            nearest = column to row
                        }
                    }
                }
                if (nearest != null && nearest in zoneTilesInGrid) {
                    val index = y * renderedWidth + x
                    rendered[index] = (0xFF shl 24) or (rendered[index] and 0xFFFFFF)
                }
            }
        }
        return Triple(rendered, renderedWidth, renderedHeight)
    }
}
