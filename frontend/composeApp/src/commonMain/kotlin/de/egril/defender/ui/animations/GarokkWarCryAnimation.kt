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
import de.egril.defender.model.GarokkWarCryEffect
import de.egril.defender.model.Position
import de.egril.defender.ui.hexagon.HexagonalGridConstants
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val GAROKK_WAR_CRY_DURATION_MS = 700

@Composable
fun GarokkWarCryOverlay(
    effects: List<GarokkWarCryEffect>,
    hexSizeDp: Float,
    contentSize: IntSize,
    animate: Boolean,
) {
    val density = LocalDensity.current.density
    val hexSizePx = hexSizeDp * density
    val colSpacingPx = HexagonalGridConstants.HORIZONTAL_SPACING * density
    val rowVerticalAdjPx = HexagonalGridConstants.VERTICAL_SPACING_ADJUSTMENT * density
    val contentWidth: Dp = (contentSize.width / density).dp
    val contentHeight: Dp = (contentSize.height / density).dp

    effects.forEach { effect ->
        key(effect.turnNumber, effect.position.x, effect.position.y) {
            SingleGarokkWarCryOverlay(
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
private fun SingleGarokkWarCryOverlay(
    effect: GarokkWarCryEffect,
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

    val center = tileCenterPx(effect.position)
    val progress = remember { Animatable(0f) }

    LaunchedEffect(effect.turnNumber, effect.position) {
        if (animate) {
            progress.animateTo(
                targetValue = 1f,
                animationSpec =
                    tween(
                        durationMillis = GAROKK_WAR_CRY_DURATION_MS,
                        easing = LinearEasing,
                    ),
            )
        } else {
            progress.snapTo(1f)
        }
    }

    Canvas(modifier = Modifier.requiredSize(contentWidth, contentHeight)) {
        val t = progress.value
        val radius = hexSizePx * (0.35f + t * 1.1f)
        val alpha = 1f - t

        drawCircle(
            color = Color(0xFFBF3B2D).copy(alpha = alpha * 0.35f),
            radius = radius,
            center = center,
        )
        drawCircle(
            color = Color(0xFFFFA726).copy(alpha = alpha * 0.50f),
            radius = radius * 0.74f,
            center = center,
        )
        drawCircle(
            color = Color(0xFFFFD54F).copy(alpha = alpha * 0.75f),
            radius = radius * 0.42f,
            center = center,
        )

        val flareLength = hexSizePx * 0.9f
        val glowAlpha = alpha * 0.8f
        for (dir in 0..5) {
            val angle = dir * (PI / 3.0) + (t * 2.0 * PI / 5.0)
            val startX = center.x + cos(angle).toFloat() * radius * 0.55f
            val startY = center.y + sin(angle).toFloat() * radius * 0.55f
            val endX = center.x + cos(angle).toFloat() * (radius + flareLength)
            val endY = center.y + sin(angle).toFloat() * (radius + flareLength)
            drawLine(
                color = Color(0xFFFFF3B0).copy(alpha = glowAlpha),
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = hexSizePx * 0.09f,
            )
        }
    }
}
