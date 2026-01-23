package de.egril.defender.ui

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.rememberScrollbarAdapter

/**
 * Padding for scrollbar spacing to prevent overlap
 */
private const val SCROLLBAR_PADDING = 12

@Composable
actual fun BoxScope.LevelSequenceScrollbar(
    horizontalScrollState: ScrollState,
    verticalScrollState: ScrollState
) {
    // Interactive scrollbars with custom styling for better visibility in both light and dark modes
    val scrollbarStyle = ScrollbarStyle(
        minimalHeight = 16.dp,
        thickness = 8.dp,
        shape = MaterialTheme.shapes.small,
        hoverDurationMillis = 300,
        unhoverColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
        hoverColor = MaterialTheme.colorScheme.primary
    )

    HorizontalScrollbar(
        adapter = rememberScrollbarAdapter(horizontalScrollState),
        style = scrollbarStyle,
        modifier = Modifier
            .align(Alignment.BottomStart)
            .fillMaxWidth()
            .padding(end = SCROLLBAR_PADDING.dp) // Space for vertical scrollbar
    )

    VerticalScrollbar(
        adapter = rememberScrollbarAdapter(verticalScrollState),
        style = scrollbarStyle,
        modifier = Modifier
            .align(Alignment.CenterEnd)
            .fillMaxHeight()
            .padding(bottom = SCROLLBAR_PADDING.dp) // Space for horizontal scrollbar
    )
}
