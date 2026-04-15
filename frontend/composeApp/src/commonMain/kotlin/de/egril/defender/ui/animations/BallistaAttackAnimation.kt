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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import de.egril.defender.model.BallistaAttackEffect
import de.egril.defender.model.Position
import de.egril.defender.ui.hexagon.HexagonalGridConstants
import de.egril.defender.ui.gameplay.GamePlayConstants
import kotlinx.coroutines.delay
import kotlin.math.sqrt

/** Fraction of the animation (0–1) spent on the muzzle flash before the rock starts flying. */
private const val FIRING_PHASE_END = 0.20f

/** Rock radius as a fraction of the hex size. */
private const val ROCK_RADIUS_RATIO = 0.30f

/**
 * Map-level overlay animation for ballista attacks.
 *
 * Unlike the per-tile arrow animation, the ballista fires a single large rock that travels
 * across multiple tiles. This Canvas-based overlay is rendered at the same coordinate space
 * as the tiles (via the HexagonalMapView overlayContent slot) so it can span any distance.
 *
 * Animation sequence:
 *  1. Muzzle flash on the source tile (0–20% of total duration)
 *  2. Rock flies from source to target (20–100% of total duration)
 *  3. Impact at target handled by the existing TowerAttackImpactAnimation (delayed by
 *     [GamePlayConstants.AnimationTimings.BALLISTA_FLIGHT_DELAY_MS])
 *
 * @param effects     Active ballista attack effects to render.
 * @param hexSizeDp   Hex size in dp (matches the map's hexSize value, typically 40f).
 * @param contentSize Pixel dimensions of the map content area (used to size the Canvas).
 * @param animate     When false the animation is skipped.
 */
@Composable
fun BallistaAttackOverlay(
    effects: List<BallistaAttackEffect>,
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
            SingleBallistaRockOverlay(
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
private fun SingleBallistaRockOverlay(
    effect: BallistaAttackEffect,
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

    // progress 0.0 → 1.0 covers the full flight duration
    val overallProgress = remember { Animatable(0f) }

    LaunchedEffect(effect.turnNumber, effect.sourcePosition) {
        if (animate) {
            overallProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = GamePlayConstants.AnimationTimings.BALLISTA_FLIGHT_DELAY_MS.toInt(),
                    easing = LinearEasing
                )
            )
        } else {
            overallProgress.snapTo(1f)
        }
    }

    val progress = overallProgress.value

    Canvas(modifier = Modifier.requiredSize(contentWidth, contentHeight)) {
        drawBallistaRock(progress, sourceCenter, targetCenter, hexSizePx)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBallistaRock(
    progress: Float,
    sourceCenter: Offset,
    targetCenter: Offset,
    hexSizePx: Float
) {
    val rockRadius = hexSizePx * ROCK_RADIUS_RATIO

    if (progress < FIRING_PHASE_END) {
        // Phase 1: Muzzle flash / ballista firing glow on the source tile
        val fp = progress / FIRING_PHASE_END
        val glowAlpha = if (fp < 0.5f) fp * 2f else (1f - fp) * 2f

        // Outer orange glow ring
        drawCircle(
            color = Color(0xFFFF8C00).copy(alpha = glowAlpha * 0.35f),
            radius = hexSizePx * (0.7f + fp * 0.4f),
            center = sourceCenter
        )
        // Inner bright golden flash
        drawCircle(
            color = Color(0xFFFFD700).copy(alpha = glowAlpha * 0.55f),
            radius = hexSizePx * (0.35f + fp * 0.2f),
            center = sourceCenter
        )
    } else if (progress < 1f) {
        // Phase 2: Rock in flight
        val flyProgress = (progress - FIRING_PHASE_END) / (1f - FIRING_PHASE_END)
        val rockX = sourceCenter.x + (targetCenter.x - sourceCenter.x) * flyProgress
        val rockY = sourceCenter.y + (targetCenter.y - sourceCenter.y) * flyProgress
        val rockCenter = Offset(rockX, rockY)

        // Drop shadow (offset slightly down-right)
        drawCircle(
            color = Color(0x55000000),
            radius = rockRadius * 0.9f,
            center = Offset(rockCenter.x + rockRadius * 0.3f, rockCenter.y + rockRadius * 0.3f)
        )
        // Rock body – dark brownish-grey boulder
        drawCircle(
            color = Color(0xFF7A6A55),
            radius = rockRadius,
            center = rockCenter
        )
        // Slightly lighter surface layer for depth
        drawCircle(
            color = Color(0xFF9B8B73),
            radius = rockRadius * 0.75f,
            center = Offset(rockCenter.x - rockRadius * 0.08f, rockCenter.y - rockRadius * 0.08f)
        )
        // Highlight spot (upper-left)
        drawCircle(
            color = Color(0xAAC4B09A),
            radius = rockRadius * 0.35f,
            center = Offset(rockCenter.x - rockRadius * 0.28f, rockCenter.y - rockRadius * 0.32f)
        )
    }
}

/**
 * Standalone preview of the ballista rock animation for the animation test screen.
 * Shows the muzzle flash on the left, then the rock flying to the right, looping.
 */
@Composable
fun BallistaAttackTestPreview(modifier: Modifier = Modifier) {
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
        val rockRadius = size.width * 0.045f  // fixed size for the standalone preview
        val sourceX = size.width * 0.15f
        val targetX = size.width * 0.85f
        val midY = size.height * 0.5f
        val sourceCenter = Offset(sourceX, midY)

        if (p < FIRING_PHASE_END) {
            val fp = p / FIRING_PHASE_END
            val glowAlpha = if (fp < 0.5f) fp * 2f else (1f - fp) * 2f
            // Outer glow
            drawCircle(
                color = Color(0xFFFF8C00).copy(alpha = glowAlpha * 0.35f),
                radius = rockRadius * (2.2f + fp * 1.4f),
                center = sourceCenter
            )
            // Inner bright flash
            drawCircle(
                color = Color(0xFFFFD700).copy(alpha = glowAlpha * 0.55f),
                radius = rockRadius * (1.1f + fp * 0.7f),
                center = sourceCenter
            )
        } else if (p < 1f) {
            val flyProgress = (p - FIRING_PHASE_END) / (1f - FIRING_PHASE_END)
            val rockX = sourceX + (targetX - sourceX) * flyProgress
            val rockCenter = Offset(rockX, midY)

            // Shadow
            drawCircle(
                color = Color(0x55000000),
                radius = rockRadius * 0.9f,
                center = Offset(rockCenter.x + rockRadius * 0.3f, rockCenter.y + rockRadius * 0.3f)
            )
            // Rock body
            drawCircle(color = Color(0xFF7A6A55), radius = rockRadius, center = rockCenter)
            // Surface layer
            drawCircle(
                color = Color(0xFF9B8B73),
                radius = rockRadius * 0.75f,
                center = Offset(rockCenter.x - rockRadius * 0.08f, rockCenter.y - rockRadius * 0.08f)
            )
            // Highlight
            drawCircle(
                color = Color(0xAAC4B09A),
                radius = rockRadius * 0.35f,
                center = Offset(rockCenter.x - rockRadius * 0.28f, rockCenter.y - rockRadius * 0.32f)
            )
        }
    }
}
