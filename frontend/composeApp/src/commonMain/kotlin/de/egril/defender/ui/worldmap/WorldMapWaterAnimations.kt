package de.egril.defender.ui.worldmap

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.IntSize
import de.egril.defender.editor.WorldMapData
import de.egril.defender.editor.WorldMapRiver
import de.egril.defender.editor.WorldMapSeaArea
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Overlay that renders water animations on the world map:
 * - Sea wave animations over defined sea areas
 * - River flow animations along defined river paths
 *
 * Animations are rendered only when [enableAnimations] is true.
 * Both animation types respect the animation setting from AppSettings.
 */
@Composable
fun BoxScope.WaterAnimationsOverlay(
    worldMapData: WorldMapData,
    containerSize: IntSize,
    imageAspectRatio: Float,
    enableAnimations: Boolean
) {
    // Always create transitions so composable calls are unconditional
    val infiniteTransition = rememberInfiniteTransition(label = "worldmap_water")

    // Sea wave phase: full cycle every 5 seconds
    val seaPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(5000, easing = LinearEasing)),
        label = "sea_wave_phase"
    )

    // River flow phase: particles travel from 0 to 1 every 2.5 seconds
    val riverPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2500, easing = LinearEasing)),
        label = "river_flow_phase"
    )

    if (!enableAnimations || containerSize.width == 0) return

    val seaAreas = worldMapData.seaAreas
    val rivers = worldMapData.rivers
    if (seaAreas.isEmpty() && rivers.isEmpty()) return

    // Calculate image bounds within the container (ContentScale.Fit)
    val containerAspectRatio = containerSize.width.toFloat() / containerSize.height.toFloat().coerceAtLeast(1f)
    val (imageWidth, imageHeight, imageOffsetX, imageOffsetY) = if (containerAspectRatio > imageAspectRatio) {
        val h = containerSize.height.toFloat()
        val w = h * imageAspectRatio
        val offsetX = (containerSize.width - w) / 2f
        listOf(w, h, offsetX, 0f)
    } else {
        val w = containerSize.width.toFloat()
        val h = w / imageAspectRatio
        val offsetY = (containerSize.height - h) / 2f
        listOf(w, h, 0f, offsetY)
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        // --- Sea wave animations ---
        for (seaArea in seaAreas) {
            val areaX = imageOffsetX + seaArea.x / 1000f * imageWidth
            val areaY = imageOffsetY + seaArea.y / 1000f * imageHeight
            val areaW = seaArea.width / 1000f * imageWidth
            val areaH = seaArea.height / 1000f * imageHeight
            if (areaW < 2f || areaH < 2f) continue
            clipRect(areaX, areaY, areaX + areaW, areaY + areaH) {
                drawSeaWaves(areaX, areaY, areaW, areaH, seaPhase)
            }
        }

        // --- River flow animations ---
        for (river in rivers) {
            if (river.points.size < 2) continue
            val pixelPoints = river.points.map { pt ->
                androidx.compose.ui.geometry.Offset(
                    imageOffsetX + pt.x / 1000f * imageWidth,
                    imageOffsetY + pt.y / 1000f * imageHeight
                )
            }
            drawRiverFlow(pixelPoints, riverPhase)
        }
    }
}

/**
 * Draws animated sea waves as sine-wave lines inside the given rectangle.
 *
 * @param areaX left edge of the sea area in canvas pixels
 * @param areaY top edge of the sea area in canvas pixels
 * @param areaW width of the sea area in canvas pixels
 * @param areaH height of the sea area in canvas pixels
 * @param phase current animation phase in radians [0, 2π)
 */
private fun DrawScope.drawSeaWaves(
    areaX: Float,
    areaY: Float,
    areaW: Float,
    areaH: Float,
    phase: Float
) {
    val waveColor = Color(0x3300AAFF)  // semi-transparent blue
    val amplitude = (areaH * 0.04f).coerceAtLeast(2f)
    val numWaves = 5
    val freqX = 2f * PI.toFloat() / (areaW / 3f)

    for (waveIdx in 0 until numWaves) {
        val waveY = areaY + areaH * (waveIdx + 1f) / (numWaves + 1f)
        val phaseOffset = waveIdx * PI.toFloat() / 2.5f

        val path = Path()
        var firstPoint = true
        var x = areaX
        while (x <= areaX + areaW) {
            val y = waveY + amplitude * sin(x * freqX + phase + phaseOffset)
            if (firstPoint) {
                path.moveTo(x, y)
                firstPoint = false
            } else {
                path.lineTo(x, y)
            }
            x += 3f
        }
        drawPath(path, waveColor, style = Stroke(width = 1.5f, cap = StrokeCap.Round))
    }
}

/**
 * Draws animated flowing dashes along a river path.
 * Dashes move from the first point to the last point (towards the sea).
 *
 * @param points list of pixel-space points defining the river path
 * @param phase  current animation phase in [0, 1)
 */
private fun DrawScope.drawRiverFlow(
    points: List<androidx.compose.ui.geometry.Offset>,
    phase: Float
) {
    if (points.size < 2) return

    // Build cumulative distances along the path
    val segmentLengths = FloatArray(points.size - 1)
    var totalLength = 0f
    for (i in 0 until points.size - 1) {
        val dx = points[i + 1].x - points[i].x
        val dy = points[i + 1].y - points[i].y
        val len = sqrt(dx * dx + dy * dy)
        segmentLengths[i] = len
        totalLength += len
    }
    if (totalLength < 1f) return

    val dashColor = Color(0x6600BBEE)   // semi-transparent river blue
    val dashLength = (totalLength * 0.06f).coerceIn(5f, 20f)
    val dashSpacing = dashLength * 2.5f
    val numDashes = ((totalLength / dashSpacing) + 1).toInt().coerceAtMost(30)

    for (dashIdx in 0 until numDashes) {
        // Distribute dashes evenly and advance with phase
        val dashStart = ((dashIdx.toFloat() / numDashes + phase) % 1.0f) * totalLength
        val dashEnd = dashStart + dashLength

        val startOffset = getPointAtDistance(points, segmentLengths, totalLength, dashStart) ?: continue
        val endOffset = getPointAtDistance(points, segmentLengths, totalLength, dashEnd.coerceAtMost(totalLength)) ?: continue

        drawLine(
            color = dashColor,
            start = startOffset,
            end = endOffset,
            strokeWidth = 3.5f,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Returns the interpolated position along a polyline at the given distance from its start.
 *
 * @param points         the polyline vertices
 * @param segmentLengths pre-computed lengths of each segment
 * @param totalLength    total path length
 * @param distance       distance from the start (clamped to [0, totalLength])
 */
private fun getPointAtDistance(
    points: List<androidx.compose.ui.geometry.Offset>,
    segmentLengths: FloatArray,
    totalLength: Float,
    distance: Float
): androidx.compose.ui.geometry.Offset? {
    if (distance <= 0f) return points.firstOrNull()
    if (distance >= totalLength) return points.lastOrNull()

    var remaining = distance
    for (i in segmentLengths.indices) {
        val segLen = segmentLengths[i]
        if (remaining <= segLen) {
            val t = remaining / segLen.coerceAtLeast(0.001f)
            val p0 = points[i]
            val p1 = points[i + 1]
            return androidx.compose.ui.geometry.Offset(
                x = p0.x + t * (p1.x - p0.x),
                y = p0.y + t * (p1.y - p0.y)
            )
        }
        remaining -= segLen
    }
    return points.lastOrNull()
}
