package de.egril.defender.ui.worldmap

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.IntSize

/**
 * Shallow coastal water color used for the tide animation.
 * Matches the lighter blue band visible near the shoreline on the world map image.
 */
private val TIDE_WAVE_COLOR = Color(0xFF469FE2)

/**
 * Full continent perimeter polygon in normalized image coordinates (x/imageWidth, y/imageHeight).
 *
 * Points are traced **clockwise** (screen coords, y-down) starting from the north-west tip,
 * continuing east along the north coast, then south along the east coast (including the large
 * east bay at y≈0.61–0.67), then west along the south coast, then north up the west coast.
 *
 * Source: pixel analysis of the 2048×1622 world_map_background.png.
 */
private val CONTINENT_POLYGON = listOf(
    // NW tip → North coast (going east)
    Offset(0.261f, 0.089f), Offset(0.300f, 0.084f), Offset(0.360f, 0.094f),
    Offset(0.440f, 0.076f), Offset(0.520f, 0.067f), Offset(0.600f, 0.062f),
    Offset(0.640f, 0.059f), Offset(0.760f, 0.048f), Offset(0.800f, 0.081f),
    Offset(0.840f, 0.092f),
    // NE corner → East coast (going south)
    Offset(0.880f, 0.096f), Offset(0.897f, 0.120f), Offset(0.914f, 0.179f),
    Offset(0.912f, 0.200f), Offset(0.854f, 0.280f), Offset(0.847f, 0.320f),
    Offset(0.887f, 0.380f), Offset(0.920f, 0.440f), Offset(0.945f, 0.480f),
    Offset(0.958f, 0.560f), Offset(0.896f, 0.600f),
    // East bay indentation (water intrudes at y≈0.61–0.67)
    Offset(0.716f, 0.610f), Offset(0.707f, 0.660f),
    // Bay closes — coast resumes south
    Offset(0.775f, 0.680f), Offset(0.827f, 0.720f), Offset(0.868f, 0.740f),
    Offset(0.890f, 0.770f),
    // SE corner → South coast (going west)
    Offset(0.882f, 0.840f), Offset(0.877f, 0.870f), Offset(0.800f, 0.906f),
    Offset(0.740f, 0.842f), Offset(0.710f, 0.888f), Offset(0.650f, 0.898f),
    Offset(0.590f, 0.873f), Offset(0.560f, 0.862f), Offset(0.500f, 0.844f),
    Offset(0.470f, 0.861f), Offset(0.440f, 0.856f), Offset(0.410f, 0.838f),
    Offset(0.380f, 0.823f), Offset(0.350f, 0.809f), Offset(0.320f, 0.806f),
    Offset(0.290f, 0.816f), Offset(0.260f, 0.831f),
    // SW corner → West coast (going north)
    Offset(0.240f, 0.820f), Offset(0.226f, 0.780f), Offset(0.211f, 0.740f),
    Offset(0.207f, 0.700f), Offset(0.196f, 0.660f), Offset(0.188f, 0.620f),
    Offset(0.130f, 0.600f), Offset(0.100f, 0.560f), Offset(0.083f, 0.520f),
    Offset(0.044f, 0.500f), Offset(0.075f, 0.480f), Offset(0.102f, 0.440f),
    Offset(0.118f, 0.400f), Offset(0.143f, 0.380f), Offset(0.134f, 0.340f),
    Offset(0.152f, 0.300f), Offset(0.193f, 0.260f), Offset(0.199f, 0.220f),
    Offset(0.203f, 0.180f), Offset(0.233f, 0.160f), Offset(0.246f, 0.120f),
    Offset(0.251f, 0.100f), // → close() back to NW tip
)

/** Center of the lower-left island in normalized image coordinates. */
private val ISLAND_CENTER = Offset(0.080f, 0.875f)

/**
 * Island half-width as a fraction of [imageWidth].
 * Actual island spans x ≈ 0.050–0.110 → half-width ≈ 0.030; add a small margin.
 */
private const val ISLAND_RADIUS_X = 0.035f

/**
 * Island half-height as a fraction of [imageHeight].
 * Actual island spans y ≈ 0.850–0.900 → half-height ≈ 0.025; add a small margin.
 */
private const val ISLAND_RADIUS_Y = 0.028f

/** Duration of one half-cycle (expand OR contract) in milliseconds — slow ambient tide. */
private const val TIDE_HALF_CYCLE_MS = 13500

/**
 * Maximum seaward expansion of the tide band as a fraction of [imageWidth].
 * The stroke centered on the coast has width = 2× this value; the sea clip removes the
 * inward (land-facing) half, so the visible band expands exactly [MAX_EXPANSION_FRACTION]
 * toward the open sea.
 */
private const val MAX_EXPANSION_FRACTION = 0.020f

/** Alpha of the tide color — constant so the band is always clearly visible. */
private const val TIDE_ALPHA = 0.55f

/**
 * Tide animation overlay for the world map.
 *
 * Draws exactly **two** animated elements:
 * 1. A stroke along the full continent perimeter (all four coasts).
 * 2. A stroke oval around the small island in the lower-left.
 *
 * Both are drawn centred on the coastline and clipped to a **sea region** (EvenOdd path =
 * image rect − continent polygon − island oval).  The clip removes the land-facing half of
 * each stroke, so the visible tide band:
 * - has its **inner edge pinned to the coastline** at all times, and
 * - has its **outer edge expand seaward** as [tideProgress] grows from 0 → 1.
 *
 * [RepeatMode.Reverse] then contracts the band back to nothing, producing a genuine
 * forth-and-back tide without any abrupt reset.
 *
 * @param containerSize Pixel size of the host container (reserved for API symmetry).
 * @param imageAspectRatio Aspect ratio (width/height) of the world map background image.
 * @param modifier Modifier applied to the full-size Canvas.
 */
@Composable
fun TideAnimationOverlay(
    containerSize: IntSize,
    imageAspectRatio: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "tide")

    // Expands 0→1 then contracts 1→0 — true forth-and-back without abrupt reset.
    val tideProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = TIDE_HALF_CYCLE_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tide_progress"
    )

    Canvas(modifier = modifier) {
        val cw = size.width
        val ch = size.height

        // Compute actual image bounds within the container (ContentScale.Fit letterboxes).
        val containerAspectRatio = cw / ch.coerceAtLeast(1f)
        val (imageWidth, imageHeight, imgOffsetX, imgOffsetY) = if (containerAspectRatio > imageAspectRatio) {
            val iH = ch
            val iW = iH * imageAspectRatio
            listOf(iW, iH, (cw - iW) / 2f, 0f)
        } else {
            val iW = cw
            val iH = iW / imageAspectRatio
            listOf(iW, iH, 0f, (ch - iH) / 2f)
        }

        // Helpers: convert normalised image coords → canvas coords.
        fun nx(x: Float) = imgOffsetX + x * imageWidth
        fun ny(y: Float) = imgOffsetY + y * imageHeight

        // ── Continent polygon (closed) ──────────────────────────────────────────────
        val continentPath = Path().apply {
            moveTo(nx(CONTINENT_POLYGON[0].x), ny(CONTINENT_POLYGON[0].y))
            for (i in 1 until CONTINENT_POLYGON.size) {
                lineTo(nx(CONTINENT_POLYGON[i].x), ny(CONTINENT_POLYGON[i].y))
            }
            close()
        }

        // ── Island oval bounds ──────────────────────────────────────────────────────
        val iCx = nx(ISLAND_CENTER.x)
        val iCy = ny(ISLAND_CENTER.y)
        val iRx = ISLAND_RADIUS_X * imageWidth
        val iRy = ISLAND_RADIUS_Y * imageHeight
        val islandRect = Rect(iCx - iRx, iCy - iRy, iCx + iRx, iCy + iRy)

        // ── Sea clip path ────────────────────────────────────────────────────────────
        // EvenOdd fill: a point inside an ODD number of sub-paths is "inside" the clip.
        //   • Open sea (inside image rect only)                  → 1 crossing → clipped IN  ✓
        //   • Continent land (inside image rect + continent)     → 2 crossings → clipped OUT ✓
        //   • Island land (inside image rect + island oval)      → 2 crossings → clipped OUT ✓
        val seaClipPath = Path().apply {
            fillType = PathFillType.EvenOdd
            // 1. Image boundary
            addRect(Rect(imgOffsetX, imgOffsetY, imgOffsetX + imageWidth, imgOffsetY + imageHeight))
            // 2. Continent polygon — creates a "hole" over land
            moveTo(nx(CONTINENT_POLYGON[0].x), ny(CONTINENT_POLYGON[0].y))
            for (i in 1 until CONTINENT_POLYGON.size) {
                lineTo(nx(CONTINENT_POLYGON[i].x), ny(CONTINENT_POLYGON[i].y))
            }
            close()
            // 3. Island oval — another hole over island land
            addOval(islandRect)
        }

        // Stroke width = 2 × seaward expansion (stroke is centred on the coast; clip removes
        // the inward/land half, leaving a band of exactly [expansion] toward the open sea).
        val expansion = tideProgress * MAX_EXPANSION_FRACTION * imageWidth
        val strokeWidth = expansion * 2f

        if (strokeWidth > 0f) {
            val tideColor = TIDE_WAVE_COLOR.copy(alpha = TIDE_ALPHA)
            val tideStroke = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)

            clipPath(seaClipPath) {
                // Continent perimeter — only the seaward half of the stroke survives the clip.
                drawPath(path = continentPath, color = tideColor, style = tideStroke)
                // Island — only the outward (seaward) half survives the clip.
                drawOval(
                    color = tideColor,
                    topLeft = Offset(iCx - iRx, iCy - iRy),
                    size = Size(iRx * 2f, iRy * 2f),
                    style = tideStroke
                )
            }
        }
    }
}
