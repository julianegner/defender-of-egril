package de.egril.defender.ui.animations

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import de.egril.defender.ui.gameplay.GamePlayConstants

@Composable
fun RocketAttackAnimation(
    animate: Boolean,
    modifier: Modifier = Modifier,
    directionAngle: Float = 0f,
) {
    if (!animate) return

    val progress = remember { Animatable(0f) }
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(directionAngle) {
        isVisible = true
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec =
                tween(
                    durationMillis = GamePlayConstants.AnimationTimings.ARROW_FLIGHT_DELAY_MS.toInt(),
                    easing = LinearEasing,
                ),
        )
        isVisible = false
    }

    if (!isVisible) return

    Canvas(modifier = modifier.graphicsLayer { rotationZ = directionAngle }) {
        val px = size.width * progress.value
        val py = size.height * 0.5f
        val bodyLength = size.minDimension * 0.34f
        val bodyHeight = size.minDimension * 0.12f
        val noseLength = bodyLength * 0.26f
        val tailX = px - bodyLength * 0.58f
        val bodyStartX = tailX + noseLength
        val bodyEndX = px + bodyLength * 0.42f

        drawRoundRect(
            color = Color(0xFFCFC9B8),
            topLeft = Offset(bodyStartX, py - bodyHeight / 2f),
            size = androidx.compose.ui.geometry.Size(bodyEndX - bodyStartX, bodyHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(bodyHeight * 0.35f, bodyHeight * 0.35f),
        )
        drawRoundRect(
            color = Color(0xFF6A6965),
            topLeft = Offset(bodyStartX, py - bodyHeight / 2f),
            size = androidx.compose.ui.geometry.Size(bodyEndX - bodyStartX, bodyHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(bodyHeight * 0.35f, bodyHeight * 0.35f),
            style = Stroke(width = bodyHeight * 0.12f),
        )

        val nosePath =
            Path().apply {
                moveTo(bodyEndX, py - bodyHeight / 2f)
                lineTo(bodyEndX + noseLength, py)
                lineTo(bodyEndX, py + bodyHeight / 2f)
                close()
            }
        drawPath(path = nosePath, color = Color(0xFFD04535))
        drawPath(path = nosePath, color = Color(0xFF6D221D), style = Stroke(width = bodyHeight * 0.12f))

        val finWidth = bodyLength * 0.24f
        val finHeight = bodyHeight * 0.9f
        val upperFin =
            Path().apply {
                moveTo(tailX + finWidth * 0.25f, py - bodyHeight / 2f)
                lineTo(tailX - finWidth * 0.35f, py - bodyHeight / 2f - finHeight * 0.5f)
                lineTo(tailX + finWidth * 0.85f, py - bodyHeight * 0.14f)
                close()
            }
        val lowerFin =
            Path().apply {
                moveTo(tailX + finWidth * 0.25f, py + bodyHeight / 2f)
                lineTo(tailX - finWidth * 0.35f, py + bodyHeight / 2f + finHeight * 0.5f)
                lineTo(tailX + finWidth * 0.85f, py + bodyHeight * 0.14f)
                close()
            }
        drawPath(upperFin, color = Color(0xFFB33A2D))
        drawPath(lowerFin, color = Color(0xFFB33A2D))

        val flameLength = bodyLength * 0.35f
        val flameWidth = bodyHeight * 0.9f
        val flamePath =
            Path().apply {
                moveTo(tailX - flameLength, py)
                lineTo(tailX - flameLength * 0.2f, py - flameWidth / 2f)
                lineTo(tailX, py)
                lineTo(tailX - flameLength * 0.2f, py + flameWidth / 2f)
                close()
            }
        drawPath(flamePath, color = Color(0xFFFF7F32))
        drawPath(
            path =
                Path().apply {
                    moveTo(tailX - flameLength * 0.75f, py)
                    lineTo(tailX - flameLength * 0.22f, py - flameWidth * 0.24f)
                    lineTo(tailX - flameLength * 0.04f, py)
                    lineTo(tailX - flameLength * 0.22f, py + flameWidth * 0.24f)
                    close()
                },
            color = Color(0xFFFFD54F),
        )
    }
}
