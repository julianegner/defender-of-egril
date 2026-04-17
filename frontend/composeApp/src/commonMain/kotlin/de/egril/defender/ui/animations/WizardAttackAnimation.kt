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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import de.egril.defender.model.Position
import de.egril.defender.model.WizardAttackEffect
import de.egril.defender.ui.hexagon.HexagonalGridConstants
import de.egril.defender.ui.gameplay.GamePlayConstants
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Fraction of the animation (0–1) spent on the charge glow before the fireball starts flying. */
private const val CHARGE_PHASE_END = 0.20f

/**
 * Map-level overlay animation for wizard tower attacks.
 *
 * A fireball with a trailing flame tail flies from the wizard tower tile to the target tile
 * across multiple tiles. This Canvas-based overlay is rendered at the same coordinate space
 * as the tiles (via the HexagonalMapView overlayContent slot) so it can span any distance.
 *
 * Animation sequence:
 *  1. Purple/orange charge glow on the source tile (0–20% of total duration)
 *  2. Fireball with flame trail flies from source to target (20–100% of total duration)
 *  3. Impact at target handled by the existing TowerAttackImpactAnimation (delayed by
 *     [GamePlayConstants.AnimationTimings.WIZARD_FLIGHT_DELAY_MS])
 *
 * @param effects     Active wizard attack effects to render.
 * @param hexSizeDp   Hex size in dp (matches the map's hexSize value, typically 40f).
 * @param contentSize Pixel dimensions of the map content area (used to size the Canvas).
 * @param animate     When false the animation is skipped.
 */
@Composable
fun WizardAttackOverlay(
    effects: List<WizardAttackEffect>,
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
            SingleFireballOverlay(
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
private fun SingleFireballOverlay(
    effect: WizardAttackEffect,
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
                    durationMillis = GamePlayConstants.AnimationTimings.WIZARD_FLIGHT_DELAY_MS.toInt(),
                    easing = LinearEasing
                )
            )
        } else {
            overallProgress.snapTo(1f)
        }
    }

    val progress = overallProgress.value

    Canvas(modifier = Modifier.requiredSize(contentWidth, contentHeight)) {
        drawFireball(progress, sourceCenter, targetCenter, hexSizePx)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFireball(
    progress: Float,
    sourceCenter: Offset,
    targetCenter: Offset,
    hexSizePx: Float
) {
    val ballRadius = hexSizePx * 0.28f

    if (progress < CHARGE_PHASE_END) {
        // Phase 1: Wizard charge glow (purple/orange) on the source tile
        val fp = progress / CHARGE_PHASE_END
        val glowAlpha = if (fp < 0.5f) fp * 2f else (1f - fp) * 2f

        // Outer purple glow ring
        drawCircle(
            color = Color(0xFFAA44FF).copy(alpha = glowAlpha * 0.40f),
            radius = hexSizePx * (0.65f + fp * 0.40f),
            center = sourceCenter
        )
        // Mid orange ring
        drawCircle(
            color = Color(0xFFFF8800).copy(alpha = glowAlpha * 0.45f),
            radius = hexSizePx * (0.40f + fp * 0.25f),
            center = sourceCenter
        )
        // Inner white-hot flash
        drawCircle(
            color = Color(0xFFFFFFFF).copy(alpha = glowAlpha * 0.60f),
            radius = hexSizePx * (0.18f + fp * 0.15f),
            center = sourceCenter
        )
    } else if (progress < 1f) {
        // Phase 2: Fireball in flight with trailing flame tail
        val flyProgress = (progress - CHARGE_PHASE_END) / (1f - CHARGE_PHASE_END)

        val dx = targetCenter.x - sourceCenter.x
        val dy = targetCenter.y - sourceCenter.y
        val dist = sqrt(dx * dx + dy * dy)
        if (dist < 0.01f) return

        // Normalized direction vector (toward target) and perpendicular
        val nx = dx / dist
        val ny = dy / dist
        val px = -ny
        val py = nx

        // Current fireball center position
        val ballX = sourceCenter.x + dx * flyProgress
        val ballY = sourceCenter.y + dy * flyProgress
        val ballCenter = Offset(ballX, ballY)

        // Tail length grows with distance traveled
        val tailLength = dist * flyProgress * 0.55f + hexSizePx * 0.4f

        // Draw layered flame trail (from tip toward ball center)
        // Each layer is slightly offset perpendicular to add width and realism.
        // Use sinusoidal offsets keyed on flyProgress to create flickering edges.
        val flickerT = flyProgress * 6f  // phase for flicker

        // Outer flame layer (dark red, widest)
        drawFlameTrail(
            ballCenter = ballCenter,
            nx = nx, ny = ny, px = px, py = py,
            tailLength = tailLength,
            halfWidth = ballRadius * 1.30f,
            color = Color(0xAACC1100),
            segments = 6,
            flickerT = flickerT,
            flickerAmp = 0.18f
        )
        // Mid flame layer (orange)
        drawFlameTrail(
            ballCenter = ballCenter,
            nx = nx, ny = ny, px = px, py = py,
            tailLength = tailLength * 0.75f,
            halfWidth = ballRadius * 1.00f,
            color = Color(0xBBFF5500),
            segments = 6,
            flickerT = flickerT + 1f,
            flickerAmp = 0.14f
        )
        // Inner flame layer (orange-yellow)
        drawFlameTrail(
            ballCenter = ballCenter,
            nx = nx, ny = ny, px = px, py = py,
            tailLength = tailLength * 0.50f,
            halfWidth = ballRadius * 0.70f,
            color = Color(0xCCFF9900),
            segments = 5,
            flickerT = flickerT + 2f,
            flickerAmp = 0.10f
        )
        // Bright core trail (yellow, narrow)
        drawFlameTrail(
            ballCenter = ballCenter,
            nx = nx, ny = ny, px = px, py = py,
            tailLength = tailLength * 0.28f,
            halfWidth = ballRadius * 0.38f,
            color = Color(0xDDFFDD00),
            segments = 4,
            flickerT = flickerT + 3f,
            flickerAmp = 0.06f
        )

        // Fireball outer glow (dark red halo)
        drawCircle(
            color = Color(0xBBCC2200),
            radius = ballRadius * 1.55f,
            center = ballCenter
        )
        // Fireball body (orange-red)
        drawCircle(
            color = Color(0xFFFF4400),
            radius = ballRadius * 1.25f,
            center = ballCenter
        )
        // Fireball mid layer (orange)
        drawCircle(
            color = Color(0xFFFF8800),
            radius = ballRadius * 0.90f,
            center = ballCenter
        )
        // Fireball inner layer (yellow)
        drawCircle(
            color = Color(0xFFFFCC00),
            radius = ballRadius * 0.62f,
            center = ballCenter
        )
        // Fireball core (white-hot)
        drawCircle(
            color = Color(0xFFFFFFFF),
            radius = ballRadius * 0.35f,
            center = Offset(ballCenter.x - nx * ballRadius * 0.10f, ballCenter.y - ny * ballRadius * 0.10f)
        )
    }
}

/**
 * Draws a tapering, slightly wavy flame trail behind the fireball using a polygon path.
 *
 * @param ballCenter  Current center of the fireball ball.
 * @param nx, ny      Normalized direction toward target.
 * @param px, py      Perpendicular vector (left of direction).
 * @param tailLength  How far behind the ball the tail extends.
 * @param halfWidth   Half-width of the tail at the ball end.
 * @param color       Fill color of the trail.
 * @param segments    Number of polygon segments (higher = smoother waviness).
 * @param flickerT    Phase offset for sinusoidal flickering.
 * @param flickerAmp  Amplitude of the perpendicular flicker as a fraction of halfWidth.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFlameTrail(
    ballCenter: Offset,
    nx: Float, ny: Float, px: Float, py: Float,
    tailLength: Float,
    halfWidth: Float,
    color: Color,
    segments: Int,
    flickerT: Float,
    flickerAmp: Float
) {
    if (tailLength < 0.01f || segments < 2) return

    val path = Path()
    val tipX = ballCenter.x - nx * tailLength
    val tipY = ballCenter.y - ny * tailLength

    // Left edge: from ball toward tip
    val leftPoints = mutableListOf<Offset>()
    val rightPoints = mutableListOf<Offset>()
    for (i in 0..segments) {
        val t = i.toFloat() / segments  // 0 = ball end, 1 = tip
        val cx = ballCenter.x - nx * tailLength * t
        val cy = ballCenter.y - ny * tailLength * t
        val width = halfWidth * (1f - t)  // taper to zero at tip
        val flicker = sin((flickerT + t * 3.14159f * 2f).toDouble()).toFloat() * flickerAmp * halfWidth * (1f - t)
        leftPoints.add(Offset(cx + px * (width + flicker), cy + py * (width + flicker)))
        rightPoints.add(Offset(cx - px * (width - flicker), cy - py * (width - flicker)))
    }

    // Build polygon: left side forward, then right side backward
    path.moveTo(leftPoints[0].x, leftPoints[0].y)
    for (i in 1..segments) {
        path.lineTo(leftPoints[i].x, leftPoints[i].y)
    }
    // Merge at tip
    path.lineTo(tipX, tipY)
    for (i in segments downTo 0) {
        path.lineTo(rightPoints[i].x, rightPoints[i].y)
    }
    path.close()

    drawPath(path, color)
}

/**
 * Standalone preview of the wizard fireball animation for the animation test screen.
 * Shows the charge glow on the left, then the fireball flying to the right, looping.
 */
@Composable
fun WizardAttackTestPreview(modifier: Modifier = Modifier) {
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

        drawFireball(p, sourceCenter, targetCenter, hexSizePx)
    }
}
