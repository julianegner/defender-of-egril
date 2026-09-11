package de.egril.defender.ui

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
actual fun BoxScope.EditorHorizontalScrollbar(scrollState: ScrollState) {
    HorizontalScrollbar(
        adapter = rememberScrollbarAdapter(scrollState),
        style =
            ScrollbarStyle(
                minimalHeight = 16.dp,
                thickness = 8.dp,
                shape = MaterialTheme.shapes.small,
                hoverDurationMillis = 300,
                unhoverColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                hoverColor = MaterialTheme.colorScheme.primary,
            ),
        modifier =
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(),
    )
}
