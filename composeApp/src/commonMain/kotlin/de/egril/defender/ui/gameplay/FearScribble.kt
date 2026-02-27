package de.egril.defender.ui.gameplay

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
 * When animations are enabled, LottieAnimation with AnimationType.FEAR_SPELL is used instead.
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
            color = Color.Black.copy(alpha = 0.75f)
        )
    }
}

/**
 * Draw a scribbled dark cloud shape using irregular jagged lines.
 */
private fun DrawScope.drawFearCloud(
    topLeft: Offset,
    width: Float,
    height: Float,
    color: Color
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
            val jag = 0.07f * sin(angle * 5f + phaseShift * 2f)
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
