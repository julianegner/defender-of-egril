package de.egril.defender.ui.worldmap

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.IntSize

/**
 * Shallow coastal water color used for the tide animation waves.
 * Matches the lighter blue band visible near the shoreline on the world map image.
 */
private val TIDE_WAVE_COLOR = Color(0xFF469FE2)

/**
 * Shore anchor points in normalized image coordinates (x, y).
 * Positioned a few percent seaward of the actual land boundary so that the
 * expanding rings stay over water and do not bleed onto the green/yellow land areas.
 * The coast runs roughly from upper-centre down to lower-left.
 */
private val SHORE_ANCHORS = listOf(
    Offset(0.22f, 0.12f),   // Upper northern coast
    Offset(0.16f, 0.17f),   // Upper coast
    Offset(0.14f, 0.23f),   // Mid-upper coast
    Offset(0.12f, 0.30f),   // Mid coast
    Offset(0.09f, 0.37f),   // Lower-mid coast
    Offset(0.07f, 0.47f),   // Lower coast
)

private const val WAVE_COUNT = 4
/** Duration of one half-cycle (expand OR contract) in milliseconds — ~3× the original 4500 ms. */
private const val WAVE_HALF_CYCLE_MS = 13500
/** Maximum outward ring expansion as a fraction of the image width (~1.6 %). */
private const val MAX_WAVE_EXPANSION_FRACTION = 0.016f
/** Base ring radius as a fraction of the image width (~0.8 %). */
private const val BASE_RADIUS_FRACTION = 0.008f
/** Maximum ring stroke alpha. */
private const val MAX_WAVE_ALPHA = 0.55f
/** Stroke width as a fraction of the image width. */
private const val WAVE_STROKE_FRACTION = 0.005f
/** Vertical-to-horizontal scale of each wave oval, reflecting the roughly N–S coast orientation. */
private const val WAVE_OVAL_VERTICAL_SCALE = 1.8f

/**
 * Tide animation overlay for the world map.
 *
 * Draws thin semi-transparent stroke rings along the coastline that expand
 * seaward and then contract back — simulating a slow, ambient tide.
 *
 * - Rings are stroke-only (not filled) so they are confined to a narrow coastal band
 *   and never cover land.
 * - [RepeatMode.Reverse] makes the motion genuinely forth-and-back.
 * - Multiple rings are staggered in phase for a continuous cascading look.
 * - The animation respects [AppSettings.enableAnimations] (gated in the call site).
 *
 * @param containerSize Pixel size of the host container (unused in drawing; reserved for API
 *                      symmetry with other overlay composables).
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

    // Forward (expand) + reverse (contract) = true tide forth-and-back motion.
    val tideProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = WAVE_HALF_CYCLE_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tide_wave"
    )

    Canvas(modifier = modifier) {
        val cw = size.width
        val ch = size.height

        // Calculate actual image bounds within the container.
        // ContentScale.Fit letterboxes the image, so we must account for that offset.
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

        val maxExpansion = imageWidth * MAX_WAVE_EXPANSION_FRACTION
        val strokeWidth = imageWidth * WAVE_STROKE_FRACTION

        for (waveIndex in 0 until WAVE_COUNT) {
            // Stagger each ring evenly in phase so they cascade continuously.
            val phaseOffset = waveIndex.toFloat() / WAVE_COUNT
            val phase = (tideProgress + phaseOffset) % 1f

            // Expansion grows from 0 to maxExpansion as phase goes 0→1.
            val expansion = phase * maxExpansion

            // Alpha is greatest near the shore (phase ≈ 0) and fades at full expansion.
            val alpha = (1f - phase) * MAX_WAVE_ALPHA

            for (anchor in SHORE_ANCHORS) {
                val cx = imgOffsetX + anchor.x * imageWidth
                val cy = imgOffsetY + anchor.y * imageHeight

                val baseRadius = imageWidth * BASE_RADIUS_FRACTION
                val rx = baseRadius + expansion
                val ry = rx * WAVE_OVAL_VERTICAL_SCALE

                drawOval(
                    color = TIDE_WAVE_COLOR.copy(alpha = alpha),
                    topLeft = Offset(cx - rx, cy - ry),
                    size = Size(rx * 2f, ry * 2f),
                    style = Stroke(width = strokeWidth)
                )
            }
        }
    }
}
