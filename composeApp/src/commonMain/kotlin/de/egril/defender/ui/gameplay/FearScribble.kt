package de.egril.defender.ui.gameplay

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Static black scribble/cloud overlay for feared enemies when animations are disabled.
 * Drawn at the upper third of the cell to represent a dark fear cloud around the head.
 * When animations are enabled, AnimatedFearCloud is used instead.
 */
@Composable
fun FearScribble(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        // Draw in the upper ~30% of the cell
        drawFearCloud(
            topLeft = Offset(w * 0.08f, h * 0.03f),
            width = w * 0.84f,
            height = h * 0.28f,
            color = Color.Black.copy(alpha = 0.75f),
            animated = false,
            animOffset = 0f
        )
    }
}

/**
 * Animated black scribble/cloud overlay for feared enemies when animations are enabled.
 * The lines of the cloud move to convey a dark swirling fear cloud.
 */
@Composable
fun AnimatedFearCloud(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "fearCloud")
    val animOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "fearCloudOffset"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawFearCloud(
            topLeft = Offset(w * 0.08f, h * 0.03f),
            width = w * 0.84f,
            height = h * 0.28f,
            color = Color.Black.copy(alpha = 0.75f),
            animated = true,
            animOffset = animOffset
        )
    }
}

/**
 * Draw a scribbled dark cloud shape using irregular jagged lines.
 * When animated, the lines shift position to simulate movement.
 */
private fun DrawScope.drawFearCloud(
    topLeft: Offset,
    width: Float,
    height: Float,
    color: Color,
    animated: Boolean,
    animOffset: Float
) {
    val strokeWidth = width * 0.06f
    val centerX = topLeft.x + width / 2f
    val centerY = topLeft.y + height / 2f
    val rx = width / 2f * 0.9f
    val ry = height / 2f * 0.85f

    // Draw 3 overlapping jagged arcs to create a scribble-cloud effect
    val arcCount = 3
    for (arcIdx in 0 until arcCount) {
        val phaseShift = arcIdx * (2f * PI.toFloat() / arcCount)
        val segments = 14
        val points = (0..segments).map { i ->
            val t = i.toFloat() / segments
            val angle = t * 2f * PI.toFloat() + phaseShift
            // Add jaggedness: small amplitude oscillation perpendicular to the ellipse
            val jag = if (animated) {
                val wave = 0.12f * sin(angle * 3f + animOffset + phaseShift)
                wave
            } else {
                // Static jaggedness based on position
                0.07f * sin(angle * 5f + phaseShift * 2f)
            }
            val rxJ = rx * (1f + jag)
            val ryJ = ry * (1f - jag * 0.5f)
            Offset(
                centerX + rxJ * cos(angle),
                centerY + ryJ * sin(angle)
            )
        }
        for (i in 0 until points.size - 1) {
            drawLine(
                color = color,
                start = points[i],
                end = points[i + 1],
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
    }
}
