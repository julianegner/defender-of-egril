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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import de.egril.defender.model.MorvathShadowOrbEffect
import de.egril.defender.model.Position
import de.egril.defender.ui.gameplay.GamePlayConstants
import de.egril.defender.ui.hexagon.HexagonalGridConstants
import kotlin.math.sqrt

/** Fraction of the flight spent in the charge phase before the orb starts moving. */
private const val ORB_CHARGE_PHASE_END = 0.18f

/**
 * Map-level overlay animation for Morvath's shadow orb ability.
 *
 * A compact shadow orb flies from Morvath's position to the distant tile he is cloaking with
 * shadow. Visually it is a dense dark-violet sphere with a faint trailing wisp — distinct from
 * Xarithon's larger fireball but in the same shadow colour palette.
 */
@Composable
fun MorvathShadowOrbOverlay(
    effects: List<MorvathShadowOrbEffect>,
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
            SingleMorvathOrbOverlay(
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
private fun SingleMorvathOrbOverlay(
    effect: MorvathShadowOrbEffect,
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
                        durationMillis = GamePlayConstants.AnimationTimings.MORVATH_ORB_FLIGHT_DELAY_MS.toInt(),
                        easing = LinearEasing,
                    ),
            )
        } else {
            overallProgress.snapTo(1f)
        }
    }

    Canvas(modifier = Modifier.requiredSize(contentWidth, contentHeight)) {
        drawMorvathShadowOrb(overallProgress.value, sourceCenter, targetCenter, hexSizePx)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMorvathShadowOrb(
    progress: Float,
    sourceCenter: Offset,
    targetCenter: Offset,
    hexSizePx: Float,
) {
    val orbRadius = hexSizePx * 0.20f

    if (progress < ORB_CHARGE_PHASE_END) {
        // Phase 1: dim charge pulse on Morvath's tile
        val fp = progress / ORB_CHARGE_PHASE_END
        val glowAlpha = if (fp < 0.5f) fp * 2f else (1f - fp) * 2f
        drawCircle(
            color = Color(0xFF4A0080).copy(alpha = glowAlpha * 0.55f),
            radius = hexSizePx * (0.30f + fp * 0.20f),
            center = sourceCenter,
        )
        drawCircle(
            color = Color(0xFF9900FF).copy(alpha = glowAlpha * 0.45f),
            radius = hexSizePx * (0.12f + fp * 0.10f),
            center = sourceCenter,
        )
    } else if (progress < 1f) {
        // Phase 2: orb in flight with a short wispy tail
        val flyProgress = (progress - ORB_CHARGE_PHASE_END) / (1f - ORB_CHARGE_PHASE_END)

        val dx = targetCenter.x - sourceCenter.x
        val dy = targetCenter.y - sourceCenter.y
        val dist = sqrt(dx * dx + dy * dy)
        if (dist < 0.01f) return

        val nx = dx / dist
        val ny = dy / dist

        val ballX = sourceCenter.x + dx * flyProgress
        val ballY = sourceCenter.y + dy * flyProgress
        val ballCenter = Offset(ballX, ballY)

        // Tail: a series of fading circles trailing behind the orb
        val tailSteps = 5
        for (i in tailSteps downTo 1) {
            val t = flyProgress - i * 0.04f
            if (t < 0f) continue
            val tx = sourceCenter.x + dx * t
            val ty = sourceCenter.y + dy * t
            val alpha = (tailSteps - i + 1f) / (tailSteps + 1f) * 0.35f
            drawCircle(
                color = Color(0xFF5500AA).copy(alpha = alpha),
                radius = orbRadius * (0.5f + (tailSteps - i) * 0.08f),
                center = Offset(tx, ty),
            )
        }

        // Outer glow
        drawCircle(
            color = Color(0xFF200040).copy(alpha = 0.55f),
            radius = orbRadius * 1.50f,
            center = ballCenter,
        )
        // Orb body
        drawCircle(
            color = Color(0xFF3D0070),
            radius = orbRadius * 1.15f,
            center = ballCenter,
        )
        // Mid violet layer
        drawCircle(
            color = Color(0xFF6600BB),
            radius = orbRadius * 0.80f,
            center = ballCenter,
        )
        // Bright core
        drawCircle(
            color = Color(0xFFAA44FF),
            radius = orbRadius * 0.42f,
            center = Offset(ballCenter.x - nx * orbRadius * 0.08f, ballCenter.y - ny * orbRadius * 0.08f),
        )
    }
}
