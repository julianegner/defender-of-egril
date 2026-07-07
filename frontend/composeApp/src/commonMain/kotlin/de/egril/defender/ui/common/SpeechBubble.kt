package de.egril.defender.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The edge of a [SpeechBubble] the pointer (tail) protrudes from.
 * The pointer always points *away* from the bubble body, towards the element the
 * bubble refers to (e.g. [UP] points upwards, so the bubble sits below its anchor).
 */
enum class SpeechBubblePointer {
    UP,
    DOWN,
    LEFT,
    RIGHT,
}

/**
 * A reusable speech-bubble container: a rounded rectangle with a small triangular
 * pointer on one edge, meant to visually "point" at a nearby element.
 *
 * Colours default to theme-aware values (secondary container), so the bubble
 * automatically fits both light and dark mode. Provide [backgroundColor]/[contentColor]
 * to override.
 *
 * The composable reserves space for the pointer on the relevant edge, so the pointer
 * never overlaps the [content]. Wrap any content (usually a Text).
 *
 * @param pointer Which edge the pointer protrudes from (and therefore the direction it points).
 * @param pointerBias Position of the pointer along its edge, 0f (start/top) .. 1f (end/bottom).
 * @param pointerOffset Absolute distance from the start of the pointer's edge (left for UP/DOWN,
 *   top for LEFT/RIGHT) to the pointer tip. When non-null it takes precedence over [pointerBias];
 *   useful to aim the tip at a specific anchor regardless of the bubble's own width/height.
 * @param backgroundColor Fill colour of the bubble.
 * @param contentColor Colour provided to the content via [LocalContentColor].
 * @param borderColor Optional outline colour drawn around the bubble and pointer; null to disable.
 * @param cornerRadius Corner radius of the bubble body.
 * @param pointerWidth Base width of the triangular pointer.
 * @param pointerLength How far the pointer protrudes from the bubble body.
 * @param borderWidth Stroke width used when [borderColor] is set.
 * @param contentPadding Inner padding around [content].
 */
@Composable
fun SpeechBubble(
    modifier: Modifier = Modifier,
    pointer: SpeechBubblePointer = SpeechBubblePointer.UP,
    pointerBias: Float = 0.5f,
    pointerOffset: Dp? = null,
    backgroundColor: Color = SpeechBubbleDefaults.backgroundColor,
    contentColor: Color = SpeechBubbleDefaults.contentColor,
    borderColor: Color? = SpeechBubbleDefaults.borderColor,
    cornerRadius: Dp = 12.dp,
    pointerWidth: Dp = 16.dp,
    pointerLength: Dp = 8.dp,
    borderWidth: Dp = 1.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    content: @Composable () -> Unit,
) {
    // Reserve space for the pointer on the edge it protrudes from so it does not overlap content.
    val pointerPadding =
        when (pointer) {
            SpeechBubblePointer.UP -> PaddingValues(top = pointerLength)
            SpeechBubblePointer.DOWN -> PaddingValues(bottom = pointerLength)
            SpeechBubblePointer.LEFT -> PaddingValues(start = pointerLength)
            SpeechBubblePointer.RIGHT -> PaddingValues(end = pointerLength)
        }

    Box(
        modifier =
            modifier
                .drawBehind {
                    val path =
                        buildSpeechBubblePath(
                            pointer = pointer,
                            pointerBias = pointerBias.coerceIn(0f, 1f),
                            pointerOffsetPx = pointerOffset?.toPx(),
                            cornerRadiusPx = cornerRadius.toPx(),
                            pointerWidthPx = pointerWidth.toPx(),
                            pointerLengthPx = pointerLength.toPx(),
                        )
                    drawPath(path, color = backgroundColor)
                    if (borderColor != null) {
                        drawPath(path, color = borderColor, style = Stroke(width = borderWidth.toPx()))
                    }
                }.padding(pointerPadding)
                .padding(contentPadding),
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}

/** Theme-aware default colours for [SpeechBubble]. */
object SpeechBubbleDefaults {
    val backgroundColor: Color
        @Composable get() = MaterialTheme.colorScheme.secondaryContainer
    val contentColor: Color
        @Composable get() = MaterialTheme.colorScheme.onSecondaryContainer
    val borderColor: Color
        @Composable get() = MaterialTheme.colorScheme.outline
}

/**
 * Builds the outline [Path] of a rounded rectangle with a triangular pointer on one edge.
 * The bubble body is inset from the drawing area by [pointerLengthPx] on the pointer's edge,
 * leaving room for the protruding pointer.
 */
private fun DrawScope.buildSpeechBubblePath(
    pointer: SpeechBubblePointer,
    pointerBias: Float,
    pointerOffsetPx: Float?,
    cornerRadiusPx: Float,
    pointerWidthPx: Float,
    pointerLengthPx: Float,
): Path {
    val width = size.width
    val height = size.height

    // The rounded-rectangle body, inset on the side the pointer protrudes from.
    val left = if (pointer == SpeechBubblePointer.LEFT) pointerLengthPx else 0f
    val top = if (pointer == SpeechBubblePointer.UP) pointerLengthPx else 0f
    val right = if (pointer == SpeechBubblePointer.RIGHT) width - pointerLengthPx else width
    val bottom = if (pointer == SpeechBubblePointer.DOWN) height - pointerLengthPx else height

    // Clamp the corner radius so it never exceeds half of the shorter body side.
    val radius = cornerRadiusPx.coerceAtMost(minOf(right - left, bottom - top) / 2f).coerceAtLeast(0f)
    val half = pointerWidthPx / 2f

    val body =
        Path().apply {
            addRoundRect(
                RoundRect(
                    left = left,
                    top = top,
                    right = right,
                    bottom = bottom,
                    cornerRadius = CornerRadius(radius, radius),
                ),
            )
        }

    val pointerPath =
        Path().apply {
            when (pointer) {
                SpeechBubblePointer.UP -> {
                    val centerX = resolveCenter(pointerOffsetPx, pointerBias, left + radius + half, right - radius - half)
                    moveTo(centerX - half, top)
                    lineTo(centerX, 0f)
                    lineTo(centerX + half, top)
                }
                SpeechBubblePointer.DOWN -> {
                    val centerX = resolveCenter(pointerOffsetPx, pointerBias, left + radius + half, right - radius - half)
                    moveTo(centerX - half, bottom)
                    lineTo(centerX, height)
                    lineTo(centerX + half, bottom)
                }
                SpeechBubblePointer.LEFT -> {
                    val centerY = resolveCenter(pointerOffsetPx, pointerBias, top + radius + half, bottom - radius - half)
                    moveTo(left, centerY - half)
                    lineTo(0f, centerY)
                    lineTo(left, centerY + half)
                }
                SpeechBubblePointer.RIGHT -> {
                    val centerY = resolveCenter(pointerOffsetPx, pointerBias, top + radius + half, bottom - radius - half)
                    moveTo(right, centerY - half)
                    lineTo(width, centerY)
                    lineTo(right, centerY + half)
                }
            }
            close()
        }

    return Path().apply {
        op(body, pointerPath, PathOperation.Union)
    }
}

/**
 * Resolves the pointer tip centre coordinate along its edge, clamped between [min] and [max].
 * Uses the explicit [offsetPx] when provided, otherwise interpolates via [bias].
 */
private fun resolveCenter(
    offsetPx: Float?,
    bias: Float,
    min: Float,
    max: Float,
): Float {
    val safeMax = max.coerceAtLeast(min)
    val raw = offsetPx ?: lerp(min, safeMax, bias)
    return raw.coerceIn(min, safeMax)
}

private fun lerp(
    start: Float,
    stop: Float,
    fraction: Float,
): Float = start + (stop - start) * fraction
