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
import de.egril.defender.model.BowAttackEffect
import de.egril.defender.model.Position
import de.egril.defender.ui.hexagon.HexagonalGridConstants
import de.egril.defender.ui.gameplay.GamePlayConstants
import kotlinx.coroutines.delay
import kotlin.math.sqrt

/** Fraction of the animation (0–1) spent on the bow-string flash before the arrows start flying. */
private const val FIRING_PHASE_END = 0.20f

/** Number of arrows in the volley. */
private const val VOLLEY_SIZE = 3

/**
 * Map-level overlay animation for bow tower attacks.
 *
 * A volley of [VOLLEY_SIZE] arrows flies from the bow tower tile to the target tile across
 * multiple tiles. This Canvas-based overlay is rendered at the same coordinate space as the
 * tiles (via the HexagonalMapView overlayContent slot) so it can span any distance.
 *
 * Animation sequence:
 *  1. Bow-string flash on the source tile (0–20% of total duration)
 *  2. Arrow volley flies from source to target (20–100% of total duration)
 *  3. Impact at target handled by the existing TowerAttackImpactAnimation (delayed by
 *     [GamePlayConstants.AnimationTimings.ARROW_FLIGHT_DELAY_MS])
 *
 * @param effects     Active bow attack effects to render.
 * @param hexSizeDp   Hex size in dp (matches the map's hexSize value, typically 40f).
 * @param contentSize Pixel dimensions of the map content area (used to size the Canvas).
 * @param animate     When false the animation is skipped.
 */
@Composable
fun BowAttackOverlay(
    effects: List<BowAttackEffect>,
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
            SingleBowVolleyOverlay(
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
private fun SingleBowVolleyOverlay(
    effect: BowAttackEffect,
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
        drawBowVolley(progress, sourceCenter, targetCenter, hexSizePx)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBowVolley(
    progress: Float,
    sourceCenter: Offset,
    targetCenter: Offset,
    hexSizePx: Float
) {
    if (progress < FIRING_PHASE_END) {
        // Phase 1: Bow-string snap flash on the source tile
        val fp = progress / FIRING_PHASE_END
        val glowAlpha = if (fp < 0.5f) fp * 2f else (1f - fp) * 2f

        // Outer yellow-green glow ring
        drawCircle(
            color = Color(0xFF99CC00).copy(alpha = glowAlpha * 0.35f),
            radius = hexSizePx * (0.6f + fp * 0.35f),
            center = sourceCenter
        )
        // Inner bright flash
        drawCircle(
            color = Color(0xFFFFFF00).copy(alpha = glowAlpha * 0.55f),
            radius = hexSizePx * (0.3f + fp * 0.18f),
            center = sourceCenter
        )
    } else if (progress < 1f) {
        // Phase 2: Arrow volley in flight
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

        val arrowLength = hexSizePx * 0.75f
        val spread = hexSizePx * 0.22f
        val shaftWidth = hexSizePx * 0.06f
        val headSize = hexSizePx * 0.18f

        // Draw each arrow in the volley, fanned out perpendicular to the flight direction
        for (i in -1..1) {
            val offsetX = px * spread * i
            val offsetY = py * spread * i

            val arrowTipX = sourceCenter.x + dx * flyProgress + offsetX
            val arrowTipY = sourceCenter.y + dy * flyProgress + offsetY
            val tipCenter = Offset(arrowTipX, arrowTipY)
            val tailCenter = Offset(arrowTipX - nx * arrowLength, arrowTipY - ny * arrowLength)

            // Arrow shaft (dark brown wood)
            drawLine(
                color = Color(0xFF8B4513),
                start = tailCenter,
                end = Offset(tipCenter.x - nx * headSize * 0.5f, tipCenter.y - ny * headSize * 0.5f),
                strokeWidth = shaftWidth,
                cap = StrokeCap.Round
            )

            // Arrowhead (silver triangle pointing in direction of travel)
            val tipX = tipCenter.x
            val tipY = tipCenter.y
            val baseLeft = Offset(
                tipX - nx * headSize - px * headSize * 0.55f,
                tipY - ny * headSize - py * headSize * 0.55f
            )
            val baseRight = Offset(
                tipX - nx * headSize + px * headSize * 0.55f,
                tipY - ny * headSize + py * headSize * 0.55f
            )

            val arrowHeadPath = Path().apply {
                moveTo(tipX, tipY)
                lineTo(baseLeft.x, baseLeft.y)
                lineTo(baseRight.x, baseRight.y)
                close()
            }
            drawPath(arrowHeadPath, Color(0xFFC8C8C8))

            // Small tail feathers (fletching) — two short lines at the tail angled ±30° from shaft
            val fletchLength = arrowLength * 0.22f
            val fletchSpread = hexSizePx * 0.07f
            drawLine(
                color = Color(0xFFCCCCFF).copy(alpha = 0.8f),
                start = tailCenter,
                end = Offset(tailCenter.x + nx * fletchLength + px * fletchSpread, tailCenter.y + ny * fletchLength + py * fletchSpread),
                strokeWidth = shaftWidth * 0.7f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color(0xFFCCCCFF).copy(alpha = 0.8f),
                start = tailCenter,
                end = Offset(tailCenter.x + nx * fletchLength - px * fletchSpread, tailCenter.y + ny * fletchLength - py * fletchSpread),
                strokeWidth = shaftWidth * 0.7f,
                cap = StrokeCap.Round
            )
        }
    }
}

/**
 * Standalone preview of the bow arrow volley animation for the animation test screen.
 * Shows the bow-string flash on the left, then the volley flying to the right, looping.
 */
@Composable
fun BowAttackTestPreview(modifier: Modifier = Modifier) {
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
        val arrowLength = size.width * 0.12f
        val sourceX = size.width * 0.15f
        val targetX = size.width * 0.85f
        val midY = size.height * 0.5f
        val sourceCenter = Offset(sourceX, midY)
        val targetCenter = Offset(targetX, midY)
        val hexSizePx = size.height * 0.35f

        drawBowVolley(p, sourceCenter, targetCenter, hexSizePx)
    }
}
