package de.egril.defender.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

/**
 * Wraps content with hover-based tooltip support.
 * The tooltip is shown when the user hovers over the content (desktop/web platforms).
 *
 * The tooltip appears **below** the element by default.
 * When there is not enough space below (i.e. the element is near the bottom of the window),
 * the tooltip is shown **above** instead.
 * The tooltip is also clamped horizontally so it is never clipped by the left or right window edge,
 * and always keeps a small margin from both edges so it is never flush against the screen border.
 *
 * @param text Tooltip text to display, or null/empty to disable the tooltip
 * @param modifier Modifier for the wrapper Box
 * @param content The composable content to wrap
 */
@Composable
fun TooltipWrapper(
    text: String?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    var isHovered by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current

    // Track element position and size in the window, updated after each layout pass
    var elementHeightPx by remember { mutableIntStateOf(0) }
    var elementLeftPx by remember { mutableFloatStateOf(0f) }
    var elementBottomPx by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is HoverInteraction.Enter -> isHovered = true
                is HoverInteraction.Exit -> isHovered = false
            }
        }
    }

    Box(
        modifier = modifier
            .hoverable(interactionSource = interactionSource)
            .onGloballyPositioned { coordinates ->
                elementHeightPx = coordinates.size.height
                val posInWindow = coordinates.positionInWindow()
                elementLeftPx = posInWindow.x
                elementBottomPx = posInWindow.y + coordinates.size.height
            }
    ) {
        if (isHovered && !text.isNullOrEmpty()) {
            val windowHeightPx = windowInfo.containerSize.height.toFloat()
            val windowWidthPx = windowInfo.containerSize.width.toFloat()
            val spaceBelowPx = windowHeightPx - elementBottomPx

            // Show above when less than 60dp of space remains below the element
            val minSpaceBelowPx = with(density) { 60.dp.toPx() }
            val showAbove = windowHeightPx > 0 && spaceBelowPx < minSpaceBelowPx

            val offsetY: Dp = if (showAbove) {
                (-40).dp  // above the element
            } else {
                with(density) { elementHeightPx.toDp() } + 4.dp  // below with a small gap
            }

            TooltipBox(
                text = text,
                offsetY = offsetY,
                elementLeftPx = elementLeftPx,
                windowWidthPx = windowWidthPx,
            )
        }
        content()
    }
}

@Composable
private fun TooltipBox(
    text: String,
    offsetY: Dp = 0.dp,
    elementLeftPx: Float = 0f,
    windowWidthPx: Float = Float.MAX_VALUE,
) {
    Box(
        modifier = Modifier
            .zIndex(100f)
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val yOffsetPx = offsetY.roundToPx()

                // Compute a horizontal shift so the tooltip stays within the window bounds,
                // keeping a small margin from both screen edges so the tooltip never touches the border.
                // By default the tooltip's left edge aligns with the element's left edge (xOffset = 0).
                var xOffsetPx = 0
                if (windowWidthPx > 0 && windowWidthPx < Float.MAX_VALUE) {
                    val edgeMarginPx = 8.dp.roundToPx()
                    val rightEdge = elementLeftPx + placeable.width
                    if (rightEdge > windowWidthPx - edgeMarginPx) {
                        // Tooltip would overflow to the right — shift it left, keeping margin from right edge
                        xOffsetPx = -(rightEdge - windowWidthPx + edgeMarginPx).toInt()
                    }
                    // Ensure the left edge always respects the margin from the left edge
                    if (elementLeftPx + xOffsetPx < edgeMarginPx) {
                        xOffsetPx = (edgeMarginPx - elementLeftPx).toInt()
                    }
                }

                // Report zero size so the tooltip does not affect the layout of surrounding elements
                layout(0, 0) {
                    placeable.place(xOffsetPx, yOffsetPx)
                }
            }
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.inverseSurface)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.inverseOnSurface,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
