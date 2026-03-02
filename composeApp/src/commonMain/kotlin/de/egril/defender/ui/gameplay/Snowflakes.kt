package de.egril.defender.ui.gameplay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Static snowflakes overlay for frozen enemies when animations are disabled
 * (AppSettings.enableAnimations = false). Shows four white snowflakes at
 * different heights and with different sizes, spread horizontally.
 * When animations are enabled, LottieAnimation with AnimationType.FREEZE_SPELL is used instead.
 */
@Composable
fun Snowflakes(
    modifier: Modifier = Modifier
) {
    // Four static snowflakes: (xFrac, yFrac, radiusFrac) – different sizes and heights
    val snowflakeConfigs = remember {
        listOf(
            Triple(0.20f, 0.10f, 0.09f),   // left, high, large
            Triple(0.42f, 0.38f, 0.04f),   // center-left, upper-mid, small
            Triple(0.65f, 0.58f, 0.065f),  // center-right, mid, medium
            Triple(0.83f, 0.82f, 0.033f)   // right, lower, tiny
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

/**
 * Draw a simple snowflake at the given position
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSnowflake(
    center: Offset,
    radius: Float,
    color: Color
) {
    val strokeWidth = radius * 0.15f

    // Draw 6 arms of the snowflake
    for (i in 0 until 6) {
        val angle = i * PI.toFloat() / 3f
        val cosAngle = cos(angle)
        val sinAngle = sin(angle)

        // Main arm
        drawLine(
            color = color,
            start = center,
            end = Offset(
                center.x + cosAngle * radius,
                center.y + sinAngle * radius
            ),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        // Small branches
        val branchLength = radius * 0.3f
        val branchPos = radius * 0.65f
        val branchX = center.x + cosAngle * branchPos
        val branchY = center.y + sinAngle * branchPos

        // Left branch
        val leftAngle = angle - PI.toFloat() / 6f
        drawLine(
            color = color,
            start = Offset(branchX, branchY),
            end = Offset(
                branchX + cos(leftAngle) * branchLength,
                branchY + sin(leftAngle) * branchLength
            ),
            strokeWidth = strokeWidth * 0.6f,
            cap = StrokeCap.Round
        )

        // Right branch
        val rightAngle = angle + PI.toFloat() / 6f
        drawLine(
            color = color,
            start = Offset(branchX, branchY),
            end = Offset(
                branchX + cos(rightAngle) * branchLength,
                branchY + sin(rightAngle) * branchLength
            ),
            strokeWidth = strokeWidth * 0.6f,
            cap = StrokeCap.Round
        )
    }
}
