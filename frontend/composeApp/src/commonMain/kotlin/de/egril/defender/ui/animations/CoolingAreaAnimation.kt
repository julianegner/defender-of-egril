package de.egril.defender.ui.animations

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import de.egril.defender.ui.icon.SnowflakeIcon
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Cooling area overlay for tiles affected by the Cooling spell.
 * When [animate] is true, shows a canvas snowflake pattern (rich visual).
 * When [animate] is false, shows a single static snowflake icon.
 */
@Composable
fun CoolingAreaAnimation(animate: Boolean, modifier: Modifier = Modifier) {
    if (animate) {
        AnimatedCoolingArea(modifier)
    } else {
        StaticCoolingArea(modifier)
    }
}

@Composable
private fun AnimatedCoolingArea(modifier: Modifier = Modifier) {
    val snowflakeConfigs = remember {
        listOf(
            Triple(0.08f, 0.62f, 0.04f),
            Triple(0.15f, 0.04f, 0.09f),
            Triple(0.25f, 0.42f, 0.07f),
            Triple(0.32f, 0.83f, 0.04f),
            Triple(0.42f, 0.73f, 0.035f),
            Triple(0.50f, 0.20f, 0.065f),
            Triple(0.60f, 0.51f, 0.06f),
            Triple(0.67f, 0.88f, 0.033f),
            Triple(0.75f, 0.18f, 0.08f),
            Triple(0.82f, 0.54f, 0.072f),
            Triple(0.88f, 0.85f, 0.045f),
            Triple(0.93f, 0.04f, 0.05f)
        )
    }
    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        snowflakeConfigs.forEach { (xFrac, yFrac, radiusFrac) ->
            drawSnowflake(
                center = Offset(xFrac * canvasWidth, yFrac * canvasHeight),
                radius = canvasWidth * radiusFrac,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
private fun StaticCoolingArea(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        SnowflakeIcon(
            size = 24.dp,
            tint = Color.Cyan.copy(alpha = 0.7f)
        )
    }
}

private fun DrawScope.drawSnowflake(center: Offset, radius: Float, color: Color) {
    val strokeWidth = radius * 0.15f
    for (i in 0 until 6) {
        val angle = i * PI.toFloat() / 3f
        val cosAngle = cos(angle)
        val sinAngle = sin(angle)
        drawLine(
            color = color,
            start = center,
            end = Offset(center.x + cosAngle * radius, center.y + sinAngle * radius),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        val branchLength = radius * 0.3f
        val branchPos = radius * 0.65f
        val branchX = center.x + cosAngle * branchPos
        val branchY = center.y + sinAngle * branchPos
        val leftAngle = angle - PI.toFloat() / 6f
        drawLine(
            color = color,
            start = Offset(branchX, branchY),
            end = Offset(branchX + cos(leftAngle) * branchLength, branchY + sin(leftAngle) * branchLength),
            strokeWidth = strokeWidth * 0.6f,
            cap = StrokeCap.Round
        )
        val rightAngle = angle + PI.toFloat() / 6f
        drawLine(
            color = color,
            start = Offset(branchX, branchY),
            end = Offset(branchX + cos(rightAngle) * branchLength, branchY + sin(rightAngle) * branchLength),
            strokeWidth = strokeWidth * 0.6f,
            cap = StrokeCap.Round
        )
    }
}
