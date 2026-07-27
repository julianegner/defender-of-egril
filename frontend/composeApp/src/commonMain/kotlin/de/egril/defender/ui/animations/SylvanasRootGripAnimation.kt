package de.egril.defender.ui.animations

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
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
import io.github.alexzhirkevich.compottie.Compottie
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Thorny vine animation overlay for towers disabled by Sylvanas's Root Grip.
 * When [animate] is true, shows a Lottie animation of vines growing and engulfing the tower.
 * When [animate] is false, shows a static thorny vine overlay.
 */
@Composable
fun SylvanasRootGripAnimation(
    animate: Boolean,
    modifier: Modifier = Modifier,
) {
    if (animate && AppSettings.enableAnimations.value) {
        AnimatedSylvanasRootGrip(modifier)
    } else {
        StaticSylvanasRootGrip(modifier)
    }
}

@Composable
private fun AnimatedSylvanasRootGrip(modifier: Modifier = Modifier) {
    LottieAnimation(
        animationType = AnimationType.SYLVANAS_ROOT_GRIP,
        modifier = modifier.fillMaxSize(),
        iterations = Compottie.IterateForever,
    )
}

@Composable
private fun StaticSylvanasRootGrip(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition()
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
        ),
        label = "Root Grip Rotation",
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val baseRadius = size.minDimension / 2 * 0.6f
            val vineColor = Color(0xFF4CAF50)
            val thornColor = Color(0xFF2E7D32)

            // Draw thorny vines in a circular pattern
            val vineCount = 8
            for (i in 0 until vineCount) {
                val angle = (i * 360f / vineCount + rotationAngle) * PI.toFloat() / 180f
                val vineX = centerX + baseRadius * sin(angle)
                val vineY = centerY - baseRadius * cos(angle)

                // Draw vine line from center outward
                drawLine(
                    color = vineColor,
                    start = Offset(centerX, centerY),
                    end = Offset(vineX, vineY),
                    strokeWidth = 2.dp.toPx(),
                )

                // Draw thorns along the vine
                val thornCount = 3
                for (j in 1..thornCount) {
                    val thornPos = j / (thornCount + 1f)
                    val thornX = centerX + baseRadius * sin(angle) * thornPos
                    val thornY = centerY - baseRadius * cos(angle) * thornPos

                    // Draw thorn as a small cross
                    val thornSize = 3.dp.toPx()
                    drawLine(
                        color = thornColor,
                        start = Offset(thornX - thornSize, thornY),
                        end = Offset(thornX + thornSize, thornY),
                        strokeWidth = 1.dp.toPx(),
                    )
                    drawLine(
                        color = thornColor,
                        start = Offset(thornX, thornY - thornSize),
                        end = Offset(thornX, thornY + thornSize),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
            }

            // Draw center engulfing circle with thorns
            drawCircle(
                color = vineColor.copy(alpha = 0.3f),
                radius = baseRadius * 0.4f,
                center = Offset(centerX, centerY),
            )

            // Draw circle outline
            drawCircle(
                color = thornColor,
                radius = baseRadius * 0.4f,
                center = Offset(centerX, centerY),
                style = Stroke(width = 2.dp.toPx()),
            )
        }
    }
}
