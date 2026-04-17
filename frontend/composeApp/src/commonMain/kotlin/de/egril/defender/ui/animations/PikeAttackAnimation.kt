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
import de.egril.defender.model.PikeAttackEffect
import de.egril.defender.model.Position
import de.egril.defender.ui.hexagon.HexagonalGridConstants
import de.egril.defender.ui.gameplay.GamePlayConstants
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Fraction of the total animation (0–1) spent extending the pike before it reaches the target.
 * The remaining fraction is used for the inline impact burst at the target.
 */
private const val EXTEND_PHASE_END = 0.75f

/**
 * Map-level overlay animation for pike (spike) tower attacks.
 *
 * The pike extends from the center of the tower tile toward the center of the target tile.
 * The tail stays fixed at the source while the tip moves forward; the wooden shaft grows
 * longer as the tip travels. Once the tip reaches the target center an impact burst is drawn
 * inline, after which the entire animation ends and all parts are removed.
 *
 * This Canvas-based overlay is rendered at the same coordinate space as the tiles (via the
 * HexagonalMapView overlayContent slot) so it can span any distance.
 *
 * Animation sequence:
 *  1. Pike extends from source to target (0–75% of total duration)
 *  2. Impact burst at target (75–100% of total duration)
 *  3. Impact at target handled by the existing TowerAttackImpactAnimation (delayed by
 *     [GamePlayConstants.AnimationTimings.PIKE_EXTEND_DELAY_MS])
 *
 * @param effects     Active pike attack effects to render.
 * @param hexSizeDp   Hex size in dp (matches the map's hexSize value, typically 40f).
 * @param contentSize Pixel dimensions of the map content area (used to size the Canvas).
 * @param animate     When false the animation is skipped.
 */
@Composable
fun PikeAttackOverlay(
    effects: List<PikeAttackEffect>,
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
            SinglePikeOverlay(
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
private fun SinglePikeOverlay(
    effect: PikeAttackEffect,
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
                    durationMillis = GamePlayConstants.AnimationTimings.PIKE_EXTEND_DELAY_MS.toInt(),
                    easing = LinearEasing
                )
            )
        } else {
            overallProgress.snapTo(1f)
        }
    }

    val progress = overallProgress.value

    Canvas(modifier = Modifier.requiredSize(contentWidth, contentHeight)) {
        drawPike(progress, sourceCenter, targetCenter, hexSizePx)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPike(
    progress: Float,
    sourceCenter: Offset,
    targetCenter: Offset,
    hexSizePx: Float
) {
    if (progress >= 1f) return

    val dx = targetCenter.x - sourceCenter.x
    val dy = targetCenter.y - sourceCenter.y
    val dist = sqrt(dx * dx + dy * dy)
    if (dist < 0.01f) return

    // Normalized direction and perpendicular vectors
    val nx = dx / dist
    val ny = dy / dist
    val px = -ny
    val py = nx

    val shaftWidth = hexSizePx * 0.09f
    val headLength = hexSizePx * 0.38f
    val headWidth = hexSizePx * 0.18f

    if (progress < EXTEND_PHASE_END) {
        // Phase 1: Pike extends – tail stays at source, tip moves toward target
        val extendProgress = progress / EXTEND_PHASE_END

        val tipX = sourceCenter.x + dx * extendProgress
        val tipY = sourceCenter.y + dy * extendProgress
        val tipCenter = Offset(tipX, tipY)

        // The tail of the shaft is fixed at the source center
        val tailCenter = sourceCenter

        // Only draw shaft if the tip has moved far enough from the source
        val travelDist = dist * extendProgress
        if (travelDist > headLength * 0.6f) {
            // Drop shadow
            val shadowOff = shaftWidth * 0.5f
            drawLine(
                color = Color(0x44000000),
                start = Offset(tailCenter.x + shadowOff, tailCenter.y + shadowOff),
                end = Offset(tipCenter.x - nx * headLength * 0.5f + shadowOff, tipCenter.y - ny * headLength * 0.5f + shadowOff),
                strokeWidth = shaftWidth,
                cap = StrokeCap.Round
            )

            // Shaft (dark brown hardwood)
            drawLine(
                color = Color(0xFF6B3A1F),
                start = tailCenter,
                end = Offset(tipCenter.x - nx * headLength * 0.55f, tipCenter.y - ny * headLength * 0.55f),
                strokeWidth = shaftWidth,
                cap = StrokeCap.Round
            )

            // Grip wrap near the tail (darker band)
            val wrapCenter = Offset(
                tailCenter.x + nx * shaftWidth * 1.5f,
                tailCenter.y + ny * shaftWidth * 1.5f
            )
            drawLine(
                color = Color(0xFF3A200A),
                start = Offset(wrapCenter.x - nx * shaftWidth, wrapCenter.y - ny * shaftWidth),
                end = Offset(wrapCenter.x + nx * shaftWidth, wrapCenter.y + ny * shaftWidth),
                strokeWidth = shaftWidth * 1.8f,
                cap = StrokeCap.Round
            )
        }

        // Pike head (metallic silver/steel triangle)
        val tipPoint = tipCenter
        val baseLeft = Offset(
            tipPoint.x - nx * headLength - px * headWidth * 0.5f,
            tipPoint.y - ny * headLength - py * headWidth * 0.5f
        )
        val baseRight = Offset(
            tipPoint.x - nx * headLength + px * headWidth * 0.5f,
            tipPoint.y - ny * headLength + py * headWidth * 0.5f
        )

        val headPath = Path().apply {
            moveTo(tipPoint.x, tipPoint.y)
            lineTo(baseLeft.x, baseLeft.y)
            lineTo(baseRight.x, baseRight.y)
            close()
        }
        // Steel-colored head with slight blue tint
        drawPath(headPath, Color(0xFFB8C8D8))

        // Head highlight (bright steel streak along center)
        drawLine(
            color = Color(0xCCEEF4FF),
            start = Offset(tipPoint.x - nx * headLength * 0.7f, tipPoint.y - ny * headLength * 0.7f),
            end = tipPoint,
            strokeWidth = shaftWidth * 0.3f,
            cap = StrokeCap.Round
        )

        // Head base reinforcement ring (dark metal band where head meets shaft)
        if (travelDist > headLength * 0.8f) {
            val ringCenter = Offset(
                tipPoint.x - nx * headLength,
                tipPoint.y - ny * headLength
            )
            drawLine(
                color = Color(0xFF505050),
                start = Offset(ringCenter.x - px * headWidth * 0.55f, ringCenter.y - py * headWidth * 0.55f),
                end = Offset(ringCenter.x + px * headWidth * 0.55f, ringCenter.y + py * headWidth * 0.55f),
                strokeWidth = shaftWidth * 1.4f,
                cap = StrokeCap.Round
            )
        }
    } else {
        // Phase 2: Impact burst at target – pike tip has reached the target
        val hitProgress = (progress - EXTEND_PHASE_END) / (1f - EXTEND_PHASE_END)
        drawPikeImpactBurst(targetCenter, hitProgress, hexSizePx)
    }
}

/** Draws a radiating burst at [center] to represent the pike's impact. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPikeImpactBurst(
    center: Offset,
    hitProgress: Float,
    hexSizePx: Float
) {
    val maxLineLength = hexSizePx * 0.50f
    val innerRadius = hexSizePx * 0.10f
    val alpha = (1f - hitProgress) * 0.90f
    val lineCount = 6
    val lineWidth = hexSizePx * 0.06f

    for (i in 0 until lineCount) {
        val angle = (i.toFloat() / lineCount) * 2f * PI.toFloat()
        val lineLength = maxLineLength * hitProgress
        val startX = center.x + cos(angle) * innerRadius
        val startY = center.y + sin(angle) * innerRadius
        val endX = center.x + cos(angle) * (innerRadius + lineLength)
        val endY = center.y + sin(angle) * (innerRadius + lineLength)
        drawLine(
            color = Color(0xFFD0E0F0).copy(alpha = alpha),
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = lineWidth,
            cap = StrokeCap.Round
        )
    }

    // Central flash circle
    drawCircle(
        color = Color(0xFFFFFFFF).copy(alpha = alpha * 0.75f),
        radius = hexSizePx * (0.06f + hitProgress * 0.14f),
        center = center
    )
}

/**
 * Standalone preview of the pike extend animation for the animation test screen.
 * Shows the pike extending from the left to the right, then the impact burst, looping.
 */
@Composable
fun PikeAttackTestPreview(modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            progress.snapTo(0f)
            progress.animateTo(1f, animationSpec = tween(900, easing = LinearEasing))
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

        drawPike(p, sourceCenter, targetCenter, hexSizePx)
    }
}
