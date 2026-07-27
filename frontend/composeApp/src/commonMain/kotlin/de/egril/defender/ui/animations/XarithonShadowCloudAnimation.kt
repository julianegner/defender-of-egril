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
 * Black magic cloud overlay for towers disabled by Xarithon's Shadow Spew.
 * When [animate] is true and animations are globally enabled, the dark tendrils rotate slowly.
 * Otherwise a static shadow pattern is drawn.
 */
@Composable
fun XarithonShadowCloudAnimation(
    animate: Boolean,
    modifier: Modifier = Modifier,
) {
    val useAnimation = animate && AppSettings.enableAnimations.value

    val infiniteTransition = rememberInfiniteTransition(label = "Shadow Cloud")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
        ),
        label = "Shadow Cloud Rotation",
    )

    val currentAngle = if (useAnimation) rotationAngle else 0f

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val baseRadius = size.minDimension / 2 * 0.68f
            val shadowColor = Color(0xFF2A0055)
            val voidColor = Color(0xFF0A0015)
            val glowColor = Color(0xFF8800FF)

            // Draw dark shadow tendrils radiating from the center
            val tendrilCount = 6
            for (i in 0 until tendrilCount) {
                val angle = (i * 360f / tendrilCount + currentAngle) * PI.toFloat() / 180f
                val tendrilX = centerX + baseRadius * sin(angle)
                val tendrilY = centerY - baseRadius * cos(angle)

                drawLine(
                    color = shadowColor,
                    start = Offset(centerX, centerY),
                    end = Offset(tendrilX, tendrilY),
                    strokeWidth = 3.dp.toPx(),
                )

                // Counter-rotating secondary tendrils for eerie effect
                val angle2 = (i * 360f / tendrilCount - currentAngle * 0.6f) * PI.toFloat() / 180f
                val tend2X = centerX + baseRadius * 0.75f * sin(angle2)
                val tend2Y = centerY - baseRadius * 0.75f * cos(angle2)
                drawLine(
                    color = glowColor.copy(alpha = 0.35f),
                    start = Offset(centerX, centerY),
                    end = Offset(tend2X, tend2Y),
                    strokeWidth = 1.5.dp.toPx(),
                )

                // Draw void orbs along each tendril
                val orbCount = 2
                for (j in 1..orbCount) {
                    val orbPos = j / (orbCount + 1f)
                    val orbX = centerX + baseRadius * sin(angle) * orbPos
                    val orbY = centerY - baseRadius * cos(angle) * orbPos
                    val orbRadius = 2.5.dp.toPx() * (1f - orbPos * 0.4f)
                    drawCircle(
                        color = glowColor.copy(alpha = 0.50f),
                        radius = orbRadius,
                        center = Offset(orbX, orbY),
                    )
                }
            }

            // Draw outer shadow ring
            drawCircle(
                color = shadowColor.copy(alpha = 0.45f),
                radius = baseRadius,
                center = Offset(centerX, centerY),
                style = Stroke(width = 3.5.dp.toPx()),
            )

            // Secondary pulsing ring (half radius)
            drawCircle(
                color = glowColor.copy(alpha = 0.25f),
                radius = baseRadius * 0.55f,
                center = Offset(centerX, centerY),
                style = Stroke(width = 2.dp.toPx()),
            )

            // Draw void cloud at center
            drawCircle(
                color = voidColor.copy(alpha = 0.55f),
                radius = baseRadius * 0.38f,
                center = Offset(centerX, centerY),
            )
            drawCircle(
                color = shadowColor,
                radius = baseRadius * 0.38f,
                center = Offset(centerX, centerY),
                style = Stroke(width = 2.dp.toPx()),
            )
            // Inner glow core
            drawCircle(
                color = glowColor.copy(alpha = 0.30f),
                radius = baseRadius * 0.18f,
                center = Offset(centerX, centerY),
            )
        }
    }
}
