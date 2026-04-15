package de.egril.defender.ui.animations

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import de.egril.defender.model.SpearAttackEffect
import de.egril.defender.model.Position
import de.egril.defender.ui.hexagon.HexagonalGridConstants
import de.egril.defender.ui.gameplay.GamePlayConstants
import kotlinx.coroutines.delay
import kotlin.math.sqrt

/** Fraction of the animation (0–1) spent on the throwing flash before the spear starts flying. */
private const val FIRING_PHASE_END = 0.20f

/**
 * Map-level overlay animation for spear tower attacks.
 *
 * A single large spear flies from the spear tower tile to the target tile across multiple tiles.
 * This Canvas-based overlay is rendered at the same coordinate space as the tiles (via the
 * HexagonalMapView overlayContent slot) so it can span any distance.
 *
 * Animation sequence:
 *  1. Throwing flash on the source tile (0–20% of total duration)
 *  2. Spear flies from source to target (20–100% of total duration)
 *  3. Impact at target handled by the existing TowerAttackImpactAnimation (delayed by
 *     [GamePlayConstants.AnimationTimings.ARROW_FLIGHT_DELAY_MS])
 *
 * @param effects     Active spear attack effects to render.
 * @param hexSizeDp   Hex size in dp (matches the map's hexSize value, typically 40f).
 * @param contentSize Pixel dimensions of the map content area (used to size the Canvas).
 * @param animate     When false the animation is skipped.
 */
@Composable
fun SpearAttackOverlay(
    effects: List<SpearAttackEffect>,
    hexSizeDp: Float,
    contentSize: IntSize,
    animate: Boolean
) {
    val pixelDensity = LocalDensity.current.density
    val hexSizePx = hexSizeDp * pixelDensity
    val colSpacingPx = HexagonalGridConstants.HORIZONTAL_SPACING * pixelDensity
    val rowVerticalAdjPx = HexagonalGridConstants.VERTICAL_SPACING_ADJUSTMENT * pixelDensity
    val contentWidth: Dp = (contentSize.width / pixelDensity).dp
    val contentHeight: Dp = (contentSize.height / pixelDensity).dp

    effects.forEach { effect ->
        key(effect.turnNumber, effect.sourcePosition.x, effect.sourcePosition.y) {
            SingleSpearOverlay(
                effect = effect,
                hexSizePx = hexSizePx,
                colSpacingPx = colSpacingPx,
                rowVerticalAdjPx = rowVerticalAdjPx,
                contentWidth = contentWidth,
                contentHeight = contentHeight,
                animate = animate
            )
        }
    }
}

@Composable
private fun SingleSpearOverlay(
    effect: SpearAttackEffect,
    hexSizePx: Float,
    colSpacingPx: Float,
    rowVerticalAdjPx: Float,
    contentWidth: Dp,
    contentHeight: Dp,
    animate: Boolean
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

    LaunchedEffect(effect.turnNumber, effect.sourcePosition) {
        if (animate) {
            overallProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = GamePlayConstants.AnimationTimings.ARROW_FLIGHT_DELAY_MS.toInt(),
                    easing = LinearEasing
                )
            )
        } else {
            overallProgress.snapTo(1f)
        }
    }

    val progress = overallProgress.value

    Canvas(modifier = Modifier.requiredSize(contentWidth, contentHeight)) {
        drawSpear(progress, sourceCenter, targetCenter, hexSizePx)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSpear(
    progress: Float,
    sourceCenter: Offset,
    targetCenter: Offset,
    hexSizePx: Float
) {
    if (progress < FIRING_PHASE_END) {
        // Phase 1: Throwing flash on the source tile (blue-white warrior glow)
        val fp = progress / FIRING_PHASE_END
        val glowAlpha = if (fp < 0.5f) fp * 2f else (1f - fp) * 2f

        // Outer blue-white glow ring
        drawCircle(
            color = Color(0xFF4488FF).copy(alpha = glowAlpha * 0.35f),
            radius = hexSizePx * (0.65f + fp * 0.35f),
            center = sourceCenter
        )
        // Inner bright white flash
        drawCircle(
            color = Color(0xFFFFFFFF).copy(alpha = glowAlpha * 0.55f),
            radius = hexSizePx * (0.32f + fp * 0.18f),
            center = sourceCenter
        )
    } else if (progress < 1f) {
        // Phase 2: Spear in flight
        val flyProgress = (progress - FIRING_PHASE_END) / (1f - FIRING_PHASE_END)

        val dx = targetCenter.x - sourceCenter.x
        val dy = targetCenter.y - sourceCenter.y
        val dist = sqrt(dx * dx + dy * dy)
        if (dist < 0.01f) return

        // Normalized direction and perpendicular vectors
        val nx = dx / dist
        val ny = dy / dist
        val px = -ny
        val py = nx

        val spearLength = hexSizePx * 1.5f
        val shaftWidth = hexSizePx * 0.10f
        val headLength = hexSizePx * 0.42f
        val headWidth = hexSizePx * 0.20f

        val spearTipX = sourceCenter.x + dx * flyProgress
        val spearTipY = sourceCenter.y + dy * flyProgress
        val tipCenter = Offset(spearTipX, spearTipY)
        val tailCenter = Offset(spearTipX - nx * spearLength, spearTipY - ny * spearLength)

        // Drop shadow (slightly offset)
        val shadowOff = shaftWidth * 0.6f
        drawLine(
            color = Color(0x44000000),
            start = Offset(tailCenter.x + shadowOff, tailCenter.y + shadowOff),
            end = Offset(tipCenter.x + shadowOff, tipCenter.y + shadowOff),
            strokeWidth = shaftWidth,
            cap = StrokeCap.Round
        )

        // Spear shaft (dark brown hardwood)
        drawLine(
            color = Color(0xFF5C3317),
            start = tailCenter,
            end = Offset(tipCenter.x - nx * headLength * 0.6f, tipCenter.y - ny * headLength * 0.6f),
            strokeWidth = shaftWidth,
            cap = StrokeCap.Round
        )

        // Spearhead (metallic silver triangle)
        val tipX = tipCenter.x
        val tipY = tipCenter.y
        val baseLeft = Offset(
            tipX - nx * headLength - px * headWidth * 0.5f,
            tipY - ny * headLength - py * headWidth * 0.5f
        )
        val baseRight = Offset(
            tipX - nx * headLength + px * headWidth * 0.5f,
            tipY - ny * headLength + py * headWidth * 0.5f
        )

        val spearHeadPath = Path().apply {
            moveTo(tipX, tipY)
            lineTo(baseLeft.x, baseLeft.y)
            lineTo(baseRight.x, baseRight.y)
            close()
        }
        drawPath(spearHeadPath, Color(0xFFD0D0D0))

        // Spearhead highlight (lighter silver streak along the center)
        drawLine(
            color = Color(0xCCFFFFFF),
            start = Offset(tipX - nx * headLength * 0.65f, tipY - ny * headLength * 0.65f),
            end = tipCenter,
            strokeWidth = shaftWidth * 0.35f,
            cap = StrokeCap.Round
        )

        // Tail grip wrap (darker band near the tail end)
        val wrapCenter = Offset(
            tailCenter.x + nx * spearLength * 0.12f,
            tailCenter.y + ny * spearLength * 0.12f
        )
        drawLine(
            color = Color(0xFF3A200A),
            start = Offset(wrapCenter.x - nx * shaftWidth, wrapCenter.y - ny * shaftWidth),
            end = Offset(wrapCenter.x + nx * shaftWidth, wrapCenter.y + ny * shaftWidth),
            strokeWidth = shaftWidth * 1.6f,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Standalone preview of the spear throw animation for the animation test screen.
 * Shows the throwing flash on the left, then the spear flying to the right, looping.
 */
@Composable
fun SpearAttackTestPreview(modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            progress.snapTo(0f)
            progress.animateTo(1f, animationSpec = tween(1000, easing = LinearEasing))
            delay(400L)
        }
    }

    val p = progress.value

    Canvas(modifier = modifier) {
        val sourceX = size.width * 0.15f
        val targetX = size.width * 0.85f
        val midY = size.height * 0.5f
        val sourceCenter = Offset(sourceX, midY)
        val targetCenter = Offset(targetX, midY)
        val hexSizePx = size.height * 0.35f

        drawSpear(p, sourceCenter, targetCenter, hexSizePx)
    }
}
