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
 * (AppSettings.enableAnimations = false). Shows three white snowflakes at
 * different fixed heights. When animations are enabled, LottieAnimation with
 * AnimationType.FREEZE_SPELL is used instead.
 */
@Composable
fun SnowflakeAnimation(
    modifier: Modifier = Modifier
) {
    // Three static snowflakes at different fixed heights (top, middle, lower)
    val snowflakePositions = remember {
        listOf(
            Pair(0.22f, 0.18f),  // left, high
            Pair(0.50f, 0.50f),  // center, mid
            Pair(0.78f, 0.80f)   // right, low
        )
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        snowflakePositions.forEach { (xFrac, yFrac) ->
            drawSnowflake(
                center = Offset(xFrac * canvasWidth, yFrac * canvasHeight),
                radius = canvasWidth * 0.06f,
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
