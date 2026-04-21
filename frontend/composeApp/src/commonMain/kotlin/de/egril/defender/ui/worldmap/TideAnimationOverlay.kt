package de.egril.defender.ui.worldmap

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize

/**
 * Shallow coastal water color used for the tide animation waves.
 * Matches the lighter blue band visible near the shoreline on the world map image.
 */
private val TIDE_WAVE_COLOR = Color(0xFF469FE2)

/**
 * Shore anchor points in normalized image coordinates (x, y).
 * These define the coastline along which tide waves emanate outward.
 * Derived from the worldmap image analysis: the coast runs from the
 * upper-right area diagonally to the lower-center, forming the boundary
 * between the lighter coastal water and the darker deep sea.
 */
private val SHORE_ANCHORS = listOf(
    Offset(0.30f, 0.12f),   // Upper northern shore
    Offset(0.22f, 0.17f),   // Upper coast
    Offset(0.20f, 0.23f),   // Mid-upper coast
    Offset(0.18f, 0.29f),   // Mid coast (near arrow region in the issue)
    Offset(0.14f, 0.35f),   // Lower-mid coast
    Offset(0.11f, 0.46f),   // Lower coast
)

private const val WAVE_COUNT = 5
private const val WAVE_CYCLE_MS = 4500
/** Maximum outward expansion of each wave ring, as a fraction of the image width. */
private const val MAX_WAVE_EXPANSION_FRACTION = 0.09f
private const val BASE_RADIUS_FRACTION = 0.045f   // 4.5% base radius
private const val MAX_WAVE_ALPHA = 0.22f           // Maximum wave opacity
/** Vertical-to-horizontal scale of each wave oval, reflecting the roughly N–S coast orientation. */
private const val WAVE_OVAL_VERTICAL_SCALE = 1.8f

/**
 * Tide animation overlay for the world map.
 *
 * Draws semi-transparent wave rings that ripple outward from the coastline,
 * simulating the shallow coastal water expanding toward the deeper sea and
 * then receding — creating an ambient tide effect.
 *
 * Multiple waves are staggered in phase so they roll continuously.
 * The animation only runs when [AppSettings.enableAnimations] is true.
 *
 * @param containerSize Current pixel size of the host container.
 * @param imageAspectRatio Aspect ratio (width/height) of the world map background image,
 *                         used to compute image bounds inside the container.
 * @param modifier Modifier applied to the full-size Canvas.
 */
@Composable
fun TideAnimationOverlay(
    containerSize: IntSize,
    imageAspectRatio: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "tide")

    val tideProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = WAVE_CYCLE_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "tide_wave"
    )

    Canvas(modifier = modifier) {
        val cw = size.width
        val ch = size.height

        // Calculate actual image bounds within the container (same logic as other overlays).
        // ContentScale.Fit is used on the background image, so we must account for letterboxing.
        val containerAspectRatio = cw / ch.coerceAtLeast(1f)
        val (imageWidth, imageHeight, imgOffsetX, imgOffsetY) = if (containerAspectRatio > imageAspectRatio) {
            // Container is wider than image — fit to height, center horizontally
            val iH = ch
            val iW = iH * imageAspectRatio
            val offX = (cw - iW) / 2f
            listOf(iW, iH, offX, 0f)
        } else {
            // Container is taller than image — fit to width, center vertically
            val iW = cw
            val iH = iW / imageAspectRatio
            val offY = (ch - iH) / 2f
            listOf(iW, iH, 0f, offY)
        }

        val maxExpansion = imageWidth * MAX_WAVE_EXPANSION_FRACTION

        for (waveIndex in 0 until WAVE_COUNT) {
            // Stagger each wave ring evenly so they cascade continuously
            val phaseOffset = waveIndex.toFloat() / WAVE_COUNT
            val phase = (tideProgress + phaseOffset) % 1f

            // Wave expands outward from coast toward open sea as phase increases
            val expansion = phase * maxExpansion

            // Alpha fades to zero at full expansion — wave is brightest near the shore
            val alpha = (1f - phase) * MAX_WAVE_ALPHA

            for (anchor in SHORE_ANCHORS) {
                val cx = imgOffsetX + anchor.x * imageWidth
                val cy = imgOffsetY + anchor.y * imageHeight

                // Elongated oval oriented along the coastline:
                //   radiusX (horizontal) grows with the wave expanding toward the sea
                //   radiusY (vertical) is 1.8× wider to match the roughly N-S coast shape
                val baseRadius = imageWidth * BASE_RADIUS_FRACTION
                val rx = baseRadius + expansion
                val ry = rx * WAVE_OVAL_VERTICAL_SCALE

                drawOval(
                    color = TIDE_WAVE_COLOR.copy(alpha = alpha),
                    topLeft = Offset(cx - rx, cy - ry),
                    size = Size(rx * 2f, ry * 2f)
                )
            }
        }
    }
}
