package de.egril.defender.mapgen

import androidx.compose.ui.graphics.Color
import de.egril.defender.editor.EditorMap
import de.egril.defender.editor.TileType
import kotlin.math.*

/**
 * Generates map background images from EditorMap data.
 *
 * Biome assignments:
 * - PATH / SPAWN_POINT / TARGET → Arid/Sandy (yellow-brown)
 * - BUILD_AREA / ISLAND → Grassland (green)
 * - NO_PLAY → Mountains (gray-brown)
 * - RIVER / WATER → Water (blue)
 *
 * Smooth transitions between biomes are achieved by distance-weighted blending.
 *
 * Based on concepts from: https://github.com/redblobgames/mapgen4
 * Original mapgen4 by Red Blob Games (redblobgames.com), used as inspiration.
 */
object MapImageGenerator {

    // Biome base colors
    private val COLOR_PATH = Color(0xFFC8B560)      // Sandy/arid yellow-brown
    private val COLOR_BUILD = Color(0xFF4A7C3F)      // Forest green
    private val COLOR_NOPLAY = Color(0xFF7A7060)     // Rocky mountain gray-brown
    private val COLOR_WATER = Color(0xFF3A6EA5)      // Deep water blue
    private val COLOR_SPAWN = Color(0xFFBB8833)      // Slightly darker sandy (spawn point)
    private val COLOR_TARGET = Color(0xFF336622)     // Deep green (target)

    // Hex grid layout constants (matching HexagonalGridConstants)
    private const val HEX_SIZE = 40f         // hexSize in dp
    private val SQRT3 = sqrt(3.0).toFloat()
    val HEX_WIDTH = HEX_SIZE * SQRT3         // ≈ 69.28 dp
    val HEX_HEIGHT = HEX_SIZE * 2f           // = 80 dp
    private val VERTICAL_SPACING = HEX_HEIGHT * 0.75f  // = 60 dp
    private const val VERTICAL_SPACING_ADJUSTMENT = -7f
    private const val HORIZONTAL_SPACING = -10f
    private const val ODD_ROW_OFFSET_RATIO = 0.42f

    // Row step = hexHeight + rowSpacing - 1 (from HexUtils.kt formula)
    val ROW_STEP = HEX_HEIGHT + (-HEX_HEIGHT + VERTICAL_SPACING + VERTICAL_SPACING_ADJUSTMENT) - 1f  // = 52 dp
    val COL_STEP = HEX_WIDTH + HORIZONTAL_SPACING  // ≈ 59.28 dp
    val ODD_ROW_OFFSET = HEX_WIDTH * ODD_ROW_OFFSET_RATIO  // ≈ 29.1 dp

    /**
     * Calculate the pixel position of hex tile center (x, y).
     * Using the same formula as HexUtils.screenToHexGridPosition (reversed).
     */
    fun tileCenter(x: Int, y: Int): Pair<Float, Float> {
        val pixelY = y * ROW_STEP + 1f + HEX_HEIGHT / 2f
        val pixelX = (if (y % 2 == 1) ODD_ROW_OFFSET else 0f) + x * COL_STEP + HEX_WIDTH / 2f
        return Pair(pixelX, pixelY)
    }

    /**
     * Calculate the image dimensions for a grid of given size.
     */
    fun imageSize(gridWidth: Int, gridHeight: Int): Pair<Int, Int> {
        val (lastColX, _) = tileCenter(gridWidth - 1, 0)
        val width = (lastColX + HEX_WIDTH / 2f).toInt() + 2
        val (_, lastRowY) = tileCenter(0, gridHeight - 1)
        val height = (lastRowY + HEX_HEIGHT / 2f).toInt() + 2
        return Pair(width, height)
    }

    /**
     * Assigns a biome color to a tile type.
     */
    fun biomeColor(tileType: TileType): Color {
        return when (tileType) {
            TileType.PATH -> COLOR_PATH
            TileType.BUILD_AREA -> COLOR_BUILD
            TileType.NO_PLAY -> COLOR_NOPLAY
            TileType.RIVER -> COLOR_WATER
            TileType.SPAWN_POINT -> COLOR_SPAWN
            TileType.TARGET -> COLOR_TARGET
        }
    }

    /**
     * Blend two colors by factor t (0.0 = color1, 1.0 = color2).
     */
    fun blendColors(c1: Color, c2: Color, t: Float): Color {
        val tt = t.coerceIn(0f, 1f)
        return Color(
            red = c1.red + (c2.red - c1.red) * tt,
            green = c1.green + (c2.green - c1.green) * tt,
            blue = c1.blue + (c2.blue - c1.blue) * tt,
            alpha = 1f
        )
    }

    /**
     * Simple smooth step function for transitions.
     */
    private fun smoothStep(t: Float): Float {
        val x = t.coerceIn(0f, 1f)
        return x * x * (3f - 2f * x)
    }

    /**
     * Generate pixel color data for the map image.
     * Returns a Triple of (pixels ARGB array, width, height).
     */
    fun generatePixels(map: EditorMap): Triple<IntArray, Int, Int> {
        val (imgWidth, imgHeight) = imageSize(map.width, map.height)
        val pixels = IntArray(imgWidth * imgHeight)

        // Pre-compute tile centers and biome colors
        data class TileInfo(val cx: Float, val cy: Float, val color: Color)
        val tiles = mutableListOf<TileInfo>()
        for (y in 0 until map.height) {
            for (x in 0 until map.width) {
                val tileType = map.tiles["$x,$y"] ?: TileType.NO_PLAY
                val (cx, cy) = tileCenter(x, y)
                tiles.add(TileInfo(cx, cy, biomeColor(tileType)))
            }
        }

        // For each pixel, find the weighted blend of nearby tile colors
        val blendRadius = HEX_SIZE * 2.5f

        for (py in 0 until imgHeight) {
            for (px in 0 until imgWidth) {
                var totalWeight = 0f
                var r = 0f; var g = 0f; var b = 0f

                for (tile in tiles) {
                    val dx = px - tile.cx
                    val dy = py - tile.cy
                    val dist = sqrt(dx * dx + dy * dy)
                    if (dist < blendRadius) {
                        val normalizedDist = dist / blendRadius
                        val weight = smoothStep(1f - normalizedDist)
                        totalWeight += weight
                        r += tile.color.red * weight
                        g += tile.color.green * weight
                        b += tile.color.blue * weight
                    }
                }

                if (totalWeight > 0f) {
                    val fr = (r / totalWeight).coerceIn(0f, 1f)
                    val fg = (g / totalWeight).coerceIn(0f, 1f)
                    val fb = (b / totalWeight).coerceIn(0f, 1f)
                    pixels[py * imgWidth + px] = (0xFF shl 24) or
                        ((fr * 255).toInt() shl 16) or
                        ((fg * 255).toInt() shl 8) or
                        (fb * 255).toInt()
                } else {
                    pixels[py * imgWidth + px] = 0xFF7A7060.toInt()
                }
            }
        }

        return Triple(pixels, imgWidth, imgHeight)
    }
}
