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
import de.egril.defender.model.ShadowSpewEffect
import de.egril.defender.ui.gameplay.GamePlayConstants
import de.egril.defender.ui.hexagon.HexagonalGridConstants
import kotlin.math.sin
import kotlin.math.sqrt

/** Fraction of the animation (0–1) spent on the shadow charge before the dark fireball starts flying. */
private const val SHADOW_CHARGE_PHASE_END = 0.20f

/**
 * Map-level overlay animation for Xarithon's Shadow Spew ability.
 *
 * A dark fireball (shadow flame) with a trailing void-black flame tail flies from Xarithon's
 * position to the 2×2 target area, mirroring the wizard tower fireball but in shadow colors
 * (void black, dark purple, deep violet).
 *
 * @param effects     Active shadow spew effects to render.
 * @param hexSizeDp   Hex size in dp (matches the map's hexSize value).
 * @param contentSize Pixel dimensions of the map content area (used to size the Canvas).
 * @param animate     When false the animation is skipped.
 */
@Composable
fun XarithonShadowSpewOverlay(
    effects: List<ShadowSpewEffect>,
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
            SingleShadowSpewOverlay(
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
private fun SingleShadowSpewOverlay(
    effect: ShadowSpewEffect,
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

    LaunchedEffect(effect.turnNumber, effect.sourcePosition) {
        if (animate) {
            overallProgress.animateTo(
                targetValue = 1f,
                animationSpec =
                    tween(
                        durationMillis = GamePlayConstants.AnimationTimings.SHADOW_SPEW_FLIGHT_DELAY_MS.toInt(),
                        easing = LinearEasing,
                    ),
            )
        } else {
            overallProgress.snapTo(1f)
        }
    }

    val progress = overallProgress.value

    Canvas(modifier = Modifier.requiredSize(contentWidth, contentHeight)) {
        drawShadowFireball(progress, sourceCenter, targetCenter, hexSizePx)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawShadowFireball(
    progress: Float,
    sourceCenter: Offset,
    targetCenter: Offset,
    hexSizePx: Float,
) {
    val ballRadius = hexSizePx * 0.28f

    if (progress < SHADOW_CHARGE_PHASE_END) {
        // Phase 1: Shadow charge glow (dark purple/black void) on the source tile
        val fp = progress / SHADOW_CHARGE_PHASE_END
        val glowAlpha = if (fp < 0.5f) fp * 2f else (1f - fp) * 2f

        // Outer void purple glow ring
        drawCircle(
            color = Color(0xFF3D0070).copy(alpha = glowAlpha * 0.50f),
            radius = hexSizePx * (0.65f + fp * 0.40f),
            center = sourceCenter,
        )
        // Mid dark void ring
        drawCircle(
            color = Color(0xFF1A0040).copy(alpha = glowAlpha * 0.55f),
            radius = hexSizePx * (0.40f + fp * 0.25f),
            center = sourceCenter,
        )
        // Inner shadow core flash
        drawCircle(
            color = Color(0xFF8800FF).copy(alpha = glowAlpha * 0.65f),
            radius = hexSizePx * (0.18f + fp * 0.15f),
            center = sourceCenter,
        )
    } else if (progress < 1f) {
        // Phase 2: Shadow fireball in flight with void flame trail
        val flyProgress = (progress - SHADOW_CHARGE_PHASE_END) / (1f - SHADOW_CHARGE_PHASE_END)

        val dx = targetCenter.x - sourceCenter.x
        val dy = targetCenter.y - sourceCenter.y
        val dist = sqrt(dx * dx + dy * dy)
        if (dist < 0.01f) return

        val nx = dx / dist
        val ny = dy / dist
        val px = -ny
        val py = nx

        val ballX = sourceCenter.x + dx * flyProgress
        val ballY = sourceCenter.y + dy * flyProgress
        val ballCenter = Offset(ballX, ballY)

        val tailLength = dist * flyProgress * 0.55f + hexSizePx * 0.4f
        val flickerT = flyProgress * 6f

        // Outer shadow trail (near-black void, widest)
        drawShadowFlameTrail(
            ballCenter = ballCenter,
            nx = nx, ny = ny, px = px, py = py,
            tailLength = tailLength,
            halfWidth = ballRadius * 1.30f,
            color = Color(0xAA0D0020),
            segments = 6, flickerT = flickerT, flickerAmp = 0.18f,
        )
        // Mid void purple trail
        drawShadowFlameTrail(
            ballCenter = ballCenter,
            nx = nx, ny = ny, px = px, py = py,
            tailLength = tailLength * 0.75f,
            halfWidth = ballRadius * 1.00f,
            color = Color(0xBB200050),
            segments = 6, flickerT = flickerT + 1f, flickerAmp = 0.14f,
        )
        // Inner deep violet trail
        drawShadowFlameTrail(
            ballCenter = ballCenter,
            nx = nx, ny = ny, px = px, py = py,
            tailLength = tailLength * 0.50f,
            halfWidth = ballRadius * 0.70f,
            color = Color(0xCC5000A0),
            segments = 5, flickerT = flickerT + 2f, flickerAmp = 0.10f,
        )
        // Bright core trail (glowing violet, narrow)
        drawShadowFlameTrail(
            ballCenter = ballCenter,
            nx = nx, ny = ny, px = px, py = py,
            tailLength = tailLength * 0.28f,
            halfWidth = ballRadius * 0.38f,
            color = Color(0xDD8800FF),
            segments = 4, flickerT = flickerT + 3f, flickerAmp = 0.06f,
        )

        // Shadow fireball outer glow (dark void halo)
        drawCircle(
            color = Color(0xBB150030),
            radius = ballRadius * 1.55f,
            center = ballCenter,
        )
        // Shadow fireball body (deep purple)
        drawCircle(
            color = Color(0xFF300060),
            radius = ballRadius * 1.25f,
            center = ballCenter,
        )
        // Shadow fireball mid layer (dark violet)
        drawCircle(
            color = Color(0xFF5500AA),
            radius = ballRadius * 0.90f,
            center = ballCenter,
        )
        // Shadow fireball inner layer (bright violet)
        drawCircle(
            color = Color(0xFF8800EE),
            radius = ballRadius * 0.62f,
            center = ballCenter,
        )
        // Shadow fireball core (glowing purple-white)
        drawCircle(
            color = Color(0xFFBB66FF),
            radius = ballRadius * 0.35f,
            center = Offset(ballCenter.x - nx * ballRadius * 0.10f, ballCenter.y - ny * ballRadius * 0.10f),
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawShadowFlameTrail(
    ballCenter: Offset,
    nx: Float,
    ny: Float,
    px: Float,
    py: Float,
    tailLength: Float,
    halfWidth: Float,
    color: Color,
    segments: Int,
    flickerT: Float,
    flickerAmp: Float,
) {
    if (tailLength < 0.01f || segments < 2) return

    val path = Path()
    val tipX = ballCenter.x - nx * tailLength
    val tipY = ballCenter.y - ny * tailLength

    val leftPoints = mutableListOf<Offset>()
    val rightPoints = mutableListOf<Offset>()
    for (i in 0..segments) {
        val t = i.toFloat() / segments
        val cx = ballCenter.x - nx * tailLength * t
        val cy = ballCenter.y - ny * tailLength * t
        val width = halfWidth * (1f - t)
        val flicker = sin((flickerT + t * kotlin.math.PI.toFloat() * 2f).toDouble()).toFloat() * flickerAmp * halfWidth * (1f - t)
        leftPoints.add(Offset(cx + px * (width + flicker), cy + py * (width + flicker)))
        rightPoints.add(Offset(cx - px * (width - flicker), cy - py * (width - flicker)))
    }

    path.moveTo(leftPoints[0].x, leftPoints[0].y)
    for (i in 1..segments) {
        path.lineTo(leftPoints[i].x, leftPoints[i].y)
    }
    path.lineTo(tipX, tipY)
    for (i in segments downTo 0) {
        path.lineTo(rightPoints[i].x, rightPoints[i].y)
    }
    path.close()

    drawPath(path, color)
}
