package de.egril.defender.ui.animations

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import de.egril.defender.model.Position
import de.egril.defender.model.RocketAttackEffect
import de.egril.defender.ui.gameplay.GamePlayConstants
import de.egril.defender.ui.hexagon.HexagonalGridConstants
import kotlin.math.sqrt

private const val FIRING_PHASE_END = 0.15f

@Composable
fun RocketAttackOverlay(
    effects: List<RocketAttackEffect>,
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
            SingleRocketOverlay(
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
private fun SingleRocketOverlay(
    effect: RocketAttackEffect,
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
    val overallProgress = remember { Animatable(0f) }

    LaunchedEffect(effect.turnNumber, effect.sourcePosition, effect.targetPosition, animate) {
        overallProgress.snapTo(0f)
        if (animate) {
            overallProgress.animateTo(
                targetValue = 1f,
                animationSpec =
                    tween(
                        durationMillis = GamePlayConstants.AnimationTimings.ARROW_FLIGHT_DELAY_MS.toInt(),
                        easing = LinearEasing,
                    ),
            )
        } else {
            overallProgress.snapTo(1f)
        }
    }

    val progress by remember { androidx.compose.runtime.derivedStateOf { overallProgress.value } }

    Canvas(modifier = Modifier.requiredSize(contentWidth, contentHeight)) {
        drawRocket(progress = progress, sourceCenter = sourceCenter, targetCenter = targetCenter, hexSizePx = hexSizePx)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRocket(
    progress: Float,
    sourceCenter: Offset,
    targetCenter: Offset,
    hexSizePx: Float,
) {
    if (progress >= 1f) return

    val dx = targetCenter.x - sourceCenter.x
    val dy = targetCenter.y - sourceCenter.y
    val dist = sqrt(dx * dx + dy * dy)
    if (dist < 0.01f) return
    val nx = dx / dist
    val ny = dy / dist
    val px = -ny
    val py = nx

    if (progress < FIRING_PHASE_END) {
        val fp = progress / FIRING_PHASE_END
        val alpha = if (fp < 0.5f) fp * 2f else (1f - fp) * 2f
        drawCircle(
            color = Color(0xFFFFB74D).copy(alpha = alpha * 0.45f),
            radius = hexSizePx * (0.5f + fp * 0.35f),
            center = sourceCenter,
        )
    }

    val flyProgress = ((progress - FIRING_PHASE_END) / (1f - FIRING_PHASE_END)).coerceIn(0f, 1f)
    if (flyProgress <= 0f || flyProgress >= 1f) return

    val tipX = sourceCenter.x + dx * flyProgress
    val tipY = sourceCenter.y + dy * flyProgress
    val tip = Offset(tipX, tipY)

    val rocketLength = hexSizePx * 1.25f
    val bodyLength = rocketLength * 0.64f
    val noseLength = rocketLength * 0.28f
    val bodyWidth = hexSizePx * 0.32f
    val finLength = rocketLength * 0.22f
    val finWidth = bodyWidth * 0.65f
    val tail = Offset(tip.x - nx * rocketLength, tip.y - ny * rocketLength)
    val bodyFront = Offset(tip.x - nx * noseLength, tip.y - ny * noseLength)
    val bodyBack = Offset(bodyFront.x - nx * bodyLength, bodyFront.y - ny * bodyLength)

    val flameLength = rocketLength * 0.55f
    val flameBack = Offset(tail.x - nx * flameLength, tail.y - ny * flameLength)
    val flameSpread = bodyWidth * 0.56f
    val flamePath =
        Path().apply {
            moveTo(flameBack.x, flameBack.y)
            lineTo(tail.x + px * flameSpread, tail.y + py * flameSpread)
            lineTo(tail.x - px * flameSpread, tail.y - py * flameSpread)
            close()
        }
    drawPath(flamePath, Color(0xFFFF8F00).copy(alpha = 0.92f))
    val innerFlamePath =
        Path().apply {
            moveTo((flameBack.x + tail.x) * 0.5f, (flameBack.y + tail.y) * 0.5f)
            lineTo(tail.x + px * flameSpread * 0.46f, tail.y + py * flameSpread * 0.46f)
            lineTo(tail.x - px * flameSpread * 0.46f, tail.y - py * flameSpread * 0.46f)
            close()
        }
    drawPath(innerFlamePath, Color(0xFFFFE082).copy(alpha = 0.95f))

    val nosePath =
        Path().apply {
            moveTo(tip.x, tip.y)
            lineTo(bodyFront.x + px * bodyWidth * 0.52f, bodyFront.y + py * bodyWidth * 0.52f)
            lineTo(bodyFront.x - px * bodyWidth * 0.52f, bodyFront.y - py * bodyWidth * 0.52f)
            close()
        }
    drawPath(nosePath, Color(0xFFD64A3A))

    val bodyPath =
        Path().apply {
            moveTo(bodyFront.x + px * bodyWidth * 0.5f, bodyFront.y + py * bodyWidth * 0.5f)
            lineTo(bodyBack.x + px * bodyWidth * 0.5f, bodyBack.y + py * bodyWidth * 0.5f)
            lineTo(bodyBack.x - px * bodyWidth * 0.5f, bodyBack.y - py * bodyWidth * 0.5f)
            lineTo(bodyFront.x - px * bodyWidth * 0.5f, bodyFront.y - py * bodyWidth * 0.5f)
            close()
        }
    drawPath(bodyPath, Color(0xFFCFC9B8))
    drawLine(
        color = Color(0xFF56524B),
        start = Offset(bodyFront.x - px * bodyWidth * 0.42f, bodyFront.y - py * bodyWidth * 0.42f),
        end = Offset(bodyBack.x - px * bodyWidth * 0.42f, bodyBack.y - py * bodyWidth * 0.42f),
        strokeWidth = bodyWidth * 0.12f,
        cap = StrokeCap.Round,
    )

    val upperFinPath =
        Path().apply {
            moveTo(bodyBack.x + px * bodyWidth * 0.35f, bodyBack.y + py * bodyWidth * 0.35f)
            lineTo(
                bodyBack.x - nx * finLength + px * (bodyWidth * 0.35f + finWidth),
                bodyBack.y - ny * finLength + py * (bodyWidth * 0.35f + finWidth),
            )
            lineTo(bodyBack.x + px * (bodyWidth * 0.12f), bodyBack.y + py * (bodyWidth * 0.12f))
            close()
        }
    val lowerFinPath =
        Path().apply {
            moveTo(bodyBack.x - px * bodyWidth * 0.35f, bodyBack.y - py * bodyWidth * 0.35f)
            lineTo(
                bodyBack.x - nx * finLength - px * (bodyWidth * 0.35f + finWidth),
                bodyBack.y - ny * finLength - py * (bodyWidth * 0.35f + finWidth),
            )
            lineTo(bodyBack.x - px * (bodyWidth * 0.12f), bodyBack.y - py * (bodyWidth * 0.12f))
            close()
        }
    drawPath(upperFinPath, Color(0xFFC54434))
    drawPath(lowerFinPath, Color(0xFFC54434))
}
