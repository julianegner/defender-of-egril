package de.egril.defender.ui.animations

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Fear spell overlay for feared enemies.
 * When [animate] is true, shows a Lottie dark cloud animation.
 * When [animate] is false, shows a static black scribble cloud in the upper portion of the tile.
 */
@Composable
fun FearSpellAnimation(animate: Boolean, modifier: Modifier = Modifier) {
    if (animate) {
        AnimatedFearSpell(modifier)
    } else {
        StaticFearSpell(modifier)
    }
}

@Composable
private fun AnimatedFearSpell(modifier: Modifier = Modifier) {
    LottieAnimation(
        animationType = AnimationType.FEAR_SPELL,
        modifier = modifier.fillMaxSize(),
        iterations = Int.MAX_VALUE
    )
}

@Composable
private fun StaticFearSpell(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawFearCloud(
            topLeft = Offset(w * 0.08f, h * 0.03f),
            width = w * 0.84f,
            height = h * 0.28f,
            color = Color.Black.copy(alpha = 0.75f)
        )
    }
}

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
