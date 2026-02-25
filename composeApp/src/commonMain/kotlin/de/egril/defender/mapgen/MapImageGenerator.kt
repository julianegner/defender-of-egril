package de.egril.defender.mapgen

import androidx.compose.ui.graphics.Color
import de.egril.defender.editor.EditorMap
import de.egril.defender.editor.TileType
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Biome-based map image generator inspired by redblobgames/mapgen4.
 * Translated to Kotlin Multiplatform from the provided TypeScript reference.
 */
object MapImageGenerator {

    // --- Hex grid geometry (matches HexUtils / HexagonalGridConstants) ---
    private const val HEX_SIZE = 40f
    private val SQRT3 = sqrt(3.0).toFloat()
    val HEX_WIDTH = HEX_SIZE * SQRT3         // ≈ 69.28 dp
    val HEX_HEIGHT = HEX_SIZE * 2f           // 80 dp
    private val VERTICAL_SPACING = HEX_HEIGHT * 0.75f  // 60 dp
    private const val VERTICAL_SPACING_ADJUSTMENT = -7f
    private const val HORIZONTAL_SPACING = -10f
    private const val ODD_ROW_OFFSET_RATIO = 0.42f

    // Row step = hexHeight + rowSpacing - 1 (from HexUtils.kt)
    // Matches HexagonalMapView row stacking: each row is hexHeight tall with -27dp spacing and a +1dp offset per row
    val ROW_STEP = HEX_HEIGHT + (-HEX_HEIGHT + VERTICAL_SPACING + VERTICAL_SPACING_ADJUSTMENT) // 53 dp
    val COL_STEP = HEX_WIDTH + HORIZONTAL_SPACING  // ≈ 59.28 dp
    val ODD_ROW_OFFSET = HEX_WIDTH * ODD_ROW_OFFSET_RATIO  // ≈ 29.1 dp

    // --- Mapgen parameters (mirroring mapgen4 style) ---
    private const val BLEND_SIGMA = 30f               // Gaussian sigma for biome blending
    private const val MOISTURE_DOMINANCE = 0.35f      // Moist biomes dominate transitions
    private const val NEAREST_ANCHOR_WEIGHT = 6f      // Ensures center tile stays dominant
    private val NOISE_SCALES = floatArrayOf(1f, 2f, 4f)
    private val NOISE_WEIGHTS = floatArrayOf(0.6f, 0.3f, 0.1f)
    private const val NOISE_BASE_PX = 256f
    private const val LAND_FBM_FREQ = 1f / 220f
    private const val MOUNTAIN_RIDGE_FREQ = 1f / 180f

    private const val SHADING_SCALE = 1.4f
    private const val AMBIENT = 0.45f
    private const val OUTLINE_STRENGTH = 3.6f
    private const val OUTLINE_THRESHOLD = 0.06f
    private val LIGHT = floatArrayOf(-0.7f, 0.5f, 0.5f) // mapgen4-ish light dir

    // --- Biome definitions (elevation/moisture/noiseAmp) ---
    private data class Biome(val elevation: Float, val moisture: Float, val noiseAmp: Float)

    private val BIOME_PATH = Biome(elevation = 0.05f, moisture = 0.1f, noiseAmp = 0.04f)
    private val BIOME_BUILD = Biome(elevation = 0.12f, moisture = 0.8f, noiseAmp = 0.04f)
    private val BIOME_NOPLAY = Biome(elevation = 0.55f, moisture = 0.1f, noiseAmp = 0.45f)
    private val BIOME_RIVER = Biome(elevation = -0.45f, moisture = 1.0f, noiseAmp = 0.02f)
    private val BIOME_SPAWN = Biome(elevation = 0.08f, moisture = 0.2f, noiseAmp = 0.05f)
    private val BIOME_TARGET = Biome(elevation = 0.15f, moisture = 0.9f, noiseAmp = 0.05f)
    private val DEFAULT_BIOME = BIOME_NOPLAY

    private val TILE_BIOME: Map<TileType, Biome> = mapOf(
        TileType.PATH to BIOME_PATH,
        TileType.BUILD_AREA to BIOME_BUILD,
        TileType.NO_PLAY to BIOME_NOPLAY,
        TileType.RIVER to BIOME_RIVER,
        TileType.SPAWN_POINT to BIOME_SPAWN,
        TileType.TARGET to BIOME_TARGET
    )

    // --- Public API ---
    fun tileCenter(x: Int, y: Int): Pair<Float, Float> {
        val pixelY = y * ROW_STEP + 1f + HEX_HEIGHT / 2f
        val pixelX = (if (y % 2 == 1) ODD_ROW_OFFSET else 0f) + x * COL_STEP + HEX_WIDTH / 2f
        return Pair(pixelX, pixelY)
    }

    fun imageSize(gridWidth: Int, gridHeight: Int): Pair<Int, Int> {
        val rowSpacing = -HEX_HEIGHT + VERTICAL_SPACING + VERTICAL_SPACING_ADJUSTMENT
        val colSpacing = HORIZONTAL_SPACING

        var maxRight = 0f
        var maxBottom = 0f

        for (y in 0 until gridHeight) {
            val startX = if (y % 2 == 1) ODD_ROW_OFFSET else 0f
            val rowWidth = startX + (gridWidth * HEX_WIDTH) + ((gridWidth - 1) * colSpacing)
            if (rowWidth > maxRight) maxRight = rowWidth

            val topY = y * (HEX_HEIGHT + rowSpacing)
            val bottomY = topY + HEX_HEIGHT
            if (bottomY > maxBottom) maxBottom = bottomY
        }

        // Add 1px guard padding to match Compose layout rounding
        val width = ceil(maxRight).toInt() + 1
        val height = ceil(maxBottom).toInt() + 1
        return Pair(width, height)
    }

    /**
     * Generate pixel data (ARGB) for a map using a mapgen4-inspired pipeline:
     * 1) Gaussian blend biome elevation & moisture per pixel using a spatial grid
     * 2) Add multi-octave simplex noise scaled by biome noiseAmp
+     * 3) Hillshade + outline mask (mountain-only) + colormap sampling
     */
    fun generatePixels(map: EditorMap): Triple<IntArray, Int, Int> {
        val (imgW, imgH) = imageSize(map.width, map.height)

        // Build tile arrays
        val n = map.width * map.height
        val cX = FloatArray(n)
        val cY = FloatArray(n)
        val elevBase = FloatArray(n)
        val moistBase = FloatArray(n)
        val noiseAmps = FloatArray(n)

        val grid = SpatialGrid(cellSize = BLEND_SIGMA, width = imgW)
        var idx = 0
        for (gx in 0 until map.width) {
            for (gy in 0 until map.height) {
                val (cx, cy) = tileCenter(gx, gy)
                cX[idx] = cx
                cY[idx] = cy
                val biome = TILE_BIOME[map.tiles["$gx,$gy"] ?: TileType.NO_PLAY] ?: DEFAULT_BIOME
                elevBase[idx] = biome.elevation
                moistBase[idx] = biome.moisture
                noiseAmps[idx] = biome.noiseAmp
                grid.insert(cx, cy, idx)
                idx++
            }
        }

        val sigma2 = 2f * BLEND_SIGMA * BLEND_SIGMA
        val searchR = ceil(4f * BLEND_SIGMA / BLEND_SIGMA).toInt() // 4 sigma

        // Seeded noise keyed to map id for deterministic output
        val noiseId = map.id ?: "egril-default"
        val noise2D = SimplexNoise2D.fromSeed(hashString(noiseId))

        val elevMap = FloatArray(imgW * imgH)
        val moistMap = FloatArray(imgW * imgH)
        val naMap = FloatArray(imgW * imgH)

        // Pass 1: blended elevation & moisture
        for (py in 0 until imgH) {
            for (px in 0 until imgW) {
                val nearby = grid.query(px.toFloat(), py.toFloat(), searchR)
                var tW = 0f
                var aE = 0f
                var aM = 0f
                var aNA = 0f
                var nearestIdx = -1
                var nearestDist2 = Float.MAX_VALUE
                for (hi in nearby) {
                    val dx = px - cX[hi]
                    val dy = py - cY[hi]
                    val moistureW = 1f + moistBase[hi] * MOISTURE_DOMINANCE
                    val w = moistureW * exp(-(dx * dx + dy * dy) / sigma2)
                    val d2 = dx * dx + dy * dy
                    if (d2 < nearestDist2) {
                        nearestDist2 = d2
                        nearestIdx = hi
                    }
                    aE += w * elevBase[hi]
                    aM += w * moistBase[hi]
                    aNA += w * noiseAmps[hi]
                    tW += w
                }
                if (nearestIdx >= 0) {
                    val w = NEAREST_ANCHOR_WEIGHT
                    aE += w * elevBase[nearestIdx]
                    aM += w * moistBase[nearestIdx]
                    aNA += w * noiseAmps[nearestIdx]
                    tW += w
                }
                var e = aE / tW
                val m = aM / tW
                val na = aNA / tW

                if (na > 0.005f) {
                    val ridge = ridgeNoise(noise2D, px.toFloat(), py.toFloat(), baseFreq = MOUNTAIN_RIDGE_FREQ, octaves = 5, gain = 0.55f, lacunarity = 2.05f)
                    val detail = fbm(noise2D, px.toFloat(), py.toFloat(), baseFreq = LAND_FBM_FREQ, octaves = 4, gain = 0.5f, lacunarity = 1.9f)
                    // Ridge dominates mountains (NO_PLAY noiseAmp is high); detail adds surface variation
                    e = (e + na * (0.85f * ridge + 0.35f * detail)).coerceIn(-1f, 1f)
                } else {
                    // Light surface variation for flat biomes
                    val subtle = fbm(noise2D, px.toFloat(), py.toFloat(), baseFreq = LAND_FBM_FREQ, octaves = 3, gain = 0.55f, lacunarity = 1.9f)
                    e = (e + 0.05f * subtle).coerceIn(-1f, 1f)
                }

                val i = py * imgW + px
                // Slight moisture variation for richer palette
                val mDetail = fbm(noise2D, px.toFloat(), py.toFloat(), baseFreq = LAND_FBM_FREQ * 0.8f, octaves = 3, gain = 0.55f, lacunarity = 2f) * 0.08f
                elevMap[i] = e
                moistMap[i] = (m + mDetail).coerceIn(0f, 1f)
                naMap[i] = na
            }
        }

        // Pass 2: hillshading + outline + colormap
        val pixels = IntArray(imgW * imgH)
        val lx = LIGHT[0]; val ly = LIGHT[1]; val lz = LIGHT[2]
        for (py in 0 until imgH) {
            for (px in 0 until imgW) {
                val i = py * imgW + px
                val e = elevMap[i]
                val m = moistMap[i]
                val na = naMap[i]

                val iL = if (px > 0) i - 1 else i
                val iR = if (px < imgW - 1) i + 1 else i
                val iU = if (py > 0) i - imgW else i
                val iD = if (py < imgH - 1) i + imgW else i

                val eL = elevMap[iL]; val eR = elevMap[iR]
                val eU = elevMap[iU]; val eD = elevMap[iD]

                val gx = (eR - eL) * 0.5f * SHADING_SCALE
                val gy = (eD - eU) * 0.5f * SHADING_SCALE
                val nLen = sqrt(gx * gx + gy * gy + 1f)
                val nx = -gx / nLen
                val ny = -gy / nLen
                val nz = 1f / nLen

                val shade = min(2f, AMBIENT + max(0f, nx * lx + ny * ly + nz * lz))

                val slopeDiff = kotlin.math.abs(eR - eL) + kotlin.math.abs(eD - eU)
                val outlineRaw = max(0f, slopeDiff * OUTLINE_STRENGTH - OUTLINE_THRESHOLD)
                val outlineMask = min(1f, na * 4f) // Mountains dominate outline; fades on flats
                val outline = min(1f, outlineRaw * outlineRaw) * outlineMask

                val base = sampleColormap(e, m)
                val s = shade * (1 - outline)

                val r = (base.red * 255f * s).coerceIn(0f, 255f).roundToInt()
                val g = (base.green * 255f * s).coerceIn(0f, 255f).roundToInt()
                val b = (base.blue * 255f * s).coerceIn(0f, 255f).roundToInt()

                pixels[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }

        return Triple(pixels, imgW, imgH)
    }

    // --- Colormap approximation (mapgen4-style elevation/moisture map) ---
    private fun sampleColormap(e: Float, m: Float): Color {
        val elev = e.coerceIn(-1f, 1f)
        val moist = m.coerceIn(0f, 1f)

        fun lerp(a: Color, b: Color, t: Float): Color {
            val tt = t.coerceIn(0f, 1f)
            return Color(
                red = a.red + (b.red - a.red) * tt,
                green = a.green + (b.green - a.green) * tt,
                blue = a.blue + (b.blue - a.blue) * tt,
                alpha = 1f
            )
        }

        // Water stack (deep → shallow → shore glow)
        val deepWater = Color(0xFF0B274B.toInt())
        val midWater = Color(0xFF124B8A.toInt())
        val shallowWater = Color(0xFF1E7AC0.toInt())
        val shoreWater = Color(0xFF48A7D5.toInt())
        val shore = Color(0xFFE3C78F.toInt())

        // Land stack (sand → grass → forest → rock → snow)
        val drySand = Color(0xFFDCC68A.toInt())
        val duneSand = Color(0xFFE4D09E.toInt())
        val dryGrass = Color(0xFF9BB575.toInt())
        val lushGrass = Color(0xFF6CA064.toInt())
        val forest = Color(0xFF3C7A4D.toInt())
        val rock = Color(0xFF8A7D6C.toInt())
        val snow = Color(0xFFF5F7FA.toInt())

        return when {
            elev < -0.6f -> lerp(deepWater, midWater, (elev + 1f) / 0.4f)
            elev < -0.35f -> lerp(midWater, shallowWater, (elev + 0.6f) / 0.25f)
            elev < -0.1f -> lerp(shallowWater, shoreWater, (elev + 0.35f) / 0.25f)
            elev < 0f -> lerp(shoreWater, shore, (elev + 0.1f) / 0.1f)
            elev < 0.14f -> {
                // Beaches to lowland; moisture pushes green inland
                val base = lerp(duneSand, dryGrass, moist.pow(0.8f))
                lerp(base, lushGrass, moist.pow(0.5f))
            }
            elev < 0.38f -> {
                val g = lerp(dryGrass, lushGrass, moist)
                lerp(g, forest, moist.pow(1.1f))
            }
            elev < 0.62f -> {
                val f = lerp(forest, rock, (elev - 0.38f) / 0.24f)
                lerp(f, lushGrass, (1f - moist) * 0.25f)
            }
            else -> {
                lerp(rock, snow, ((elev - 0.62f) / 0.38f).coerceIn(0f, 1f))
            }
        }
    }

    // --- Utilities ---
    private fun hashString(s: String): Int {
        var h = 0
        for (c in s) {
            h = (31 * h + c.code) and 0x7fffffff
        }
        return h
    }

    private fun fbm(
        noise: SimplexNoise2D,
        x: Float,
        y: Float,
        baseFreq: Float,
        octaves: Int,
        gain: Float,
        lacunarity: Float
    ): Float {
        var freq = baseFreq
        var amp = 1f
        var sum = 0f
        var norm = 0f
        repeat(octaves) {
            val n = noise.noise(x * freq, y * freq)
            sum += n * amp
            norm += amp
            freq *= lacunarity
            amp *= gain
        }
        return if (norm > 0f) sum / norm else 0f
    }

    private fun ridgeNoise(
        noise: SimplexNoise2D,
        x: Float,
        y: Float,
        baseFreq: Float,
        octaves: Int,
        gain: Float,
        lacunarity: Float
    ): Float {
        var freq = baseFreq
        var amp = 1f
        var sum = 0f
        var norm = 0f
        repeat(octaves) {
            val n = noise.noise(x * freq, y * freq)
            val r = 1f - kotlin.math.abs(n)
            val ridge = r * r
            sum += ridge * amp
            norm += amp
            freq *= lacunarity
            amp *= gain
        }
        return if (norm > 0f) sum / norm else 0f
    }
}

// Lightweight spatial grid for neighbor queries
private class SpatialGrid(private val cellSize: Float, private val width: Int) {
    private val map = HashMap<Long, MutableList<Int>>()

    private fun cellIndex(x: Float, y: Float): Pair<Int, Int> =
        Pair((x / cellSize).toInt(), (y / cellSize).toInt())

    private fun key(cx: Int, cy: Int): Long = (cx.toLong() shl 32) xor (cy.toLong() and 0xffffffffL)

    fun insert(x: Float, y: Float, idx: Int) {
        val (cx, cy) = cellIndex(x, y)
        val k = key(cx, cy)
        val list = map.getOrPut(k) { mutableListOf() }
        list.add(idx)
    }

    fun query(x: Float, y: Float, radiusCells: Int): List<Int> {
        val (cx, cy) = cellIndex(x, y)
        val out = mutableListOf<Int>()
        for (dx in -radiusCells..radiusCells) {
            for (dy in -radiusCells..radiusCells) {
                val k = key(cx + dx, cy + dy)
                val list = map[k]
                if (list != null) out.addAll(list)
            }
        }
        return out
    }
}

// Simplex noise 2D (lightweight, deterministic)
private class SimplexNoise2D(private val perm: IntArray) {
    fun noise(xin: Float, yin: Float): Float {
        // Skew constants
        val root3 = sqrt(3.0).toFloat()
        val F2 = 0.5f * (root3 - 1f)
        val G2 = (3f - root3) / 6f

        var n0: Float
        var n1: Float
        var n2: Float

        val s = (xin + yin) * F2
        val i = kotlin.math.floor((xin + s).toDouble()).toInt()
        val j = kotlin.math.floor((yin + s).toDouble()).toInt()
        val t = (i + j) * G2
        val X0 = i - t
        val Y0 = j - t
        val x0 = xin - X0
        val y0 = yin - Y0

        val (i1, j1) = if (x0 > y0) 1 to 0 else 0 to 1

        val x1 = x0 - i1 + G2
        val y1 = y0 - j1 + G2
        val x2 = x0 - 1f + 2f * G2
        val y2 = y0 - 1f + 2f * G2

        val ii = i and 255
        val jj = j and 255
        val gi0 = perm[ii + perm[jj]] % 12
        val gi1 = perm[ii + i1 + perm[jj + j1]] % 12
        val gi2 = perm[ii + 1 + perm[jj + 1]] % 12

        n0 = if (x0 * x0 + y0 * y0 < 0.5f) {
            val t0 = 0.5f - x0 * x0 - y0 * y0
            t0 * t0 * t0 * t0 * dot(grad3[gi0], x0, y0)
        } else 0f

        n1 = if (x1 * x1 + y1 * y1 < 0.5f) {
            val t1 = 0.5f - x1 * x1 - y1 * y1
            t1 * t1 * t1 * t1 * dot(grad3[gi1], x1, y1)
        } else 0f

        n2 = if (x2 * x2 + y2 * y2 < 0.5f) {
            val t2 = 0.5f - x2 * x2 - y2 * y2
            t2 * t2 * t2 * t2 * dot(grad3[gi2], x2, y2)
        } else 0f

        return 70f * (n0 + n1 + n2)
    }

    private fun dot(g: IntArray, x: Float, y: Float): Float =
        g[0] * x + g[1] * y

    companion object {
        private val grad3 = arrayOf(
            intArrayOf(1, 1), intArrayOf(-1, 1), intArrayOf(1, -1), intArrayOf(-1, -1),
            intArrayOf(1, 0), intArrayOf(-1, 0), intArrayOf(1, 0), intArrayOf(-1, 0),
            intArrayOf(0, 1), intArrayOf(0, -1), intArrayOf(0, 1), intArrayOf(0, -1)
        )

        fun fromSeed(seed: Int): SimplexNoise2D {
            val perm = IntArray(512)
            val p = IntArray(256) { it }
            var state = seed
            for (i in 255 downTo 0) {
                state = state * 1664525 + 1013904223
                val r = (state ushr 16) and 0x7fff
                val j = r % (i + 1)
                val tmp = p[i]
                p[i] = p[j]
                p[j] = tmp
            }
            for (i in 0 until 512) {
                perm[i] = p[i and 255]
            }
            return SimplexNoise2D(perm)
        }
    }
}
