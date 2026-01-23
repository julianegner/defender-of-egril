package de.egril.defender.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable

/**
 * Platform-specific scrollbar for Level Dependencies view.
 * On desktop: Shows interactive scrollbars with custom styling.
 * On other platforms: No-op (native scrolling is used).
 */
@Composable
expect fun BoxScope.LevelSequenceScrollbar(
    horizontalScrollState: ScrollState,
    verticalScrollState: ScrollState
)
