package de.egril.defender.mapgen

import de.egril.defender.editor.EditorMap
import de.egril.defender.model.TileZone
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
object TileZoneImageGenerator {
    private const val SOLID_RADIUS = 48.0
    private const val FADE_RADIUS = 150.0

    fun generateOverlayPixels(
        map: EditorMap,
        zone: TileZone,
    ): Triple<IntArray, Int, Int> {
        val tiles = map.tiles.toMutableMap()
        val zoneTiles =
            zone.tiles.filter { (position, type) ->
                position.x in 0 until map.width &&
                    position.y in 0 until map.height &&
                    type in TileZone.SUPPORTED_TILE_TYPES
            }
        zoneTiles.forEach { (position, type) -> tiles["${position.x},${position.y}"] = type }
        val (width, height) = MapImageGenerator.imageSize(map.width, map.height)
        if (zoneTiles.isEmpty()) return Triple(IntArray(width * height), width, height)

        val (rendered, renderedWidth, renderedHeight) = MapImageGenerator.generatePixels(map.copy(tiles = tiles))
        val alpha = ByteArray(rendered.size)
        for (position in zoneTiles.keys) {
            val (centerX, centerY) = MapImageGenerator.hexCenter(position.x, position.y)
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
