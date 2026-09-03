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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import de.egril.defender.utils.isPlatformMobile

/**
 * Wraps content with hover-based tooltip support.
 * The tooltip is shown when the user hovers over the content (desktop/web platforms).
 *
 * The tooltip is rendered inside a [Popup], so it is drawn in a separate overlay layer and is
 * never clipped by any parent container that clips its content (e.g. the bottom row of a
 * [androidx.compose.foundation.lazy.grid.LazyVerticalGrid]).
 *
 * The tooltip appears **below** the element by default.
 * When there is not enough space below (i.e. the element is near the bottom of the window),
 * the tooltip is shown **above** instead.
 * The tooltip is also clamped horizontally so it is never clipped by the left or right window edge,
 * and always keeps a small margin from both edges so it is never flush against the screen border.
 *
 * @param text Tooltip text to display, or null/empty to disable the tooltip
 * @param modifier Modifier for the wrapper Box
 * @param preferAbove When true, always show the tooltip above the element. Useful when the element
 *   is inside a container that clips content below it (e.g. the bottom row of a LazyVerticalGrid).
 * @param content The composable content to wrap
 */
@Composable
fun TooltipWrapper(
    text: String?,
    modifier: Modifier = Modifier,
    preferAbove: Boolean = false,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    var isHovered by remember { mutableStateOf(false) }
    val isHoverTooltipEnabled = !isPlatformMobile && !isMobileWebBrowser()
    val density = LocalDensity.current

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is HoverInteraction.Enter -> isHovered = true
                is HoverInteraction.Exit -> isHovered = false
            }
        }
    }

    Box(
        modifier =
            modifier
                .then(
                    if (isHoverTooltipEnabled) {
                        Modifier.hoverable(interactionSource = interactionSource)
                    } else {
                        Modifier
                    },
                ),
    ) {
        content()
        if (isHoverTooltipEnabled && isHovered && !text.isNullOrEmpty()) {
            val gapPx = with(density) { 4.dp.roundToPx() }
            val edgeMarginPx = with(density) { 8.dp.roundToPx() }
            Popup(
                popupPositionProvider =
                    TooltipPositionProvider(
                        preferAbove = preferAbove,
                        gapPx = gapPx,
                        edgeMarginPx = edgeMarginPx,
                    ),
            ) {
                TooltipBox(text = text)
            }
        }
    }
}

/**
 * Positions the tooltip relative to its anchor element. The tooltip is placed below the anchor by
 * default, or above it when [preferAbove] is set or when there is not enough space below.
 * It is clamped horizontally so it always keeps [edgeMarginPx] from both window edges.
 */
private class TooltipPositionProvider(
    private val preferAbove: Boolean,
    private val gapPx: Int,
    private val edgeMarginPx: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val spaceBelow = windowSize.height - anchorBounds.bottom
        val spaceAbove = anchorBounds.top
        val needed = popupContentSize.height + gapPx
        // Prefer the requested side, but fall back to whichever side actually has room.
        val showAbove =
            when {
                preferAbove && spaceAbove >= needed -> true
                preferAbove -> spaceBelow < needed
                spaceBelow >= needed -> false
                else -> spaceAbove > spaceBelow
            }

        var y =
            if (showAbove) {
                anchorBounds.top - popupContentSize.height - gapPx
            } else {
                anchorBounds.bottom + gapPx
            }
        // Clamp vertically so the tooltip is never clipped by the top or bottom window edge.
        val maxY = windowSize.height - popupContentSize.height
        if (y > maxY) {
            y = maxY
        }
        if (y < 0) {
            y = 0
        }

        var x = anchorBounds.left
        val maxX = windowSize.width - edgeMarginPx - popupContentSize.width
        if (x > maxX) {
            x = maxX
        }
        if (x < edgeMarginPx) {
            x = edgeMarginPx
        }

        return IntOffset(x, y)
    }
}

@Composable
private fun TooltipBox(text: String) {
    Box(
        modifier =
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.inverseSurface)
                .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.inverseOnSurface,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
