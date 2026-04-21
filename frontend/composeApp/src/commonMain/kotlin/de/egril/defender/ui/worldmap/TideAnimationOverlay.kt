package de.egril.defender.ui.worldmap

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.IntSize

/**
 * Shallow coastal water color used for the tide animation.
 * Matches the lighter blue band visible near the shoreline on the world map image.
 */
private val TIDE_WAVE_COLOR = Color(0xFF469FE2)

/**
 * Points tracing the continent's west-facing coastline in normalized image coordinates (x, y).
 * Each point is offset ~2–3 % of the image width seaward (to the left) from the actual
 * land boundary so that even at maximum stroke width the band stays mostly over water.
 * Points are ordered top-to-bottom along the coast.
 */
private val CONTINENT_COAST_POINTS = listOf(
    Offset(0.25f, 0.08f),
    Offset(0.22f, 0.10f),
    Offset(0.22f, 0.12f),
    Offset(0.19f, 0.14f),
    Offset(0.18f, 0.18f),
    Offset(0.17f, 0.20f),
    Offset(0.17f, 0.22f),
    Offset(0.17f, 0.26f),
    Offset(0.17f, 0.28f),
    Offset(0.13f, 0.30f),
    Offset(0.12f, 0.32f),
    Offset(0.12f, 0.34f),
    Offset(0.12f, 0.36f),
    Offset(0.12f, 0.38f),
    Offset(0.09f, 0.40f),
    Offset(0.08f, 0.42f),
    Offset(0.07f, 0.44f),
    Offset(0.06f, 0.46f),
    Offset(0.05f, 0.48f),
    Offset(0.02f, 0.50f),
    Offset(0.06f, 0.52f),
    Offset(0.07f, 0.54f),
    Offset(0.08f, 0.56f),
    Offset(0.09f, 0.58f),
    Offset(0.10f, 0.60f),
    Offset(0.17f, 0.62f),
    Offset(0.17f, 0.64f),
    Offset(0.17f, 0.66f),
    Offset(0.17f, 0.68f),
    Offset(0.18f, 0.70f),
    Offset(0.18f, 0.72f),
    Offset(0.18f, 0.74f),
    Offset(0.19f, 0.78f),
    Offset(0.22f, 0.80f),
    Offset(0.22f, 0.82f),
)

/** Center of the lower-left island in normalized image coordinates. */
private val ISLAND_CENTER = Offset(0.080f, 0.875f)
/** Island horizontal radius as a fraction of image width. */
private const val ISLAND_RADIUS_X = 0.045f
/** Island vertical radius as a fraction of image width (image-width-relative for consistency). */
private const val ISLAND_RADIUS_Y = 0.038f

/** Duration of one half-cycle (expand OR contract) in milliseconds — slow ambient tide. */
private const val TIDE_HALF_CYCLE_MS = 13500
/** Stroke width at minimum tide (contracted) as a fraction of image width. */
private const val MIN_STROKE_FRACTION = 0.003f
/** Stroke width at maximum tide (expanded) as a fraction of image width. */
private const val MAX_STROKE_FRACTION = 0.018f
/** Fixed alpha for the tide color — constant so the band is always visible. */
private const val TIDE_ALPHA = 0.50f

/**
 * Tide animation overlay for the world map.
 *
 * Draws exactly **two** semi-transparent blue stroke shapes that simulate a slow
 * coastal tide — one tracing the continent's west-facing shoreline and one encircling
 * the small island in the lower-left:
 *
 * - The stroke width pulses from [MIN_STROKE_FRACTION] to [MAX_STROKE_FRACTION] of the
 *   image width and back, creating a band that expands toward open sea and then contracts
 *   back to the shore.
 * - [RepeatMode.Reverse] makes the motion genuinely forth-and-back with no abrupt reset.
 * - Both shapes animate together with the same phase, so the tide appears uniform.
 * - The animation respects [AppSettings.enableAnimations] (gated in the call site).
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

    // Expand (0→1) then contract (1→0) — genuine tide forth-and-back.
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

        val strokeWidth = imageWidth * (MIN_STROKE_FRACTION + tideProgress * (MAX_STROKE_FRACTION - MIN_STROKE_FRACTION))
        val color = TIDE_WAVE_COLOR.copy(alpha = TIDE_ALPHA)
        val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)

        // --- 1. Continent coast polyline ---
        val coastPath = Path().apply {
            val first = CONTINENT_COAST_POINTS.first()
            moveTo(imgOffsetX + first.x * imageWidth, imgOffsetY + first.y * imageHeight)
            for (i in 1 until CONTINENT_COAST_POINTS.size) {
                val pt = CONTINENT_COAST_POINTS[i]
                lineTo(imgOffsetX + pt.x * imageWidth, imgOffsetY + pt.y * imageHeight)
            }
        }
        drawPath(path = coastPath, color = color, style = stroke)

        // --- 2. Island ring ---
        val cx = imgOffsetX + ISLAND_CENTER.x * imageWidth
        val cy = imgOffsetY + ISLAND_CENTER.y * imageHeight
        val rx = ISLAND_RADIUS_X * imageWidth
        val ry = ISLAND_RADIUS_Y * imageWidth
        drawOval(
            color = color,
            topLeft = Offset(cx - rx, cy - ry),
            size = Size(rx * 2f, ry * 2f),
            style = stroke
        )
    }
}
