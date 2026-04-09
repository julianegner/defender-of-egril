package de.egril.defender

import kotlin.math.*

/**
 * Simplified map tile types mirroring the frontend EditorMap tile types for image generation.
 * Only the types that affect rendered colours are needed.
 */
enum class BackendTileType {
    PATH, BUILD_AREA, NO_PLAY, SPAWN_POINT, TARGET, RIVER
}

/**
 * Minimal representation of a community map as stored in the `data` JSON field.
 * Only the fields required for image generation are extracted.
 */
data class BackendMapData(
    val id: String,
    val width: Int,
    val height: Int,
    /** "x,y" → BackendTileType */
    val tiles: Map<String, BackendTileType>
)

/**
 * Parses the minimal map fields from the JSON data stored in community_files.
 * Uses a hand-rolled parser to avoid adding a JSON dependency beyond what Ktor already pulls in.
 */
fun parseBackendMapData(json: String): BackendMapData? {
    return try {
        val id = extractJsonStringValue(json, "id") ?: ""
        val width = extractJsonIntValue(json, "width") ?: return null
        val height = extractJsonIntValue(json, "height") ?: return null

        // tiles is a JSON object "x,y" -> "TILE_TYPE_STRING"
        val tilesMap = mutableMapOf<String, BackendTileType>()
        val tilesStart = json.indexOf("\"tiles\"")
        if (tilesStart >= 0) {
            val braceOpen = json.indexOf('{', tilesStart)
            if (braceOpen >= 0) {
                val braceClose = findMatchingBrace(json, braceOpen)
                if (braceClose > braceOpen) {
                    val tilesJson = json.substring(braceOpen + 1, braceClose)
                    // Parse entries like "3,4":"PATH"
                    val entryRegex = Regex(""""(\d+,\d+)"\s*:\s*"([A-Z_]+)"""")
                    for (match in entryRegex.findAll(tilesJson)) {
                        val key = match.groupValues[1]
                        val typeName = match.groupValues[2]
                        val tileType = when (typeName) {
                            "PATH" -> BackendTileType.PATH
                            "BUILD_AREA" -> BackendTileType.BUILD_AREA
                            "SPAWN_POINT" -> BackendTileType.SPAWN_POINT
                            "TARGET" -> BackendTileType.TARGET
                            "RIVER" -> BackendTileType.RIVER
                            else -> BackendTileType.NO_PLAY
                        }
                        tilesMap[key] = tileType
                    }
                }
            }
        }

        BackendMapData(id = id, width = width, height = height, tiles = tilesMap)
    } catch (_: Exception) {
        null
    }
}

/** Finds the index of the closing brace that matches the opening brace at [openIndex]. */
private fun findMatchingBrace(s: String, openIndex: Int): Int {
    var depth = 0
    var inString = false
    var i = openIndex
    while (i < s.length) {
        val c = s[i]
        if (c == '"' && (i == 0 || s[i - 1] != '\\')) inString = !inString
        if (!inString) {
            if (c == '{') depth++
            else if (c == '}') {
                depth--
                if (depth == 0) return i
            }
        }
        i++
    }
    return -1
}

/** Extracts a top-level integer value from a JSON string (no nesting). */
internal fun extractJsonIntValue(json: String, key: String): Int? {
    val regex = Regex(""""$key"\s*:\s*(-?\d+)""")
    return regex.find(json)?.groupValues?.get(1)?.toIntOrNull()
}

// ──────────────────────────────────────────────────────────────────────────────
// Image generation (ported from frontend MapImageGenerator.kt)
// ──────────────────────────────────────────────────────────────────────────────

object BackendMapImageGenerator {

    private const val HEX_SIZE = 40.0
    private val SQRT3 = sqrt(3.0)
    private val HEX_WIDTH = HEX_SIZE * SQRT3
    private const val HEX_HEIGHT = HEX_SIZE * 2.0
    private const val VERTICAL_SPACING = HEX_HEIGHT * 0.75
    private const val HORIZONTAL_SPACING = -10.0
    private const val ODD_ROW_OFFSET_RATIO = 0.42
    private const val RIVER_Y_BIAS = -HEX_HEIGHT * 0.5
    private const val PADDING = 20.0
    private val BLEND_SIGMA = HEX_WIDTH * 0.45
    private const val SHADING_SCALE = 70.0
    private const val AMBIENT = 0.22
    private val LIGHT = doubleArrayOf(-0.5, -0.5, 0.707)
    private const val OUTLINE_STRENGTH = 15.0
    private const val OUTLINE_THRESHOLD = 0.25
    private const val MOISTURE_DOMINANCE = 3.0
    private val NOISE_SCALES = doubleArrayOf(1.0, 2.0, 4.0)
    private val NOISE_WEIGHTS = doubleArrayOf(0.70, 0.22, 0.08)
    private val NOISE_BASE_PX = HEX_WIDTH

    private data class Biome(val elevation: Double, val moisture: Double, val noiseAmp: Double, val blendWeight: Double = 1.0)
    private val DEFAULT_BIOME = Biome(0.60, 0.0, 0.45)
    private val TILE_BIOME = mapOf(
        BackendTileType.NO_PLAY to Biome(0.60, 0.0, 0.45),
        BackendTileType.RIVER to Biome(-0.30, 0.0, 0.05, blendWeight = 0.6),
        BackendTileType.PATH to Biome(0.06, 0.08, 0.04),
        BackendTileType.BUILD_AREA to Biome(0.05, 1.00, 0.04),
        BackendTileType.SPAWN_POINT to Biome(0.06, 0.08, 0.04),
        BackendTileType.TARGET to Biome(0.06, 0.08, 0.04)
    )

    private const val COLORMAP_WIDTH = 64
    private const val COLORMAP_HEIGHT = 64
    private val COLORMAP_DATA: IntArray by lazy { buildColormapData() }

    private fun buildColormapData(): IntArray {
        val pixels = IntArray(COLORMAP_WIDTH * COLORMAP_HEIGHT * 4)
        var p = 0
        for (y in 0 until COLORMAP_HEIGHT) {
            for (x in 0 until COLORMAP_WIDTH) {
                val e = 2.0 * x / COLORMAP_WIDTH - 1.0
                val m = y.toDouble() / COLORMAP_HEIGHT
                val (r, g, b) = when {
                    x == COLORMAP_WIDTH / 2 - 1 -> Triple(48, 120, 160)
                    x == COLORMAP_WIDTH / 2 - 2 -> Triple(48, 100, 150)
                    x == COLORMAP_WIDTH / 2 - 3 -> Triple(48, 80, 140)
                    e < 0.0 -> Triple(
                        (48 + 48 * e).roundToInt(),
                        (64 + 64 * e).roundToInt(),
                        (127 + 127 * e).roundToInt()
                    )
                    else -> {
                        val mAdjusted = m * (1 - e)
                        var rr = 210 - 100 * mAdjusted
                        var gg = 185 - 45 * mAdjusted
                        var bb = 139 - 45 * mAdjusted
                        rr = 255 * e + rr * (1 - e)
                        gg = 255 * e + gg * (1 - e)
                        bb = 255 * e + bb * (1 - e)
                        Triple(rr.roundToInt(), gg.roundToInt(), bb.roundToInt())
                    }
                }
                pixels[p++] = r; pixels[p++] = g; pixels[p++] = b; pixels[p++] = 255
            }
        }
        return pixels
    }

    private fun sampleColormap(elevation: Double, moisture: Double): Triple<Int, Int, Int> {
        val e = elevation.coerceIn(-1.0, 1.0)
        val m = moisture.coerceIn(0.0, 1.0)
        val x = min(COLORMAP_WIDTH - 1, floor((e + 1) / 2 * COLORMAP_WIDTH).toInt())
        val y = min(COLORMAP_HEIGHT - 1, floor(m * COLORMAP_HEIGHT).toInt())
        val pp = (y * COLORMAP_WIDTH + x) * 4
        return Triple(COLORMAP_DATA[pp], COLORMAP_DATA[pp + 1], COLORMAP_DATA[pp + 2])
    }

    private fun hexCenter(gx: Int, gy: Int): Pair<Double, Double> {
        val rowOffset = if (gy % 2 == 1) HEX_WIDTH * ODD_ROW_OFFSET_RATIO else 0.0
        val cx = gx * (HEX_WIDTH + HORIZONTAL_SPACING) + rowOffset + HEX_WIDTH / 2 + PADDING
        val cy = gy * VERTICAL_SPACING + HEX_HEIGHT / 2 + PADDING
        return Pair(cx, cy)
    }

    private fun imageSize(gridWidth: Int, gridHeight: Int): Pair<Int, Int> {
        val lastCol = gridWidth - 1
        val lastRow = gridHeight - 1
        val maxRowOffset = if (lastRow % 2 == 1) HEX_WIDTH * ODD_ROW_OFFSET_RATIO else 0.0
        val rightEdge = lastCol * (HEX_WIDTH + HORIZONTAL_SPACING) + maxRowOffset + HEX_WIDTH
        val bottomEdge = lastRow * VERTICAL_SPACING + HEX_HEIGHT
        val width = ceil(rightEdge + PADDING * 2).toInt()
        val height = ceil(bottomEdge + PADDING * 2).toInt()
        return Pair(width, height)
    }

    private fun hashString(str: String): Int {
        var h = 0x811C9DC5u
        for (ch in str) h = ((h xor ch.code.toUInt()) * 0x01000193u)
        return h.toInt()
    }

    private fun makeRandFloat(seed: Int): () -> Double {
        var s = seed.toUInt()
        if (s == 0u) s = 1u
        return {
            s = 1664525u * s + 1013904223u
            s.toDouble() / 0x1_0000_0000L.toDouble()
        }
    }

    /** Generates ARGB pixel data for the given map data. Returns (pixels, width, height). */
    fun generatePixels(map: BackendMapData): Triple<IntArray, Int, Int> {
        val (imgW, imgH) = imageSize(map.width, map.height)
        val n = map.width * map.height
        val cX = DoubleArray(n); val cY = DoubleArray(n)
        val elevBase = DoubleArray(n); val moistBase = DoubleArray(n)
        val noiseAmps = DoubleArray(n); val blendWeights = DoubleArray(n)
        val sigma2 = 2 * BLEND_SIGMA * BLEND_SIGMA
        val searchR = ceil(4 * BLEND_SIGMA / BLEND_SIGMA).toInt()

        val grid = SpatialGrid(BLEND_SIGMA, imgW)
        var idx = 0
        for (gx in 0 until map.width) {
            for (gy in 0 until map.height) {
                val (cx, cy) = hexCenter(gx, gy)
                val tileType = map.tiles["$gx,$gy"] ?: BackendTileType.NO_PLAY
                val biome = TILE_BIOME[tileType] ?: DEFAULT_BIOME
                cX[idx] = cx
                cY[idx] = if (tileType == BackendTileType.RIVER) cy + RIVER_Y_BIAS else cy
                elevBase[idx] = biome.elevation; moistBase[idx] = biome.moisture
                noiseAmps[idx] = biome.noiseAmp; blendWeights[idx] = biome.blendWeight
                grid.insert(cx, cY[idx], idx)
                idx++
            }
        }

        val noise2D = BackendSimplexNoise2D.fromRandom(makeRandFloat(hashString(map.id)))
        val elevMap = DoubleArray(imgW * imgH); val moistMap = DoubleArray(imgW * imgH); val naMap = DoubleArray(imgW * imgH)

        for (py in 0 until imgH) {
            for (px in 0 until imgW) {
                val nearby = grid.query(px.toDouble(), py.toDouble(), searchR)
                var tW = 0.0; var aE = 0.0; var aM = 0.0; var aNA = 0.0
                for (hi in nearby) {
                    val dx = px - cX[hi]; val dy = py - cY[hi]
                    val moistureW = 1.0 + moistBase[hi] * MOISTURE_DOMINANCE
                    val w = blendWeights[hi] * moistureW * exp(-(dx * dx + dy * dy) / sigma2)
                    aE += w * elevBase[hi]; aM += w * moistBase[hi]; aNA += w * noiseAmps[hi]; tW += w
                }
                if (tW == 0.0) continue
                var e = aE / tW; val m = aM / tW; val na = aNA / tW
                if (na > 0.005) {
                    var noiseVal = 0.0
                    for (oct in 0..2) {
                        val f = NOISE_SCALES[oct] / NOISE_BASE_PX
                        noiseVal += NOISE_WEIGHTS[oct] * noise2D.noise(px * f, py * f)
                    }
                    e = (e + na * noiseVal).coerceIn(-1.0, 1.0)
                }
                val i = py * imgW + px
                elevMap[i] = e; moistMap[i] = m; naMap[i] = na
            }
        }

        val pixels = IntArray(imgW * imgH)
        val lx = LIGHT[0]; val ly = LIGHT[1]; val lz = LIGHT[2]
        for (py in 0 until imgH) {
            for (px in 0 until imgW) {
                val i = py * imgW + px
                val e = elevMap[i]; val m = moistMap[i]; val na = naMap[i]
                val iL = if (px > 0) i - 1 else i; val iR = if (px < imgW - 1) i + 1 else i
                val iU = if (py > 0) i - imgW else i; val iD = if (py < imgH - 1) i + imgW else i
                val gx2 = (elevMap[iR] - elevMap[iL]) * 0.5 * SHADING_SCALE
                val gy2 = (elevMap[iD] - elevMap[iU]) * 0.5 * SHADING_SCALE
                val nLen = sqrt(gx2 * gx2 + gy2 * gy2 + 1)
                val nx = -gx2 / nLen; val ny = -gy2 / nLen; val nz = 1.0 / nLen
                val shade = min(2.0, AMBIENT + max(0.0, nx * lx + ny * ly + nz * lz))
                val slopeDiff = abs(elevMap[iR] - elevMap[iL]) + abs(elevMap[iD] - elevMap[iU])
                val outlineRaw = max(0.0, slopeDiff * OUTLINE_STRENGTH - OUTLINE_THRESHOLD)
                val outlineMask = min(1.0, na / 0.25)
                val outline = min(1.0, outlineRaw * outlineRaw) * outlineMask
                val (rBase, gBase, bBase) = sampleColormap(e, m)
                val s = shade * (1 - outline)
                val r = (rBase * s).coerceIn(0.0, 255.0).roundToInt()
                val gShaded = (gBase * s).coerceIn(0.0, 255.0).roundToInt()
                val b = (bBase * s).coerceIn(0.0, 255.0).roundToInt()
                pixels[i] = (0xFF shl 24) or (r shl 16) or (gShaded shl 8) or b
            }
        }
        return Triple(pixels, imgW, imgH)
    }

    /** Encode ARGB pixel data to PNG bytes using the JVM AWT library. */
    fun encodeToPng(pixels: IntArray, width: Int, height: Int): ByteArray? {
        return try {
            val image = java.awt.image.BufferedImage(width, height, java.awt.image.BufferedImage.TYPE_INT_ARGB)
            image.setRGB(0, 0, width, height, pixels, 0, width)
            val baos = java.io.ByteArrayOutputStream()
            javax.imageio.ImageIO.write(image, "png", baos)
            baos.toByteArray()
        } catch (_: Exception) {
            null
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Spatial grid
// ──────────────────────────────────────────────────────────────────────────────
private class SpatialGrid(private val cellSize: Double, imageWidth: Int) {
    private val gridW = ceil(imageWidth / cellSize).toInt() + 2
    private val cells = HashMap<Int, MutableList<Int>>()
    private fun key(cx: Int, cy: Int): Int = cy * gridW + cx
    fun insert(x: Double, y: Double, idx: Int) {
        val cx = floor(x / cellSize).toInt(); val cy = floor(y / cellSize).toInt()
        cells.getOrPut(key(cx, cy)) { mutableListOf() }.add(idx)
    }
    fun query(px: Double, py: Double, r: Int): List<Int> {
        val result = ArrayList<Int>()
        val cx = floor(px / cellSize).toInt(); val cy = floor(py / cellSize).toInt()
        for (dx in -r..r) for (dy in -r..r) cells[key(cx + dx, cy + dy)]?.let { result.addAll(it) }
        return result
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Simplex noise 2D
// ──────────────────────────────────────────────────────────────────────────────
private class BackendSimplexNoise2D(private val perm: IntArray) {
    fun noise(xin: Double, yin: Double): Double {
        val root3 = sqrt(3.0); val F2 = 0.5 * (root3 - 1.0); val G2 = (3.0 - root3) / 6.0
        val s = (xin + yin) * F2; val i = floor(xin + s).toInt(); val j = floor(yin + s).toInt()
        val t = (i + j) * G2; val X0 = i - t; val Y0 = j - t
        val x0 = xin - X0; val y0 = yin - Y0
        val (i1, j1) = if (x0 > y0) 1 to 0 else 0 to 1
        val x1 = x0 - i1 + G2; val y1 = y0 - j1 + G2
        val x2 = x0 - 1.0 + 2.0 * G2; val y2 = y0 - 1.0 + 2.0 * G2
        val ii = i and 255; val jj = j and 255
        val gi0 = perm[ii + perm[jj]] % 12
        val gi1 = perm[ii + i1 + perm[jj + j1]] % 12
        val gi2 = perm[ii + 1 + perm[jj + 1]] % 12
        val n0 = if (x0 * x0 + y0 * y0 < 0.5) { val t0 = 0.5 - x0 * x0 - y0 * y0; t0 * t0 * t0 * t0 * dot(grad3[gi0], x0, y0) } else 0.0
        val n1 = if (x1 * x1 + y1 * y1 < 0.5) { val t1 = 0.5 - x1 * x1 - y1 * y1; t1 * t1 * t1 * t1 * dot(grad3[gi1], x1, y1) } else 0.0
        val n2 = if (x2 * x2 + y2 * y2 < 0.5) { val t2 = 0.5 - x2 * x2 - y2 * y2; t2 * t2 * t2 * t2 * dot(grad3[gi2], x2, y2) } else 0.0
        return 70.0 * (n0 + n1 + n2)
    }
    private fun dot(g: IntArray, x: Double, y: Double): Double = g[0] * x + g[1] * y
    companion object {
        private val grad3 = arrayOf(
            intArrayOf(1,1), intArrayOf(-1,1), intArrayOf(1,-1), intArrayOf(-1,-1),
            intArrayOf(1,0), intArrayOf(-1,0), intArrayOf(1,0), intArrayOf(-1,0),
            intArrayOf(0,1), intArrayOf(0,-1), intArrayOf(0,1), intArrayOf(0,-1)
        )
        fun fromRandom(random: () -> Double): BackendSimplexNoise2D {
            val p = IntArray(256) { it }
            for (i in 0 until 256) {
                val r = i + floor(random() * (256 - i)).toInt()
                val tmp = p[i]; p[i] = p[r]; p[r] = tmp
            }
            val perm = IntArray(512) { p[it and 255] }
            return BackendSimplexNoise2D(perm)
        }
    }
}
