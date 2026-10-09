package de.egril.defender.ui.animations

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import de.egril.defender.model.Position
import de.egril.defender.model.SnotlingCannonThrowEffect
import de.egril.defender.ui.gameplay.GamePlayConstants
import de.egril.defender.ui.hexagon.HexagonalGridConstants
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun SnotlingCannonThrowOverlay(
    effects: List<SnotlingCannonThrowEffect>,
    hexSizeDp: Float,
    contentSize: IntSize,
    animate: Boolean,
) {
    val pixelDensity = LocalDensity.current.density
    val hexSizePx = hexSizeDp * pixelDensity
    val colSpacingPx = HexagonalGridConstants.HORIZONTAL_SPACING * pixelDensity
    val rowVerticalAdjPx = HexagonalGridConstants.VERTICAL_SPACING_ADJUSTMENT * pixelDensity
    val contentWidth: Dp = (contentSize.width / pixelDensity).dp
    val contentHeight: Dp = (contentSize.height / pixelDensity).dp

    effects.forEach { effect ->
        key(effect.turnNumber, effect.sourcePosition.x, effect.sourcePosition.y) {
            SingleSnotlingCannonThrowOverlay(
                effect = effect,
                hexSizePx = hexSizePx,
                colSpacingPx = colSpacingPx,
                rowVerticalAdjPx = rowVerticalAdjPx,
                contentWidth = contentWidth,
                contentHeight = contentHeight,
                animate = animate,
            )
        }
    }
}

@Composable
private fun SingleSnotlingCannonThrowOverlay(
    effect: SnotlingCannonThrowEffect,
    hexSizePx: Float,
    colSpacingPx: Float,
    rowVerticalAdjPx: Float,
    contentWidth: Dp,
    contentHeight: Dp,
    animate: Boolean,
) {
    val hexWidthPx = hexSizePx * sqrt(3f)
    val hexHeightPx = hexSizePx * 2f
    val rowSpacingPx = hexHeightPx * 0.75f - hexHeightPx + rowVerticalAdjPx
    val oddOffsetPx = hexWidthPx * HexagonalGridConstants.ODD_ROW_OFFSET_RATIO

    fun tileCenterPx(pos: Position): Offset {
        val oddRowOffset = if (pos.y % 2 == 1) oddOffsetPx else 0f
        val x = pos.x * (hexWidthPx + colSpacingPx) + hexWidthPx / 2f + oddRowOffset
        val y = pos.y * (hexHeightPx + rowSpacingPx) + hexHeightPx / 2f
        return Offset(x, y)
    }

    val sourceCenter = tileCenterPx(effect.sourcePosition)
    val targetCenter = tileCenterPx(effect.targetPosition)
    val progress = remember { Animatable(0f) }

    LaunchedEffect(effect.turnNumber, effect.sourcePosition, effect.targetPosition, animate) {
        progress.snapTo(0f)
        if (animate) {
            progress.animateTo(
                targetValue = 1f,
                animationSpec =
                    tween(
                        durationMillis = GamePlayConstants.AnimationTimings.ARROW_FLIGHT_DELAY_MS.toInt(),
                        easing = LinearEasing,
                    ),
            )
        } else {
            progress.snapTo(1f)
        }
    }

    Canvas(modifier = Modifier.requiredSize(contentWidth, contentHeight)) {
        drawSnotlingCannonThrow(
            progress = progress.value,
            sourceCenter = sourceCenter,
            targetCenter = targetCenter,
            hexSizePx = hexSizePx,
            thrownCount = effect.thrownCount,
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSnotlingCannonThrow(
    progress: Float,
    sourceCenter: Offset,
    targetCenter: Offset,
    hexSizePx: Float,
    thrownCount: Int,
) {
    if (progress <= 0f || progress >= 1f) return

    val dx = targetCenter.x - sourceCenter.x
    val dy = targetCenter.y - sourceCenter.y
    val swirlCount = (thrownCount / 8).coerceIn(3, 7)
    val launchGlowAlpha = (1f - kotlin.math.abs(progress - 0.5f) * 2f).coerceIn(0f, 1f)
    drawCircle(
        color = Color(0xFF7CB342).copy(alpha = launchGlowAlpha * 0.3f),
        radius = hexSizePx * (0.25f + progress * 0.2f),
        center = sourceCenter,
    )

    for (index in 0 until swirlCount) {
        val stagger = index.toFloat() / swirlCount.toFloat()
        val localProgress = (progress - stagger * 0.18f).coerceIn(0f, 1f)
        if (localProgress <= 0f || localProgress >= 1f) continue

        val arcHeight = hexSizePx * 1.15f
        val x = sourceCenter.x + dx * localProgress
        val y = sourceCenter.y + dy * localProgress - (4f * localProgress * (1f - localProgress) * arcHeight)
        val center = Offset(x, y)

        val spin = (localProgress * 3f + stagger) * (2.0 * PI)
        val radius = hexSizePx * 0.12f
        drawCircle(
            color = Color(0xFF9CCC65).copy(alpha = 0.92f),
            radius = radius,
            center = center,
        )

        val armLength = radius * 0.9f
        val legLength = radius * 0.75f
        val armDx = cos(spin).toFloat() * armLength
        val armDy = sin(spin).toFloat() * armLength
        val legDx = cos(spin + PI / 2).toFloat() * legLength
        val legDy = sin(spin + PI / 2).toFloat() * legLength

        drawLine(
            color = Color(0xFF3E2723),
            start = Offset(center.x - armDx, center.y - armDy),
            end = Offset(center.x + armDx, center.y + armDy),
            strokeWidth = radius * 0.28f,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = Color(0xFF3E2723),
            start = Offset(center.x - legDx, center.y + legDy * 0.2f),
            end = Offset(center.x + legDx, center.y - legDy * 0.2f),
            strokeWidth = radius * 0.24f,
            cap = StrokeCap.Round,
        )
    }
}
