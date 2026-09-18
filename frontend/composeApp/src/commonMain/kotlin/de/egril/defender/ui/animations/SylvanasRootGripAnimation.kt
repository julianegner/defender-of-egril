package de.egril.defender.ui.animations

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import de.egril.defender.ui.settings.AppSettings
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Thorny vine overlay for towers disabled by Sylvanas's Root Grip.
 * When [animate] is true and animations are globally enabled, the vines rotate continuously.
 * Otherwise a static thorn pattern is drawn.
 */
@Composable
fun SylvanasRootGripAnimation(
    animate: Boolean,
    modifier: Modifier = Modifier,
) {
    val useAnimation = animate && AppSettings.enableAnimations.value

    val infiniteTransition = rememberInfiniteTransition(label = "Root Grip")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(4000, easing = LinearEasing),
            ),
        label = "Root Grip Rotation",
    )

    val currentAngle = if (useAnimation) rotationAngle else 0f

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val baseRadius = size.minDimension / 2 * 0.65f
            val vineColor = Color(0xFF4CAF50)
            val thornColor = Color(0xFF1B5E20)

            // Draw thorny vines radiating from the center
            val vineCount = 8
            for (i in 0 until vineCount) {
                val angle = (i * 360f / vineCount + currentAngle) * PI.toFloat() / 180f
                val vineX = centerX + baseRadius * sin(angle)
                val vineY = centerY - baseRadius * cos(angle)

                drawLine(
                    color = vineColor,
                    start = Offset(centerX, centerY),
                    end = Offset(vineX, vineY),
                    strokeWidth = 2.5.dp.toPx(),
                )

                // Draw diamond-shaped thorns along each vine
                val thornCount = 3
                for (j in 1..thornCount) {
                    val thornPos = j / (thornCount + 1f)
                    val thornX = centerX + baseRadius * sin(angle) * thornPos
                    val thornY = centerY - baseRadius * cos(angle) * thornPos

                    val thornSize = 3.dp.toPx()
                    drawLine(
                        color = thornColor,
                        start = Offset(thornX - thornSize, thornY),
                        end = Offset(thornX + thornSize, thornY),
                        strokeWidth = 1.5.dp.toPx(),
                    )
                    drawLine(
                        color = thornColor,
                        start = Offset(thornX, thornY - thornSize),
                        end = Offset(thornX, thornY + thornSize),
                        strokeWidth = 1.5.dp.toPx(),
                    )
                }
            }

            // Draw outer ring of thorns
            drawCircle(
                color = vineColor.copy(alpha = 0.35f),
                radius = baseRadius,
                center = Offset(centerX, centerY),
                style = Stroke(width = 3.dp.toPx()),
            )

            // Draw engulfing circle at center
            drawCircle(
                color = vineColor.copy(alpha = 0.25f),
                radius = baseRadius * 0.35f,
                center = Offset(centerX, centerY),
            )
            drawCircle(
                color = thornColor,
                radius = baseRadius * 0.35f,
                center = Offset(centerX, centerY),
                style = Stroke(width = 2.dp.toPx()),
            )
        }
    }
}
