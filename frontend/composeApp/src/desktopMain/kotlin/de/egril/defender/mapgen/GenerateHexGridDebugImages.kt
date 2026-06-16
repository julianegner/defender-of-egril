package de.egril.defender.mapgen

import de.egril.defender.editor.EditorJsonSerializer
import de.egril.defender.editor.TileType
import java.awt.AlphaComposite
import java.awt.BasicStroke
import java.awt.Color
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin

/**
 * Generates debug overlay images by drawing the hex grid (tiles coloured by type) on top of the
 * map background PNG.  The output images are committed to git alongside the map sources but are
 * placed outside `composeResources` so they are never part of any build or release.
 *
 * Hex geometry matches the in-game rendering (HexagonalMapView + HexagonalGridConstants) so the
 * overlay is pixel-perfect against what the player actually sees.  The background PNG is scaled
 * to the game grid dimensions before the overlay is applied.
 *
 * Both light-mode and dark-mode colour variants are produced.
 */
object GenerateHexGridDebugImages {

    // ---------------------------------------------------------------------------
    // Hex grid geometry — must match HexagonalMapView / HexagonalGridConstants
    // ---------------------------------------------------------------------------
    private const val HEX_SIZE = 40.0
    private val SQRT3 = kotlin.math.sqrt(3.0)
    private val HEX_WIDTH = HEX_SIZE * SQRT3      // ~69.28 px  (flat-to-flat width)
    private const val HEX_HEIGHT = HEX_SIZE * 2.0  // 80 px      (point-to-point height)

    // Row-to-row layout step: HEX_HEIGHT * 0.75 + VERTICAL_SPACING_ADJUSTMENT
    // Must match HexagonalGridConstants: VERTICAL_SPACING_ADJUSTMENT = -7f
    // Game computes: spacedBy(-hexHeight + verticalSpacing + adj) → cell-to-cell = hexHeight + spacing
    //   = 80 + (-80 + 60 + (-7)) = 80 - 27 = 53
    private const val VERTICAL_SPACING_ADJUSTMENT = -7.0   // matches HexagonalGridConstants.VERTICAL_SPACING_ADJUSTMENT
    private val EFFECTIVE_ROW_STEP = HEX_HEIGHT * 0.75 + VERTICAL_SPACING_ADJUSTMENT  // 53 px (layout step)

    // HexagonalMapView applies a per-row visual correction: .offset(y = (-(y - 1)).dp)
    // This shifts row y by -(y-1) dp from its layout position, so visual top of row y is:
    //   y * EFFECTIVE_ROW_STEP - (y - 1) = y * (EFFECTIVE_ROW_STEP - 1) + 1
    // Visual row-to-row step = EFFECTIVE_ROW_STEP - 1 = 52 px
    // Row 0 is at visual y = 1 px (not 0)
    private val VISUAL_ROW_STEP = EFFECTIVE_ROW_STEP - 1.0  // 52 px
    private const val ROW_ZERO_VISUAL_OFFSET = 1.0           // row 0 is shifted 1 px down

    private const val HORIZONTAL_SPACING = -10.0            // matches HexagonalGridConstants.HORIZONTAL_SPACING
    private const val ODD_ROW_OFFSET_RATIO = 0.42           // matches HexagonalGridConstants.ODD_ROW_OFFSET_RATIO

    // Drawing circumradius — must match HexagonShape.createOutline():
    //   radius = min(HEX_WIDTH, HEX_HEIGHT) / 2 = HEX_WIDTH / 2 ≈ 34.64
    private val HEX_DRAW_RADIUS = HEX_WIDTH / 2.0           // ≈ 34.64 px

    // Opacity of the tile fill composite (0.0 = transparent, 1.0 = opaque)
    private const val FILL_ALPHA = 0.30f
    // Opacity of the hex border lines
    private const val BORDER_ALPHA = 0.80f
    // Stroke width for the hex border
    private const val BORDER_STROKE = 1.5f

    // ---------------------------------------------------------------------------
    // Tile colours — matches getTileColor() in TileUtils.kt (light and dark modes)
    // ---------------------------------------------------------------------------
    private val TILE_COLORS_LIGHT = mapOf(
        TileType.PATH to Color(0x8B4513),
        TileType.BUILD_AREA to Color(0x90EE90),
        TileType.NO_PLAY to Color(0x404040),
        TileType.SPAWN_POINT to Color(0xFF0000),
        TileType.TARGET to Color(0x0000FF),
        TileType.RIVER to Color(0x4682B4)
    )

    private val TILE_COLORS_DARK = mapOf(
        TileType.PATH to Color(0x4A2F1A),
        TileType.BUILD_AREA to Color(0x456C2E),
        TileType.NO_PLAY to Color(0x1A1A1A),
        TileType.SPAWN_POINT to Color(0x8B0000),
        TileType.TARGET to Color(0x00008B),
        TileType.RIVER to Color(0x1E3A5F)
    )

    // ---------------------------------------------------------------------------
    // Geometry helpers
    // ---------------------------------------------------------------------------

    /**
     * Returns the pixel centre of tile ([gx], [gy]) in the game grid coordinate system
     * (no padding; first cell visual top is at y=1 due to the per-row offset correction).
     *
     * Mirrors the Compose layout in HexagonalMapView:
     *   cx = gx * (HEX_WIDTH + HORIZONTAL_SPACING) + oddRowOffset + HEX_WIDTH/2
     *   cy = gy * VISUAL_ROW_STEP + ROW_ZERO_VISUAL_OFFSET + HEX_HEIGHT/2
     *
     * The VISUAL_ROW_STEP (52) differs from the layout step EFFECTIVE_ROW_STEP (53) because
     * HexagonalMapView applies .offset(y = (-(y-1)).dp) per row, which compresses the visual
     * row spacing by 1 px per row while also shifting row 0 down by 1 px.
     */
    private fun hexCenter(gx: Int, gy: Int): Pair<Double, Double> {
        val rowOffset = if (gy % 2 == 1) HEX_WIDTH * ODD_ROW_OFFSET_RATIO else 0.0
        val cx = gx * (HEX_WIDTH + HORIZONTAL_SPACING) + rowOffset + HEX_WIDTH / 2
        val cy = gy * VISUAL_ROW_STEP + ROW_ZERO_VISUAL_OFFSET + HEX_HEIGHT / 2
        return Pair(cx, cy)
    }

    /**
     * Computes the pixel dimensions of the game grid canvas for a map of [gridWidth] × [gridHeight]
     * tiles, using the same formula as [de.egril.defender.ui.gameplay.GameMap] `hexMapSizePx`.
     */
    private fun gameGridSize(gridWidth: Int, gridHeight: Int): Pair<Int, Int> {
        val oddOffset = if (gridHeight > 1) HEX_WIDTH * ODD_ROW_OFFSET_RATIO else 0.0
        val w = ceil(gridWidth * HEX_WIDTH + (gridWidth - 1) * HORIZONTAL_SPACING + oddOffset).toInt()
        val h = ceil((gridHeight - 1) * EFFECTIVE_ROW_STEP + HEX_HEIGHT).toInt()
        return Pair(w, h)
    }

    /** Returns the 6 vertices of a pointy-top hexagon centred at (cx, cy). */
    private fun hexVertices(cx: Double, cy: Double): Array<IntArray> {
        return Array(6) { i ->
            val angle = PI * (60.0 * i - 30.0) / 180.0
            intArrayOf(
                (cx + HEX_DRAW_RADIUS * cos(angle)).toInt(),
                (cy + HEX_DRAW_RADIUS * sin(angle)).toInt()
            )
        }
    }

    // ---------------------------------------------------------------------------
    // Core generator
    // ---------------------------------------------------------------------------

    /**
     * Generate both light and dark debug overlay PNGs for [jsonFile] (and its companion
     * [pngFile]).  Writes results to [outputDir] using the naming convention
     * `<map_id>_hex_grid_light.png` / `<map_id>_hex_grid_dark.png`.
     *
     * @return true if both variants were generated successfully.
     */
    fun generateOne(jsonFile: File, pngFile: File, outputDir: File): Boolean {
        return try {
            val json = jsonFile.readText()
            val map = EditorJsonSerializer.deserializeMap(json)
            if (map == null) {
                println("  ERROR: Could not parse map JSON: ${jsonFile.name}")
                return false
            }

            val background = ImageIO.read(pngFile)
                ?: run {
                    println("  ERROR: Could not read background PNG: ${pngFile.name}")
                    return false
                }

            print("  Generating debug overlays for ${jsonFile.nameWithoutExtension}...")

            outputDir.mkdirs()

            val (gameW, gameH) = gameGridSize(map.width, map.height)
            val lightOut = File(outputDir, "${map.id}_hex_grid_light.png")
            val darkOut = File(outputDir, "${map.id}_hex_grid_dark.png")

            renderOverlay(background, map.tiles, map.width, map.height, TILE_COLORS_LIGHT, lightOut)
            renderOverlay(background, map.tiles, map.width, map.height, TILE_COLORS_DARK, darkOut)

            println(" OK (${background.width}x${background.height}px -> ${gameW}x${gameH}px)")
            true
        } catch (e: Exception) {
            println(" FAILED: ${e.message}")
            false
        }
    }

    private fun renderOverlay(
        background: BufferedImage,
        tiles: Map<String, TileType>,
        gridWidth: Int,
        gridHeight: Int,
        colorMap: Map<TileType, Color>,
        outFile: File
    ) {
        val (gameW, gameH) = gameGridSize(gridWidth, gridHeight)

        // Create output at game grid dimensions
        val result = BufferedImage(gameW, gameH, BufferedImage.TYPE_INT_RGB)
        val g = result.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)

        // Scale background to game grid dimensions (matches in-game FillBounds behaviour)
        g.drawImage(background, 0, 0, gameW, gameH, null)

        // Draw hex fills (semi-transparent) — only tiles within the declared map bounds
        g.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, FILL_ALPHA)
        for (gy in 0 until gridHeight) {
            for (gx in 0 until gridWidth) {
                val tileType = tiles["$gx,$gy"] ?: TileType.NO_PLAY
                val color = colorMap[tileType] ?: colorMap[TileType.NO_PLAY] ?: Color.DARK_GRAY
                val (cx, cy) = hexCenter(gx, gy)
                val verts = hexVertices(cx, cy)
                val xPoints = IntArray(6) { verts[it][0] }
                val yPoints = IntArray(6) { verts[it][1] }
                g.color = color
                g.fillPolygon(xPoints, yPoints, 6)
            }
        }

        // Draw hex borders (slightly more opaque)
        g.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, BORDER_ALPHA)
        g.stroke = BasicStroke(BORDER_STROKE)
        for (gy in 0 until gridHeight) {
            for (gx in 0 until gridWidth) {
                val tileType = tiles["$gx,$gy"] ?: TileType.NO_PLAY
                val color = colorMap[tileType] ?: colorMap[TileType.NO_PLAY] ?: Color.DARK_GRAY
                val (cx, cy) = hexCenter(gx, gy)
                val verts = hexVertices(cx, cy)
                val xPoints = IntArray(6) { verts[it][0] }
                val yPoints = IntArray(6) { verts[it][1] }
                g.color = color
                g.drawPolygon(xPoints, yPoints, 6)
            }
        }

        g.dispose()
        ImageIO.write(result, "PNG", outFile)
    }

    // ---------------------------------------------------------------------------
    // Batch generation
    // ---------------------------------------------------------------------------

    /**
     * Generate debug overlay images for all map JSON+PNG pairs in [mapsDir].
     * Skips a map if both output images are already newer than the source PNG
     * (unless [forceRegenerate] is true).
     *
     * @param mapsDir   Directory containing `map_*.json` + `map_*.png` pairs.
     * @param outputDir Directory where debug images are written.
     *                  Defaults to `map-debug-images/` next to [mapsDir].
     * @param forceRegenerate If true, regenerate even when outputs are up-to-date.
     */
    fun generateAll(
        mapsDir: File,
        outputDir: File = File(mapsDir.parentFile, "map-debug-images"),
        forceRegenerate: Boolean = false
    ) {
        require(mapsDir.isDirectory) { "Maps directory not found: ${mapsDir.absolutePath}" }

        val jsonFiles = mapsDir.listFiles { f -> f.extension == "json" && f.name.startsWith("map_") }
            ?: emptyArray()

        if (jsonFiles.isEmpty()) {
            println("GenerateHexGridDebugImages: No map JSON files found in ${mapsDir.absolutePath}")
            return
        }

        println("GenerateHexGridDebugImages: Processing ${jsonFiles.size} map(s) -> ${outputDir.absolutePath}")

        var generated = 0
        var skipped = 0
        var failed = 0

        for (jsonFile in jsonFiles.sortedBy { it.name }) {
            val pngFile = File(jsonFile.parentFile, "${jsonFile.nameWithoutExtension}.png")
            if (!pngFile.exists()) {
                println("  Skipping (no PNG): ${jsonFile.name}")
                skipped++
                continue
            }

            // Skip if both output files are already up-to-date
            if (!forceRegenerate) {
                val json = jsonFile.readText()
                val mapId = EditorJsonSerializer.deserializeMap(json)?.id
                if (mapId != null) {
                    val lightOut = File(outputDir, "${mapId}_hex_grid_light.png")
                    val darkOut = File(outputDir, "${mapId}_hex_grid_dark.png")
                    val sourceTime = pngFile.lastModified()
                    if (lightOut.exists() && darkOut.exists()
                        && lightOut.lastModified() >= sourceTime
                        && darkOut.lastModified() >= sourceTime
                    ) {
                        println("  Skipping (up-to-date): ${pngFile.name}")
                        skipped++
                        continue
                    }
                }
            }

            val success = generateOne(jsonFile, pngFile, outputDir)
            if (success) generated++ else failed++
        }

        println("GenerateHexGridDebugImages: Done. Generated=$generated, Skipped=$skipped, Failed=$failed")
    }
}

/**
 * Entry point when run as a standalone JVM program via the `generateHexGridDebugImages` Gradle task.
 *
 * Arguments:
 *   [0] = path to the maps directory (optional, defaults to repository maps directory)
 *   [1] = path to the output directory (optional, defaults to `map-debug-images/` next to maps dir)
 *   [--force] = regenerate all images even if already up-to-date (optional, any position)
 */
fun main(args: Array<String>) {
    val positionalArgs = args.filter { it != "--force" }
    val force = args.any { it == "--force" }

    val mapsDir = if (positionalArgs.isNotEmpty()) {
        File(positionalArgs[0])
    } else {
        val projectRoot = File(System.getProperty("user.dir"))
        File(projectRoot, "composeApp/src/commonMain/composeResources/files/repository/maps")
    }

    val outputDir = if (positionalArgs.size >= 2) {
        File(positionalArgs[1])
    } else {
        File(mapsDir.parentFile, "map-debug-images")
    }

    GenerateHexGridDebugImages.generateAll(mapsDir, outputDir = outputDir, forceRegenerate = force)
}
