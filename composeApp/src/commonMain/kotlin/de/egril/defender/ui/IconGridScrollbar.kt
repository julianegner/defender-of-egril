package de.egril.defender.ui

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable

/**
 * Platform-specific vertical scrollbar for icon grid.
 * On desktop: Shows an interactive vertical scrollbar.
 * On other platforms: No-op (native scrolling is used).
 */
@Composable
expect fun BoxScope.IconGridScrollbar(gridState: LazyGridState)
