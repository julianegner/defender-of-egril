package de.egril.defender.ui.animations

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import de.egril.defender.model.AlchemyAttackEffect
import de.egril.defender.model.Position
import de.egril.defender.ui.hexagon.HexagonalGridConstants
import de.egril.defender.ui.gameplay.GamePlayConstants
import kotlinx.coroutines.delay
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Fraction of the animation (0–1) spent on the green charge flash before the vial starts flying. */
private const val CHARGE_PHASE_END = 0.20f

/**
 * Map-level overlay animation for alchemy tower attacks.
 *
 * A glass acid vial (with a round bulb at the bottom, straight tube above, filled with green acid
 * and animated bubbles) flies from the alchemy tower tile to the target tile across multiple tiles.
 * This Canvas-based overlay is rendered at the same coordinate space as the tiles (via the
 * HexagonalMapView overlayContent slot) so it can span any distance.
 *
 * Animation sequence:
 *  1. Green charge flash on the source tile (0–20% of total duration)
 *  2. Acid vial flies from source to target (20–100% of total duration)
 *  3. Impact at target handled by the existing TowerAttackImpactAnimation (delayed by
 *     [GamePlayConstants.AnimationTimings.ALCHEMY_FLIGHT_DELAY_MS])
 *
 * @param effects     Active alchemy attack effects to render.
 * @param hexSizeDp   Hex size in dp (matches the map's hexSize value, typically 40f).
 * @param contentSize Pixel dimensions of the map content area (used to size the Canvas).
 * @param animate     When false the animation is skipped.
 */
@Composable
fun AlchemyAttackOverlay(
    effects: List<AlchemyAttackEffect>,
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
            SingleVialOverlay(
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
private fun SingleVialOverlay(
    effect: AlchemyAttackEffect,
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
                    durationMillis = GamePlayConstants.AnimationTimings.ALCHEMY_FLIGHT_DELAY_MS.toInt(),
                    easing = LinearEasing
                )
            )
        } else {
            overallProgress.snapTo(1f)
        }
    }

    val progress = overallProgress.value

    Canvas(modifier = Modifier.requiredSize(contentWidth, contentHeight)) {
        drawAcidVial(progress, sourceCenter, targetCenter, hexSizePx)
    }
}

private fun DrawScope.drawAcidVial(
    progress: Float,
    sourceCenter: Offset,
    targetCenter: Offset,
    hexSizePx: Float
) {
    if (progress < CHARGE_PHASE_END) {
        // Phase 1: Alchemy tower charge glow (green)
        val fp = progress / CHARGE_PHASE_END
        val glowAlpha = if (fp < 0.5f) fp * 2f else (1f - fp) * 2f

        // Outer green glow ring
        drawCircle(
            color = Color(0xFF00CC44).copy(alpha = glowAlpha * 0.40f),
            radius = hexSizePx * (0.60f + fp * 0.35f),
            center = sourceCenter
        )
        // Inner bright green flash
        drawCircle(
            color = Color(0xFF88FF44).copy(alpha = glowAlpha * 0.55f),
            radius = hexSizePx * (0.28f + fp * 0.18f),
            center = sourceCenter
        )
        // White-hot center
        drawCircle(
            color = Color(0xFFFFFFFF).copy(alpha = glowAlpha * 0.50f),
            radius = hexSizePx * (0.12f + fp * 0.10f),
            center = sourceCenter
        )
    } else if (progress < 1f) {
        // Phase 2: Acid vial in flight
        val flyProgress = (progress - CHARGE_PHASE_END) / (1f - CHARGE_PHASE_END)

        val dx = targetCenter.x - sourceCenter.x
        val dy = targetCenter.y - sourceCenter.y
        val dist = sqrt(dx * dx + dy * dy)
        if (dist < 0.01f) return

        // Current vial center position (follows linear path)
        val vialX = sourceCenter.x + dx * flyProgress
        val vialY = sourceCenter.y + dy * flyProgress
        val vialCenter = Offset(vialX, vialY)

        // Rotation angle: vial points in the direction of travel, with a gentle tumble
        // The vial tumbles 270° over the full flight (so it spins gently)
        val flightAngleDeg = (atan2(dy.toDouble(), dx.toDouble()) * GamePlayConstants.AnimationTimings.RADIANS_TO_DEGREES).toFloat()
        val tumbleDeg = flyProgress * 270f
        val totalRotationDeg = flightAngleDeg + 90f + tumbleDeg  // +90 so tube points forward

        // Vial size proportional to hexSize
        val bulbRadius = hexSizePx * 0.18f
        val tubeHalfWidth = hexSizePx * 0.07f
        val tubeLength = hexSizePx * 0.38f
        val stopperLength = hexSizePx * 0.09f
        val stopperHalfWidth = tubeHalfWidth * 1.3f

        // Draw the vial rotated to face the direction of travel
        rotate(totalRotationDeg, pivot = vialCenter) {
            // Acid bubble trail behind the vial (small green dots)
            for (b in 0 until 4) {
                val trailT = flyProgress - (b + 1) * 0.06f
                if (trailT > 0f) {
                    val trailX = sourceCenter.x + dx * trailT
                    val trailY = sourceCenter.y + dy * trailT
                    val bubbleAlpha = (0.5f - b * 0.1f).coerceAtLeast(0f)
                    drawCircle(
                        color = Color(0xFF44EE44).copy(alpha = bubbleAlpha),
                        radius = bulbRadius * (0.25f - b * 0.05f).coerceAtLeast(0.05f),
                        center = Offset(trailX, trailY)
                    )
                }
            }

            // Bulb bottom: round glass sphere (semi-transparent)
            // Acid fill (green, mostly fills the bulb)
            drawCircle(
                color = Color(0xCC22DD44),
                radius = bulbRadius * 0.88f,
                center = vialCenter
            )
            // Glass outer ring of bulb (semi-transparent)
            drawCircle(
                color = Color(0x55AAFFCC),
                radius = bulbRadius,
                center = vialCenter
            )
            // Glass outline of bulb
            drawCircle(
                color = Color(0xBB88FFAA),
                radius = bulbRadius,
                center = vialCenter,
                style = Stroke(width = tubeHalfWidth * 0.5f)
            )
            // Bulb highlight (upper-left specular)
            drawCircle(
                color = Color(0xAAFFFFFF),
                radius = bulbRadius * 0.28f,
                center = Offset(
                    vialCenter.x - bulbRadius * 0.32f,
                    vialCenter.y - bulbRadius * 0.35f
                )
            )

            // Tube (neck above the bulb): from bulb top upward
            val tubeBottomY = vialCenter.y - bulbRadius
            val tubeTopY = tubeBottomY - tubeLength

            // Acid fill in tube (lower half of tube filled with green)
            val acidFillHeight = tubeLength * 0.55f
            drawRect(
                color = Color(0xCC22DD44),
                topLeft = Offset(vialCenter.x - tubeHalfWidth * 0.85f, tubeTopY + (tubeLength - acidFillHeight)),
                size = Size(tubeHalfWidth * 1.70f, acidFillHeight)
            )
            // Glass tube walls (semi-transparent)
            drawRect(
                color = Color(0x3388FFCC),
                topLeft = Offset(vialCenter.x - tubeHalfWidth, tubeTopY),
                size = Size(tubeHalfWidth * 2f, tubeLength)
            )
            // Tube left outline
            drawLine(
                color = Color(0xBB88FFAA),
                start = Offset(vialCenter.x - tubeHalfWidth, tubeBottomY),
                end = Offset(vialCenter.x - tubeHalfWidth, tubeTopY),
                strokeWidth = tubeHalfWidth * 0.4f,
                cap = StrokeCap.Round
            )
            // Tube right outline
            drawLine(
                color = Color(0xBB88FFAA),
                start = Offset(vialCenter.x + tubeHalfWidth, tubeBottomY),
                end = Offset(vialCenter.x + tubeHalfWidth, tubeTopY),
                strokeWidth = tubeHalfWidth * 0.4f,
                cap = StrokeCap.Round
            )
            // Tube top cap line
            drawLine(
                color = Color(0xBB88FFAA),
                start = Offset(vialCenter.x - tubeHalfWidth, tubeTopY),
                end = Offset(vialCenter.x + tubeHalfWidth, tubeTopY),
                strokeWidth = tubeHalfWidth * 0.4f,
                cap = StrokeCap.Round
            )

            // Stopper (cork) at the very top
            val stopperBottomY = tubeTopY
            val stopperTopY = stopperBottomY - stopperLength
            drawRect(
                color = Color(0xFF996633),
                topLeft = Offset(vialCenter.x - stopperHalfWidth, stopperTopY),
                size = Size(stopperHalfWidth * 2f, stopperLength)
            )
            drawLine(
                color = Color(0xFF774422),
                start = Offset(vialCenter.x - stopperHalfWidth, stopperTopY),
                end = Offset(vialCenter.x + stopperHalfWidth, stopperTopY),
                strokeWidth = tubeHalfWidth * 0.5f,
                cap = StrokeCap.Round
            )

            // Animated bubbles inside the bulb
            // Each bubble rises from bottom to top of bulb based on flyProgress
            val bubblePositions = listOf(
                Pair(-0.30f, (flyProgress * 2.1f) % 1.0f),
                Pair(0.10f,  (flyProgress * 1.7f + 0.33f) % 1.0f),
                Pair(0.35f,  (flyProgress * 2.4f + 0.66f) % 1.0f),
                Pair(-0.05f, (flyProgress * 1.5f + 0.50f) % 1.0f),
            )
            for ((xFrac, t) in bubblePositions) {
                // Bubbles rise from bottom (t=0) to top (t=1) of the bulb
                val bx = vialCenter.x + xFrac * bulbRadius * 0.70f
                val by = vialCenter.y + bulbRadius * 0.60f - t * bulbRadius * 1.20f
                // Only draw if inside the bulb region
                if (by > vialCenter.y - bulbRadius * 0.90f && by < vialCenter.y + bulbRadius * 0.80f) {
                    drawCircle(
                        color = Color(0xAA88FFCC),
                        radius = bulbRadius * 0.10f,
                        center = Offset(bx, by)
                    )
                }
            }
        }
    }
}

/**
 * Standalone preview of the acid vial animation for the animation test screen.
 * Shows the charge glow on the left, then the vial flying to the right, looping.
 */
@Composable
fun AlchemyAttackTestPreview(modifier: Modifier = Modifier) {
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
        val hexSizePx = size.height * 0.35f
        val sourceCenter = Offset(sourceX, midY)
        val targetCenter = Offset(targetX, midY)

        drawAcidVial(p, sourceCenter, targetCenter, hexSizePx)
    }
}
