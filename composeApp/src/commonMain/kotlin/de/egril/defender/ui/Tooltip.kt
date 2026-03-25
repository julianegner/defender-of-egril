package de.egril.defender.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

/**
 * Wraps content with hover-based tooltip support.
 * The tooltip is shown when the user hovers over the content (desktop/web platforms).
 *
 * @param text Tooltip text to display, or null/empty to disable the tooltip
 * @param modifier Modifier for the wrapper Box
 * @param offset Position offset for the tooltip relative to the top-left of the wrapped content
 * @param content The composable content to wrap
 */
@Composable
fun TooltipWrapper(
    text: String?,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, (-40).dp),
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered = remember { mutableStateOf(false) }

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is HoverInteraction.Enter -> isHovered.value = true
                is HoverInteraction.Exit -> isHovered.value = false
            }
        }
    }

    Box(
        modifier = modifier.hoverable(interactionSource = interactionSource)
    ) {
        if (isHovered.value && !text.isNullOrEmpty()) {
            TooltipBox(text, offset)
        }
        content()
    }
}

@Composable
private fun TooltipBox(
    text: String,
    offset: DpOffset = DpOffset(0.dp, (-40).dp),
) {
    Box(
        modifier = Modifier
            .zIndex(100f)
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                // Report zero size so the tooltip does not affect the layout of surrounding elements
                layout(0, 0) {
                    placeable.place(0, 0)
                }
            }
            .offset(x = offset.x, y = offset.y)
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
