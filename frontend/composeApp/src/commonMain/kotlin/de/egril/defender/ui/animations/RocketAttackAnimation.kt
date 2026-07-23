package de.egril.defender.ui.animations

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import de.egril.defender.ui.gameplay.GamePlayConstants

@Composable
fun RocketAttackAnimation(
    animate: Boolean,
    modifier: Modifier = Modifier,
    directionAngle: Float = 0f,
    isTargetTile: Boolean = false,
) {
    if (!animate) return

    val progress = remember { Animatable(0f) }
    LaunchedEffect(directionAngle, isTargetTile) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec =
                tween(
                    durationMillis = GamePlayConstants.AnimationTimings.ARROW_FLIGHT_DELAY_MS.toInt(),
                    easing = LinearEasing,
                ),
        )
    }

    val innerClip: Modifier =
        if (isTargetTile) {
            Modifier.drawWithContent {
                drawContext.canvas.save()
                drawContext.canvas.clipRect(0f, 0f, size.width / 2f, size.height)
                drawContent()
                drawContext.canvas.restore()
            }
        } else {
            Modifier
        }

    Canvas(modifier = modifier.graphicsLayer { rotationZ = directionAngle }.then(innerClip)) {
        val px = size.width * progress.value
        val py = size.height * 0.5f
        val bodyW = size.minDimension * 0.22f
        val bodyH = size.minDimension * 0.09f
        drawRoundRect(
            color = Color(0xFF9DA6B0),
            topLeft = Offset(px - bodyW * 0.6f, py - bodyH / 2f),
            size = Size(bodyW, bodyH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(bodyH / 2f, bodyH / 2f),
        )
        drawCircle(color = Color(0xFFE74C3C), radius = bodyH * 0.4f, center = Offset(px + bodyW * 0.45f, py))
        drawCircle(color = Color(0xFFFFB347), radius = bodyH * 0.35f, center = Offset(px - bodyW * 0.75f, py))
    }
}

